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
package org.neo4j.server.webadmin.console;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.server.database.Database;
import org.neo4j.test.ImpermanentGraphDatabase;

public class GremlinSessionTest
{
    private static final String TARGET_TEMPDB = "target/tempdb";
    private static final String NEWLINE = System.getProperty( "line.separator" );
    private ScriptSession session;
    private Database database;

    @Test
    public void retrievesTheReferenceNode()
    {
        String result = session.evaluate( "g" );

        assertEquals( String.format( "neo4jgraph[%s]" + NEWLINE, database.graph.toString() ), result );
    }

    @Test
    public void multiLineTest()
    {
        String result = session.evaluate( "for (i in 0..2) {" );
        result = session.evaluate( "println 'hi'" );
        result = session.evaluate( "}" );
        result = session.evaluate( "i = 2" );

        assertEquals( "2" + NEWLINE, result );
    }

    @Test
    public void canCreateNodesAndEdgesInGremlinLand()
    {
        String result = session.evaluate( "g.addVertex(null)" );
        assertEquals( "v[1]" + NEWLINE, result );
        result = session.evaluate( "g.V >> 2" );
        assertEquals( "v[0]" + NEWLINE + "v[1]" + NEWLINE, result );
        result = session.evaluate( "g.addVertex(null)" );
        assertEquals( "v[2]" + NEWLINE, result );
        result = session.evaluate( "g.addEdge(g.v(1), g.v(2), 'knows')" );
        assertEquals( "e[0][1-knows->2]" + NEWLINE, result );
        result = session.evaluate( "g.v(1).out" );
        assertEquals( "v[2]" + NEWLINE, result );
    }

    @Before
    public void setUp() throws Exception
    {
        this.database = new Database( new ImpermanentGraphDatabase( TARGET_TEMPDB ) );
        session = new GremlinSession( database );
    }

    @After
    public void shutdownDatabase()
    {
        this.database.shutdown();
    }
}
