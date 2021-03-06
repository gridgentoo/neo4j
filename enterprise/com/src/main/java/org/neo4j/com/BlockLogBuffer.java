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
package org.neo4j.com;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import org.jboss.netty.buffer.ChannelBuffer;
import org.neo4j.kernel.impl.transaction.xaframework.LogBuffer;

public class BlockLogBuffer implements LogBuffer
{
    static final byte FULL_BLOCK_AND_MORE = 0;
    static final int MAX_SIZE = 256/*incl. header*/;
    static final int DATA_SIZE = MAX_SIZE-1;

    private final ChannelBuffer target;
    private final byte[] byteArray = new byte[MAX_SIZE + 8/*largest atom*/];
    private final ByteBuffer byteBuffer = ByteBuffer.wrap( byteArray );

    public BlockLogBuffer( ChannelBuffer target )
    {
        this.target = target;
        clearInternalBuffer();
    }

    private void clearInternalBuffer()
    {
        byteBuffer.clear();
        // reserve space for size - assume we are going to fill the buffer
        byteBuffer.put( FULL_BLOCK_AND_MORE );
    }

    private LogBuffer checkFlush()
    {
        if ( byteBuffer.position() > MAX_SIZE )
        {
            target.writeBytes( byteArray, 0, MAX_SIZE );
            int pos = byteBuffer.position();
            clearInternalBuffer();
            byteBuffer.put( byteArray, MAX_SIZE, pos - MAX_SIZE );
        }
        return this;
    }

    public LogBuffer put( byte b ) throws IOException
    {
        byteBuffer.put( b );
        return checkFlush();
    }

    public LogBuffer putShort( short s ) throws IOException
    {
        byteBuffer.putShort( s );
        return checkFlush();
    }

    public LogBuffer putInt( int i ) throws IOException
    {
        byteBuffer.putInt( i );
        return checkFlush();
    }

    public LogBuffer putLong( long l ) throws IOException
    {
        byteBuffer.putLong( l );
        return checkFlush();
    }

    public LogBuffer putFloat( float f ) throws IOException
    {
        byteBuffer.putFloat( f );
        return checkFlush();
    }

    public LogBuffer putDouble( double d ) throws IOException
    {
        byteBuffer.putDouble( d );
        return checkFlush();
    }

    public LogBuffer put( byte[] bytes ) throws IOException
    {
        for ( int pos = 0; pos < bytes.length; )
        {
            int toWrite = Math.min( byteBuffer.remaining(), bytes.length - pos );
            byteBuffer.put( bytes, pos, toWrite );
            checkFlush();
            pos += toWrite;
        }
        return this;
    }

    public LogBuffer put( char[] chars ) throws IOException
    {
        for ( int bytePos = 0; bytePos < chars.length * 2; )
        {
            int bytesToWrite = Math.min( byteBuffer.remaining(), chars.length * 2 - bytePos );
            bytesToWrite -= ( bytesToWrite % 2 );
            for ( int i = 0; i < bytesToWrite / 2; i++ )
            {
                byteBuffer.putChar( chars[( bytePos / 2 ) + i] );
            }
            checkFlush();
            bytePos += bytesToWrite;
        }
        return this;
    }
    
    @Override
    public void writeOut() throws IOException
    {
        // Do nothing
    }

    public void force() throws IOException
    {
        // Do nothing
    }

    public long getFileChannelPosition() throws IOException
    {
        throw new UnsupportedOperationException( "BlockLogBuffer does not have a FileChannel" );
    }

    public FileChannel getFileChannel()
    {
        throw new UnsupportedOperationException( "BlockLogBuffer does not have a FileChannel" );
    }

    public void done()
    {
        assert byteBuffer.position() > 1 : "buffer should contain more than the header";
        assert byteBuffer.position() <= MAX_SIZE : "buffer should not be over full";
        byteBuffer.put( 0, (byte) ( byteBuffer.position() - 1 ) );
        byteBuffer.flip();
        target.writeBytes( byteBuffer );
        clearInternalBuffer();
    }

    public int write( ReadableByteChannel data ) throws IOException
    {
        int result = 0;
        int bytesRead = 0;
        while ( (bytesRead = data.read( byteBuffer )) >= 0 )
        {
            checkFlush();
            result += bytesRead;
        }
        return result;
    }
}
