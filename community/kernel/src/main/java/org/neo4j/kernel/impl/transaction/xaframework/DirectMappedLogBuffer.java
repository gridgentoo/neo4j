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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class DirectMappedLogBuffer implements LogBuffer
{
    // 500k
    static final int BUFFER_SIZE = 1024 * 512;

    private final FileChannel fileChannel;

    private CloseableByteBuffer byteBuffer = null;
    private long bufferStartPosition;

    public DirectMappedLogBuffer( FileChannel fileChannel ) throws IOException
    {
        this.fileChannel = fileChannel;
        bufferStartPosition = fileChannel.position();
        byteBuffer = CloseableByteBuffer.wrap( ByteBuffer.allocateDirect( BUFFER_SIZE ) );
    }

    private void ensureCapacity( int plusSize ) throws IOException
    {
        if ( byteBuffer == null
                || ( BUFFER_SIZE - byteBuffer.position() ) < plusSize )
        {
            writeOut();
        }
    }

    public LogBuffer put( byte b ) throws IOException
    {
        ensureCapacity( 1 );
        byteBuffer.put( b );
        return this;
    }

    public LogBuffer putShort( short s ) throws IOException
    {
        ensureCapacity( 2 );
        byteBuffer.putShort( s );
        return this;
    }

    public LogBuffer putInt( int i ) throws IOException
    {
        ensureCapacity( 4 );
        byteBuffer.putInt( i );
        return this;
    }

    public LogBuffer putLong( long l ) throws IOException
    {
        ensureCapacity( 8 );
        byteBuffer.putLong( l );
        return this;
    }

    public LogBuffer putFloat( float f ) throws IOException
    {
        ensureCapacity( 4 );
        byteBuffer.putFloat( f );
        return this;
    }

    public LogBuffer putDouble( double d ) throws IOException
    {
        ensureCapacity( 8 );
        byteBuffer.putDouble( d );
        return this;
    }

    public LogBuffer put( byte[] bytes ) throws IOException
    {
        put( bytes, 0 );
        return this;
    }

    private void put( byte[] bytes, int offset ) throws IOException
    {
        int bytesToWrite = bytes.length - offset;
        if ( bytesToWrite > BUFFER_SIZE )
        {
            bytesToWrite = BUFFER_SIZE;
        }
        ensureCapacity( bytesToWrite );
        byteBuffer.put( bytes, offset, bytesToWrite );
        offset += bytesToWrite;
        if ( offset < bytes.length )
        {
            put( bytes, offset );
        }
    }

    public LogBuffer put( char[] chars ) throws IOException
    {
        put( chars, 0 );
        return this;
    }

    private void put( char[] chars, int offset ) throws IOException
    {
        int charsToWrite = chars.length - offset;
        if ( charsToWrite * 2 > BUFFER_SIZE )
        {
            charsToWrite = BUFFER_SIZE / 2;
        }
        ensureCapacity( charsToWrite * 2 );
        int oldPos = byteBuffer.position();
        byteBuffer.asCharBuffer().put( chars, offset, charsToWrite );
        byteBuffer.position( oldPos + ( charsToWrite * 2 ) );
        offset += charsToWrite;
        if ( offset < chars.length )
        {
            put( chars, offset );
        }
    }
    
    @Override
    public void writeOut() throws IOException
    {
        byteBuffer.flip();
        bufferStartPosition += fileChannel.write( byteBuffer.getDelegate(),
                bufferStartPosition );
        byteBuffer.clear();
    }

    public void force() throws IOException
    {
        writeOut();
        fileChannel.force( false );
    }

    public long getFileChannelPosition()
    {
        if ( byteBuffer != null )
        {
            return bufferStartPosition + byteBuffer.position();
        }
        return bufferStartPosition;
    }

    public FileChannel getFileChannel()
    {
        return fileChannel;
    }

    public CloseableByteBuffer getBuffer()
    {
        return byteBuffer.duplicate();
    }
}
