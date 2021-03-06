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
package org.neo4j.kernel.ha;

import org.neo4j.com.Response;
import org.neo4j.com.SlaveContext;
import org.neo4j.com.StoreWriter;
import org.neo4j.com.TxExtractor;
import org.neo4j.helpers.Pair;
import org.neo4j.kernel.IdType;
import org.neo4j.kernel.impl.nioneo.store.StoreId;

/**
 * Represents the master-side of the HA communication between master and slave.
 * A master will receive calls to these methods from slaves when they do stuff.
 */
public interface Master
{
    Response<IdAllocation> allocateIds( IdType idType );

    Response<Integer> createRelationshipType( SlaveContext context, String name );

    /**
     * Called when the first write operation of lock is performed for a transaction.
     */
    Response<Void> initializeTx( SlaveContext context );

    Response<LockResult> acquireNodeWriteLock( SlaveContext context, long... nodes );

    Response<LockResult> acquireNodeReadLock( SlaveContext context, long... nodes );

    Response<LockResult> acquireRelationshipWriteLock( SlaveContext context, long... relationships );

    Response<LockResult> acquireRelationshipReadLock( SlaveContext context, long... relationships );

    Response<Long> commitSingleResourceTransaction( SlaveContext context,
            String resource, TxExtractor txGetter );

    Response<Void> finishTransaction( SlaveContext context, boolean success );

    Response<Void> pullUpdates( SlaveContext context );

    /**
     * Gets the master id for a given txId, also a checksum for that tx.
     * @param txId the transaction id to get the data for.
     * @param myStoreId clients store id.
     * @return the master id for a given txId, also a checksum for that tx.
     */
    Response<Pair<Integer,Long>> getMasterIdForCommittedTx( long txId, StoreId myStoreId );

    Response<Void> copyStore( SlaveContext context, StoreWriter writer );

    Response<Void> copyTransactions( SlaveContext context, String dsName,
            long startTxId, long endTxId );

    void shutdown();

    Response<LockResult> acquireIndexWriteLock( SlaveContext context, String index, String key );

    Response<LockResult> acquireIndexReadLock( SlaveContext context, String index, String key );
}
