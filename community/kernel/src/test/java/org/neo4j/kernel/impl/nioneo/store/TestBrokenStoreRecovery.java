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
package org.neo4j.kernel.impl.nioneo.store;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import org.junit.Test;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.test.TargetDirectory;

public class TestBrokenStoreRecovery
{
    private void trimFileToSize( File theFile, int toSize )
            throws IOException
    {
        FileChannel theChannel = new RandomAccessFile( theFile, "rw" ).getChannel();
        theChannel.truncate( toSize );
        theChannel.force( false );
        theChannel.close();
    }

    /**
     * Creates a store with a truncated property store file that remains like
     * that during recovery by truncating the logical log as well. Id
     * regeneration should proceed without exceptions, even though the last
     * property record is incomplete.
     *
     * @throws Exception
     */
    @Test
    public void testTruncatedPropertyStore() throws Exception
    {
        File storeDir = TargetDirectory.forTest(
                TestBrokenStoreRecovery.class ).directory( "propertyStore",
                true );
        assertEquals(
                0,
                Runtime.getRuntime().exec(
                        new String[] { "java", "-cp",
                                System.getProperty( "java.class.path" ),
                                ProduceUncleanStore.class.getName(),
                                storeDir.getAbsolutePath() } ).waitFor() );
        trimFileToSize( new File( storeDir, "neostore.propertystore.db" ), 42 );
        File log = new File( storeDir, "nioneo_logical.log.1" );
        trimFileToSize( log, 78 );
        new EmbeddedGraphDatabase( storeDir.getAbsolutePath() ).shutdown();
    }
}
