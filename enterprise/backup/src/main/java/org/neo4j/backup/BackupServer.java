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
package org.neo4j.backup;

import org.jboss.netty.channel.Channel;
import org.neo4j.backup.BackupClient.BackupRequestType;
import org.neo4j.com.Client;
import org.neo4j.com.Protocol;
import org.neo4j.com.RequestType;
import org.neo4j.com.Server;
import org.neo4j.com.SlaveContext;
import org.neo4j.com.TxChecksumVerifier;

class BackupServer extends Server<TheBackupInterface, Object>
{
    static final byte PROTOCOL_VERSION = 1;
    private final BackupRequestType[] contexts = BackupRequestType.values();
    static int DEFAULT_PORT = DEFAULT_BACKUP_PORT;
    static final int FRAME_LENGTH = Protocol.MEGA*4;
    
    public BackupServer( TheBackupInterface realMaster, int port, String storeDir )
    {
        super( realMaster, port, storeDir, FRAME_LENGTH, PROTOCOL_VERSION,
                DEFAULT_MAX_NUMBER_OF_CONCURRENT_TRANSACTIONS, Client.DEFAULT_READ_RESPONSE_TIMEOUT_SECONDS,
                TxChecksumVerifier.ALWAYS_MATCH );
    }

    @Override
    protected void responseWritten( RequestType<TheBackupInterface> type, Channel channel,
            SlaveContext context )
    {
    }

    @Override
    protected RequestType<TheBackupInterface> getRequestContext( byte id )
    {
        return contexts[id];
    }

    @Override
    protected void finishOffChannel( Channel channel, SlaveContext context )
    {
    }
}
