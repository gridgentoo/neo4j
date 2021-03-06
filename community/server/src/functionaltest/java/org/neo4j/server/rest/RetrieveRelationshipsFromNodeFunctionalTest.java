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
package org.neo4j.server.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.server.NeoServerWithEmbeddedWebServer;
import org.neo4j.server.database.DatabaseBlockedException;
import org.neo4j.server.helpers.FunctionalTestHelper;
import org.neo4j.server.helpers.ServerHelper;
import org.neo4j.server.rest.domain.GraphDbHelper;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.domain.JsonParseException;
import org.neo4j.server.rest.repr.RelationshipRepresentationTest;
import org.neo4j.test.TestData;

public class RetrieveRelationshipsFromNodeFunctionalTest
{

    private long nodeWithRelationships;
    private long nodeWithoutRelationships;
    private long nonExistingNode;

    private static NeoServerWithEmbeddedWebServer server;
    private static FunctionalTestHelper functionalTestHelper;
    private static GraphDbHelper helper;

    @BeforeClass
    public static void setupServer() throws IOException
    {
        server = ServerHelper.createServer();
        functionalTestHelper = new FunctionalTestHelper( server );
        helper = functionalTestHelper.getGraphDbHelper();
    }

    @Before
    public void setupTheDatabase()
    {
        ServerHelper.cleanTheDatabase( server );
        createSimpleGraph();
    }

    private void createSimpleGraph()
    {
        nodeWithRelationships = helper.createNode();
        helper.createRelationship( "LIKES", nodeWithRelationships, helper.createNode() );
        helper.createRelationship( "LIKES", helper.createNode(), nodeWithRelationships );
        helper.createRelationship( "HATES", nodeWithRelationships, helper.createNode() );
        nodeWithoutRelationships = helper.createNode();
        nonExistingNode = nodeWithoutRelationships * 100;
    }

    @AfterClass
    public static void stopServer()
    {
        server.stop();
    }

    public @Rule
    TestData<RESTDocsGenerator> gen = TestData.producedThrough( RESTDocsGenerator.PRODUCER );

    private JaxRsResponse sendRetrieveRequestToServer(long nodeId, String path) {
        return RestRequest.req().get(functionalTestHelper.nodeUri() + "/" + nodeId + "/relationships" + path);
    }

    private void verifyRelReps( int expectedSize, String json ) throws JsonParseException
    {
        List<Map<String, Object>> relreps = JsonHelper.jsonToList( json );
        assertEquals( expectedSize, relreps.size() );
        for ( Map<String, Object> relrep : relreps )
        {
            RelationshipRepresentationTest.verifySerialisation( relrep );
        }
    }

    /**
     * Get all relationships.
     */
    @Documented
    @Test
    public void shouldRespondWith200AndListOfRelationshipRepresentationsWhenGettingAllRelationshipsForANode()
            throws JsonParseException
    {
        String entity = gen.get()
                .expectedStatus( 200 )
                .get( functionalTestHelper.nodeUri() + "/" + nodeWithRelationships + "/relationships" + "/all" )
                .entity();
        verifyRelReps( 3, entity );
    }

    /**
     * Get incoming relationships.
     */
    @Documented
    @Test
    public void shouldRespondWith200AndListOfRelationshipRepresentationsWhenGettingIncomingRelationshipsForANode()
            throws JsonParseException
    {
        String entity = gen.get()
                .expectedStatus( 200 )
                .get( functionalTestHelper.nodeUri() + "/" + nodeWithRelationships + "/relationships" + "/in" )
                .entity();
        verifyRelReps( 1, entity );
    }

    /**
     * Get outgoing relationships.
     */
    @Documented
    @Test
    public void shouldRespondWith200AndListOfRelationshipRepresentationsWhenGettingOutgoingRelationshipsForANode()
            throws JsonParseException
    {
        String entity = gen.get()
                .expectedStatus( 200 )
                .get( functionalTestHelper.nodeUri() + "/" + nodeWithRelationships + "/relationships" + "/out" )
                .entity();
        verifyRelReps( 2, entity );
    }

    /**
     * Get typed relationships.
     * 
     * Note that the "+&+" needs to be escaped for example when using
     * http://curl.haxx.se/[cURL] from the terminal.
     */
    @Documented
    @Test
    public void shouldRespondWith200AndListOfRelationshipRepresentationsWhenGettingAllTypedRelationshipsForANode()
            throws JsonParseException
    {
        String entity = gen.get()
                .expectedStatus( 200 )
                .get( functionalTestHelper.nodeUri() + "/" + nodeWithRelationships + "/relationships"
                      + "/all/LIKES&HATES" )
                .entity();
        verifyRelReps( 3, entity );
    }

    @Test
    public void shouldRespondWith200AndListOfRelationshipRepresentationsWhenGettingIncomingTypedRelationshipsForANode()
            throws JsonParseException
    {
        JaxRsResponse response = sendRetrieveRequestToServer( nodeWithRelationships, "/in/LIKES" );
        assertEquals( 200, response.getStatus() );
        assertEquals( MediaType.APPLICATION_JSON_TYPE, response.getType() );
        verifyRelReps( 1, response.getEntity( String.class ) );
        response.close();
    }

    @Test
    public void shouldRespondWith200AndListOfRelationshipRepresentationsWhenGettingOutgoingTypedRelationshipsForANode()
            throws JsonParseException
    {
        JaxRsResponse response = sendRetrieveRequestToServer( nodeWithRelationships, "/out/HATES" );
        assertEquals( 200, response.getStatus() );
        assertEquals( MediaType.APPLICATION_JSON_TYPE, response.getType() );
        verifyRelReps( 1, response.getEntity( String.class ) );
        response.close();
    }

    /**
     * Get relationships on a node without relationships.
     */
    @Documented
    @Test
    public void shouldRespondWith200AndEmptyListOfRelationshipRepresentationsWhenGettingAllRelationshipsForANodeWithoutRelationships()
            throws JsonParseException
    {
        String entity = gen.get()
                .expectedStatus( 200 )
                .get( functionalTestHelper.nodeUri() + "/" + nodeWithoutRelationships + "/relationships" + "/all" )
                .entity();
        verifyRelReps( 0, entity );
    }

    @Test
    public void shouldRespondWith200AndEmptyListOfRelationshipRepresentationsWhenGettingIncomingRelationshipsForANodeWithoutRelationships()
            throws JsonParseException
    {
        JaxRsResponse response = sendRetrieveRequestToServer( nodeWithoutRelationships, "/in" );
        assertEquals( 200, response.getStatus() );
        assertEquals( MediaType.APPLICATION_JSON_TYPE, response.getType() );
        verifyRelReps( 0, response.getEntity( String.class ) );
        response.close();
    }

    @Test
    public void shouldRespondWith200AndEmptyListOfRelationshipRepresentationsWhenGettingOutgoingRelationshipsForANodeWithoutRelationships()
            throws JsonParseException
    {
        JaxRsResponse response = sendRetrieveRequestToServer( nodeWithoutRelationships, "/out" );
        assertEquals( 200, response.getStatus() );
        assertEquals( MediaType.APPLICATION_JSON_TYPE, response.getType() );
        verifyRelReps( 0, response.getEntity( String.class ) );
        response.close();
    }

    @Test
    public void shouldRespondWith404WhenGettingAllRelationshipsForNonExistingNode()
    {
        JaxRsResponse response = sendRetrieveRequestToServer( nonExistingNode, "/all" );
        assertEquals( 404, response.getStatus() );
        response.close();
    }

    @Test
    public void shouldRespondWith404WhenGettingIncomingRelationshipsForNonExistingNode()
    {
        JaxRsResponse response = sendRetrieveRequestToServer( nonExistingNode, "/in" );
        assertEquals( 404, response.getStatus() );
        response.close();
    }

    @Test
    public void shouldRespondWith404WhenGettingOutgoingRelationshipsForNonExistingNode()
    {
        JaxRsResponse response = sendRetrieveRequestToServer( nonExistingNode, "/out" );
        assertEquals( 404, response.getStatus() );
        response.close();
    }

    @Test
    public void shouldGet200WhenRetrievingValidRelationship() throws DatabaseBlockedException {
        long relationshipId = helper.createRelationship("LIKES");

        JaxRsResponse response = RestRequest.req().get(functionalTestHelper.relationshipUri(relationshipId));

        assertEquals(200, response.getStatus());
        response.close();
    }

    @Test
    public void shouldGetARelationshipRepresentationInJsonWhenRetrievingValidRelationship() throws Exception {
        long relationshipId = helper.createRelationship("LIKES");

        JaxRsResponse response = RestRequest.req().get(functionalTestHelper.relationshipUri(relationshipId));

        String entity = response.getEntity(String.class);
        assertNotNull(entity);
        isLegalJson(entity);
        response.close();
    }

    private void isLegalJson( String entity ) throws IOException, JsonParseException
    {
        JsonHelper.jsonToMap( entity );
    }
}
