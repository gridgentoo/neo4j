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
package org.neo4j.cypher.javacompat;

import static org.neo4j.visualization.asciidoc.AsciidocHelper.createCypherSnippet;
import static org.neo4j.visualization.asciidoc.AsciidocHelper.createQueryResultSnippet;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.test.GraphDescription;
import org.neo4j.test.GraphDescription.Graph;
import org.neo4j.test.GraphHolder;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.neo4j.test.JavaTestDocsGenerator;
import org.neo4j.test.TestData;
import org.neo4j.visualization.asciidoc.AsciidocHelper;

public class IntroExamplesTest implements GraphHolder
{
    public @Rule
    TestData<JavaTestDocsGenerator> gen = TestData.producedThrough( JavaTestDocsGenerator.PRODUCER );
    public @Rule
    TestData<Map<String, Node>> data = TestData.producedThrough( GraphDescription.createGraphFor(
            this, true ) );
    private ImpermanentGraphDatabase graphdb;
    private ExecutionEngine engine;
    private CypherParser parser;

    @Test
    @Graph( value = { "John friend Sara", "John friend Joe",
            "Sara friend Maria", "Joe friend Steve" }, autoIndexNodes = true )
    public void intro_examples() throws Exception
    {
        FileWriter fw = gen.get().getFW( "target/docs/", gen.get().getTitle() );
        data.get();
        fw.append( "\nImagine an example graph like\n\n" );
        fw.append( AsciidocHelper.createGraphViz( "Example Graph", graphdb(),
                "cypher-intro" ) );

        fw.append( "For example, here is a query which finds a user called John in an index and then traverses the graph looking for friends of Johns friends (though not his direct friends) before returning both John and any friends-of-friends that are found." );
        fw.append( "\n" );
        String query = "START john=node:node_auto_index(name = 'John') "
                       + "MATCH john-[:friend]->()-[:friend]->fof RETURN john, fof ";
        fw.append( createCypherSnippet( query ) );
        fw.append( "\nResulting in \n" );
        fw.append( createQueryResultSnippet( engine.execute(
                parser.parse( query ) ).toString() ) );

        fw.append( "Next up we will add filtering to set all four parts "
                   + "in motion:\n\nIn this next example, we take a list of users "
                   + "(by node ID) and traverse the graph looking for those other "
                   + "users that have an outgoing +friend+ relationship, returning "
                   + "only those followed users who have a +name+ property starting with +S+." );
        query = "START user=node("
                + data.get().get( "Joe" ).getId()
                + ","
                + data.get().get( "John" ).getId()
                + ","
                + data.get().get( "Sara" ).getId()
                + ","
                + data.get().get( "Maria" ).getId()
                + ","
                + data.get().get( "Steve" ).getId()
                + ") MATCH user-[:friend]->follower WHERE follower.name =~ /S.*/ RETURN user, follower.name ";
        fw.append( "\n" );
        fw.append( createCypherSnippet( query ) );
        fw.append( "\nResulting in\n" );
        fw.append( createQueryResultSnippet( engine.execute(
                parser.parse( query ) ).toString() ) );
        fw.close();
    }

    @Before
    public void setup() throws IOException
    {
        graphdb = new ImpermanentGraphDatabase();
        graphdb.cleanContent( false );

        parser = new CypherParser();
        engine = new ExecutionEngine( graphdb );
    }

    @Override
    public GraphDatabaseService graphdb()
    {
        return graphdb;
    }
}
