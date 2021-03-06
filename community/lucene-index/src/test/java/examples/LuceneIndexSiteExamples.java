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
package examples;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.index.Neo4jTestCase;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public class LuceneIndexSiteExamples
{
    private static GraphDatabaseService graphDb;
    private Transaction tx;
    
    @BeforeClass
    public static void setUpDb()
    {
        String path = "target/var/examples";
        Neo4jTestCase.deleteFileOrDirectory( new File( path ) );
        graphDb = new EmbeddedGraphDatabase( path );
    }
    
    @Before
    public void beginTx()
    {
        tx = graphDb.beginTx();
    }
    
    @After
    public void finishTx()
    {
        tx.success();
        tx.finish();
    }
    
    @Test
    public void addSomeThings()
    {
        // START SNIPPET: add
        Index<Node> persons = graphDb.index().forNodes( "persons" );
        Node morpheus = graphDb.createNode();
        Node trinity = graphDb.createNode();
        Node neo = graphDb.createNode();
        persons.add( morpheus, "name", "Morpheus" );
        persons.add( morpheus, "rank", "Captain" );
        persons.add( trinity, "name", "Trinity" );
        persons.add( neo, "name", "Neo" );
        persons.add( neo, "title", "The One" );
        // END SNIPPET: add
    }
    
    @Test
    public void doSomeGets()
    {
        Index<Node> persons = graphDb.index().forNodes( "persons" );
        
        // START SNIPPET: get
        Node morpheus = persons.get( "name", "Morpheus" ).getSingle();
        // END SNIPPET: get
    }

    @Test
    public void doSomeQueries()
    {
        Index<Node> persons = graphDb.index().forNodes( "persons" );
        
        // START SNIPPET: query
        for ( Node person : persons.query( "name", "*e*" ) )
        {
            // It will get Morpheus and Neo
        }
        Node neo = persons.query( "name:*e* AND title:\"The One\"" ).getSingle();
        // END SNIPPET: query
    }
}
