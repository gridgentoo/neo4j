/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.backup.check;

import java.util.Collection;

import org.neo4j.kernel.impl.nioneo.store.AbstractBaseRecord;
import org.neo4j.kernel.impl.nioneo.store.DynamicRecord;
import org.neo4j.kernel.impl.nioneo.store.NeoStore;
import org.neo4j.kernel.impl.nioneo.store.NodeRecord;
import org.neo4j.kernel.impl.nioneo.store.NodeStore;
import org.neo4j.kernel.impl.nioneo.store.PropertyBlock;
import org.neo4j.kernel.impl.nioneo.store.PropertyIndexRecord;
import org.neo4j.kernel.impl.nioneo.store.PropertyRecord;
import org.neo4j.kernel.impl.nioneo.store.PropertyStore;
import org.neo4j.kernel.impl.nioneo.store.PropertyType;
import org.neo4j.kernel.impl.nioneo.store.Record;
import org.neo4j.kernel.impl.nioneo.store.RecordStore;
import org.neo4j.kernel.impl.nioneo.store.RelationshipRecord;
import org.neo4j.kernel.impl.nioneo.store.RelationshipStore;
import org.neo4j.kernel.impl.nioneo.store.RelationshipTypeRecord;
import org.neo4j.kernel.impl.nioneo.store.RelationshipTypeStore;
import org.neo4j.kernel.impl.nioneo.store.StoreAccess;
import org.neo4j.kernel.impl.nioneo.xa.CommandRecordVisitor;

/**
 * Not thread safe (since DiffRecordStore is not thread safe), intended for
 * single threaded use.
 */
public class DiffStore extends StoreAccess implements CommandRecordVisitor
{
    public DiffStore( NeoStore store )
    {
        super( store );
    }

    public DiffStore( NodeStore nodeStore, RelationshipStore relStore, PropertyStore propStore,
            RelationshipTypeStore typeStore )
    {
        super( nodeStore, relStore, propStore, typeStore );
    }

    @Override
    protected <R extends AbstractBaseRecord> RecordStore<R> wrapStore( RecordStore<R> store )
    {
        return new DiffRecordStore<R>( store );
    }

    /**
     * Overridden to increase visibility to public, it's used from
     * {@link org.neo4j.backup.log.InconsistencyLoggingTransactionInterceptorProvider}.
     */
    @Override
    public RecordStore<?>[] allStores()
    {
        return super.allStores();
    }

    @Override
    protected void apply( RecordStore.Processor processor, RecordStore<?> store )
    {
        processor.applyById( store, (DiffRecordStore<?>) store );
    }

    @Override
    public void visitNode( NodeRecord record )
    {
        getNodeStore().forceUpdateRecord( record );
        record = getNodeStore().forceGetRaw( record.getId() );
        if ( record.inUse() )
        {
            markProperty( record.getNextProp() );
            markRelationship( record.getNextRel() );
        }
    }

    @Override
    public void visitRelationship( RelationshipRecord record )
    {
        getRelationshipStore().forceUpdateRecord( record );
        record = getRelationshipStore().forceGetRaw( record.getId() );
        if ( record.inUse() )
        {
            getNodeStore().markDirty( record.getFirstNode() );
            getNodeStore().markDirty( record.getSecondNode() );
            markProperty( record.getNextProp() );
            markRelationship( record.getFirstNextRel() );
            markRelationship( record.getFirstPrevRel() );
            markRelationship( record.getSecondNextRel() );
            markRelationship( record.getSecondPrevRel() );
        }
    }

    private void markRelationship( long rel )
    {
        if ( !Record.NO_NEXT_RELATIONSHIP.value( rel ) ) getRelationshipStore().markDirty( rel );
    }

    private void markProperty( long prop )
    {
        if ( !Record.NO_NEXT_PROPERTY.value( prop ) ) getPropertyStore().markDirty( prop );
    }

    @Override
    public void visitProperty( PropertyRecord record )
    {
        getPropertyStore().forceUpdateRecord( record );
        updateDynamic( record );
        record = getPropertyStore().forceGetRaw( record.getId() );
        updateDynamic( record );
        if ( record.inUse() )
        {
            markProperty( record.getNextProp() );
            markProperty( record.getPrevProp() );
        }
    }

    private void updateDynamic( PropertyRecord record )
    {
        for ( PropertyBlock block : record.getPropertyBlocks() )
            updateDynamic( block.getValueRecords() );
        updateDynamic( record.getDeletedRecords() );
    }

    private void updateDynamic( Collection<DynamicRecord> records )
    {
        for ( DynamicRecord record : records )
        {
            DiffRecordStore<DynamicRecord> store = ( record.getType() == PropertyType.STRING.intValue() )
                    ? getStringStore() : getArrayStore();
            store.forceUpdateRecord( record );
            if ( !Record.NO_NEXT_BLOCK.value( record.getNextBlock() ) )
                getPropertyStore().markDirty( record.getNextBlock() );
        }
    }

    @Override
    public void visitPropertyIndex( PropertyIndexRecord record )
    {
        getPropertyIndexStore().forceUpdateRecord( record );
        for ( DynamicRecord key : record.getKeyRecords() )
            getPropertyKeyStore().forceUpdateRecord( key );
    }

    @Override
    public void visitRelationshipType( RelationshipTypeRecord record )
    {
        getRelationshipTypeStore().forceUpdateRecord( record );
        for ( DynamicRecord key : record.getTypeRecords() )
            getTypeNameStore().forceUpdateRecord( key );
    }

    @Override
    public DiffRecordStore<NodeRecord> getNodeStore()
    {
        return (DiffRecordStore<NodeRecord>) super.getNodeStore();
    }

    @Override
    public DiffRecordStore<RelationshipRecord> getRelationshipStore()
    {
        return (DiffRecordStore<RelationshipRecord>) super.getRelationshipStore();
    }

    @Override
    public DiffRecordStore<PropertyRecord> getPropertyStore()
    {
        return (DiffRecordStore<PropertyRecord>) super.getPropertyStore();
    }

    @Override
    public DiffRecordStore<DynamicRecord> getStringStore()
    {
        return (DiffRecordStore<DynamicRecord>) super.getStringStore();
    }

    @Override
    public DiffRecordStore<DynamicRecord> getArrayStore()
    {
        return (DiffRecordStore<DynamicRecord>) super.getArrayStore();
    }

    @Override
    public DiffRecordStore<RelationshipTypeRecord> getRelationshipTypeStore()
    {
        return (DiffRecordStore<RelationshipTypeRecord>) super.getRelationshipTypeStore();
    }

    @Override
    public DiffRecordStore<DynamicRecord> getTypeNameStore()
    {
        return (DiffRecordStore<DynamicRecord>) super.getTypeNameStore();
    }

    @Override
    public DiffRecordStore<PropertyIndexRecord> getPropertyIndexStore()
    {
        return (DiffRecordStore<PropertyIndexRecord>) super.getPropertyIndexStore();
    }

    @Override
    public DiffRecordStore<DynamicRecord> getPropertyKeyStore()
    {
        return (DiffRecordStore<DynamicRecord>) super.getPropertyKeyStore();
    }
}
