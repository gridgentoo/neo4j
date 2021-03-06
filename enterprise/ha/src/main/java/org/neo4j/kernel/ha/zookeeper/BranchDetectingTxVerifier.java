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
package org.neo4j.kernel.ha.zookeeper;

import java.io.IOException;

import org.neo4j.com.ComException;
import org.neo4j.com.TxChecksumVerifier;
import org.neo4j.helpers.Pair;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.Config;
import org.neo4j.kernel.ha.BranchedDataException;
import org.neo4j.kernel.impl.transaction.xaframework.LogEntry;
import org.neo4j.kernel.impl.transaction.xaframework.XaDataSource;
import org.neo4j.kernel.impl.util.StringLogger;

class BranchDetectingTxVerifier implements TxChecksumVerifier
{
    private final AbstractGraphDatabase db;
    private XaDataSource dataSource;
    private final StringLogger msgLog;

    BranchDetectingTxVerifier( AbstractGraphDatabase db /* I'd like to get in StringLogger, XaDataSource instead */ )
    {
        /* We cannot pass in XaResourceManager because it this time we don't have a
         * proper db, merely the HA graph db which is a layer around a not-yet-started db
         * Rickards restructuring will of course fix this */
        this.db = db;
        msgLog = StringLogger.getLogger( db.getStoreDir() );
    }

    @Override
    public void assertMatch( long txId, int masterId, long checksum )
    {
        try
        {
            Pair<Integer, Long> readChecksum = dataSource().getMasterForCommittedTx( txId );
            /*
             * For masters with the "high-bits-set" bug, the order in checksumMatch matters.
             * The first one should be the one from a slave which is generated correctly. The
             * second should be the one read, which is generated wrong.
             */
            boolean match = masterId == readChecksum.first()
                            && LogEntry.Start.checksumMatch( checksum, readChecksum.other() );

            /* MP: This "packing" of a BranchedDataException inside a ComException is just to
             * let it be able to be picked up by current catch clauses and code paths. The only
             * special hacky thing needed in HAGraphDb would be to look for the cause in the exception
             * fed to HAGraphDb#newMaster instead of the actual exception. */
            if ( !match ) throw new ComException( new BranchedDataException( stringify( txId, masterId, checksum ) +
                    " doesn't match " + readChecksum ) );
        }
        catch ( IOException e )
        {
            msgLog.logMessage( "Couldn't verify checksum for " + stringify( txId, masterId, checksum ), e );
            throw new BranchedDataException( e );
        }
    }

    private XaDataSource dataSource()
    {
        if ( dataSource == null ) dataSource = db.getConfig().getTxModule().getXaDataSourceManager()
                .getXaDataSource( Config.DEFAULT_DATA_SOURCE_NAME );
        return dataSource;
    }

    private String stringify( long txId, int masterId, long checksum )
    {
        return "txId:" + txId + ", masterId:" + masterId + ", checksum:" + checksum;
    }
}
