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
package org.neo4j.kernel.impl.transaction.xaframework;

import java.io.IOException;
import java.nio.channels.FileChannel;

public interface LogBuffer
{
    public LogBuffer put( byte b ) throws IOException;

    public LogBuffer putShort( short b ) throws IOException;
    
    public LogBuffer putInt( int i ) throws IOException;

    public LogBuffer putLong( long l ) throws IOException;

    public LogBuffer putFloat( float f ) throws IOException;
    
    public LogBuffer putDouble( double d ) throws IOException;
    
    public LogBuffer put( byte[] bytes ) throws IOException;

    public LogBuffer put( char[] chars ) throws IOException;
    
    /**
     * Makes sure the data added to this buffer is written out to the underlying file.
     * @throws IOException if the data couldn't be written.
     */
    public void writeOut() throws IOException;

    /**
     * Makes sure the data added to this buffer is written out to the underlying file
     * and forced.
     * @throws IOException if the data couldn't be written.
     */
    public void force() throws IOException;

    public long getFileChannelPosition() throws IOException;

    public FileChannel getFileChannel();
}
