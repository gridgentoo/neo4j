/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.index.impl.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.Neo4jTestCase;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.impl.index.IndexStore;

public class TestMigration
{
    @Test
    public void canReadAndUpgradeOldIndexStoreFormat() throws Exception
    {
        String path = "target/var/old-index-store";
        Neo4jTestCase.deleteFileOrDirectory( new File( path ) );
        GraphDatabaseService db = new EmbeddedGraphDatabase( path );
        db.shutdown();
        InputStream stream = getClass().getClassLoader().getResourceAsStream( "old-index.db" );
        writeFile( stream, new File( path, "index.db" ) );
        db = new EmbeddedGraphDatabase( path );
        assertTrue( db.index().existsForNodes( "indexOne" ) );
        Index<Node> indexOne = db.index().forNodes( "indexOne" );
        verifyConfiguration( db, indexOne, LuceneIndexImplementation.EXACT_CONFIG );
        assertTrue( db.index().existsForNodes( "indexTwo" ) );
        Index<Node> indexTwo = db.index().forNodes( "indexTwo" );
        verifyConfiguration( db, indexTwo, LuceneIndexImplementation.FULLTEXT_CONFIG );
        assertTrue( db.index().existsForRelationships( "indexThree" ) );
        Index<Relationship> indexThree = db.index().forRelationships( "indexThree" );
        verifyConfiguration( db, indexThree, LuceneIndexImplementation.EXACT_CONFIG );
        db.shutdown();
    }

    @Test
    @Ignore
    public void canUpgradeFromPreviousVersion() throws Exception
    {
        GraphDatabaseService db = unpackDbFrom( "db-with-v3.0.1.zip" );
        Index<Node> index = db.index().forNodes( "v3.0.1" );
        Node node = index.get( "key", "value" ).getSingle();
        assertNotNull( node );
        db.shutdown();
    }

    private GraphDatabaseService unpackDbFrom( String file ) throws IOException
    {
        File path = new File( "target/var/zipup" );
        ZipInputStream zip = new ZipInputStream( getClass().getClassLoader().getResourceAsStream( file ) );
        ZipEntry entry = null;
        byte[] buffer = new byte[2048];
        while ( (entry = zip.getNextEntry()) != null )
        {
            if ( entry.isDirectory() )
            {
                new File( path, entry.getName() ).mkdirs();
                continue;
            }
            FileOutputStream fos = new FileOutputStream( new File( path, entry.getName() ) );
            BufferedOutputStream bos = new BufferedOutputStream( fos, buffer.length );

            int size;
            while ( (size = zip.read( buffer, 0, buffer.length )) != -1 )
            {
                bos.write( buffer, 0, size );
            }
            bos.flush();
            bos.close();
        }
        return new EmbeddedGraphDatabase( path.getAbsolutePath() );
    }

    private void verifyConfiguration( GraphDatabaseService db, Index<? extends PropertyContainer> index, Map<String, String> config )
    {
        assertEquals( config, db.index().getConfiguration( index ) );
    }

    private void writeFile( InputStream stream, File file ) throws Exception
    {
        file.delete();
        OutputStream out = new FileOutputStream( file );
        byte[] bytes = new byte[1024];
        int bytesRead = 0;
        while ( (bytesRead = stream.read( bytes )) >= 0 )
        {
            out.write( bytes, 0, bytesRead );
        }
        out.close();
    }

    @Test
    public void providerGetsFilledInAutomatically()
    {
        Map<String, String> correctConfig = MapUtil.stringMap( "type", "exact", IndexManager.PROVIDER, "lucene" );
        File storeDir = new File( "target/var/index" );
        Neo4jTestCase.deleteFileOrDirectory( storeDir );
        GraphDatabaseService graphDb = new EmbeddedGraphDatabase( storeDir.getPath() );
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forNodes( "default" ) ) );
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forNodes( "wo-provider", MapUtil.stringMap( "type", "exact" ) ) ) );
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forNodes( "w-provider", MapUtil.stringMap( "type", "exact", IndexManager.PROVIDER, "lucene" ) ) ) );
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forRelationships( "default" ) ) );
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forRelationships( "wo-provider", MapUtil.stringMap( "type", "exact" ) ) ) );
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forRelationships( "w-provider", MapUtil.stringMap( "type", "exact", IndexManager.PROVIDER, "lucene" ) ) ) );
        graphDb.shutdown();

        removeProvidersFromIndexDbFile( storeDir );
        graphDb = new EmbeddedGraphDatabase( storeDir.getPath() );
        // Getting the index w/o exception means that the provider has been reinstated
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forNodes( "default" ) ) );
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forNodes( "wo-provider", MapUtil.stringMap( "type", "exact" ) ) ) );
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forNodes( "w-provider", MapUtil.stringMap( "type", "exact", IndexManager.PROVIDER, "lucene" ) ) ) );
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forRelationships( "default" ) ) );
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forRelationships( "wo-provider", MapUtil.stringMap( "type", "exact" ) ) ) );
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forRelationships( "w-provider", MapUtil.stringMap( "type", "exact", IndexManager.PROVIDER, "lucene" ) ) ) );
        graphDb.shutdown();

        removeProvidersFromIndexDbFile( storeDir );
        graphDb = new EmbeddedGraphDatabase( storeDir.getPath() );
        // Getting the index w/o exception means that the provider has been reinstated
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forNodes( "default" ) ) );
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forNodes( "wo-provider" ) ) );
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forNodes( "w-provider" ) ) );
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forRelationships( "default" ) ) );
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forRelationships( "wo-provider" ) ) );
        assertEquals( correctConfig, graphDb.index().getConfiguration( graphDb.index().forRelationships( "w-provider" ) ) );
        graphDb.shutdown();
    }

    private void removeProvidersFromIndexDbFile( File storeDir )
    {
        IndexStore indexStore = new IndexStore( storeDir.getPath() );
        for ( Class<? extends PropertyContainer> cls : new Class[] {Node.class, Relationship.class} )
        {
            for ( String name : indexStore.getNames( cls ) )
            {
                Map<String, String> config = indexStore.get( cls, name );
                config = new HashMap<String, String>( config );
                config.remove( IndexManager.PROVIDER );
                indexStore.set( Node.class, name, config );
            }
        }
    }
}
