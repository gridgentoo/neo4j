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
import static org.neo4j.server.helpers.FunctionalTestHelper.CLIENT;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.server.NeoServerWithEmbeddedWebServer;
import org.neo4j.server.database.DatabaseBlockedException;
import org.neo4j.server.helpers.FunctionalTestHelper;
import org.neo4j.server.helpers.ServerHelper;
import org.neo4j.server.rest.domain.GraphDbHelper;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.domain.JsonParseException;
import org.neo4j.server.rest.domain.URIHelper;
import org.neo4j.server.rest.web.PropertyValueException;
import org.neo4j.test.TestData;

public class IndexRelationshipFunctionalTest
{
    private static NeoServerWithEmbeddedWebServer server;
    private static FunctionalTestHelper functionalTestHelper;
    private static GraphDbHelper helper;
    private static RestRequest request;
    public @Rule
    TestData<RESTDocsGenerator> gen = TestData.producedThrough( RESTDocsGenerator.PRODUCER );

    @BeforeClass
    public static void setupServer() throws IOException
    {
        server = ServerHelper.createServer();
        functionalTestHelper = new FunctionalTestHelper( server );
        helper = functionalTestHelper.getGraphDbHelper();
        request = RestRequest.req();
    }

    @Before
    public void cleanTheDatabase()
    {
        ServerHelper.cleanTheDatabase( server );
    }

    @AfterClass
    public static void stopServer()
    {
        server.stop();
    }

    /**
     * GET ${org.neo4j.server.rest.web}/index/relationship/
     * <p/>
     * TODO: could be abstract
     *
     * @return the Reponse
     */
    public JaxRsResponse httpGetIndexRelationshipRoot()
    {
        return httpGet( functionalTestHelper.relationshipIndexUri() );
    }

    /**
     * POST ${org.neo4j.server.rest.web}/index/relationship {
     * "name":"index-name" "config":{ // optional map of index configuration
     * params "key1":"value1", "key2":"value2" } }
<<<<<<< HEAD
=======
     *
     * POST ${org.neo4j.server.rest.web}/index/relationship/{indexName}/{key}/{
     * value} "http://uri.for.node.to.index"
>>>>>>> d3302c3... Improved the putIfAbsent/getOrCreate API and exposed it in the REST API.
     */
    @Test
    public void shouldCreateANamedRelationshipIndex() throws JsonParseException
    {
        String indexName = "favorites";
        int expectedIndexes = helper.getRelationshipIndexes().length + 1;
        Map<String, String> indexSpecification = new HashMap<String, String>();
        indexSpecification.put( "name", indexName );
        JaxRsResponse response = httpPostIndexRelationshipRoot( JsonHelper.createJsonFrom( indexSpecification ) );
        assertEquals( 201, response.getStatus() );
        assertNotNull( response.getHeaders()
                .get( "Location" )
                .get( 0 ) );
        assertEquals( expectedIndexes, helper.getRelationshipIndexes().length );
        assertNotNull( helper.getRelationshipIndex( indexName ) );

        // Add a relationship to the index
        String key = "key";
        String value = "value";
        String relationshipType = "related-to";
        long relationshipId = helper.createRelationship( relationshipType );
        response = httpPostIndexRelationshipNameKeyValue( indexName, relationshipId, key, value );
        assertEquals( Status.CREATED.getStatusCode(), response.getStatus() );
        String indexUri = response.getHeaders().get( "Location" ).get( 0 );
        assertNotNull( indexUri );
        assertEquals( Arrays.asList( (Long) relationshipId ), helper.getIndexedRelationships( indexName, key, value ) );

        // Get the relationship from the indexed URI (Location in header)
        response = httpGet( indexUri );
        assertEquals( 200, response.getStatus() );

        String discovredEntity = response.getEntity();

        Map<String, Object> map = JsonHelper.jsonToMap( discovredEntity );
        assertNotNull( map.get( "self" ) );
    }

    private JaxRsResponse httpPostIndexRelationshipRoot( String jsonIndexSpecification )
    {
        return RestRequest.req()
                .post( functionalTestHelper.relationshipIndexUri(), jsonIndexSpecification );
    }

    /**
     * POST ${org.neo4j.server.rest.web}/index/relationship/{indexName}/{key}/{
     * value} "http://uri.for.node.to.index"
     */
    @Test
    public void shouldRespondWith201CreatedWhenIndexingRelationship() throws DatabaseBlockedException,
            JsonParseException
    {
        final String key = "key";
        final String value = "value";
        final String indexName = "testy";
        helper.createRelationshipIndex( indexName );
        final String relationshipType = "related-to";
        final long relationshipId = helper.createRelationship( relationshipType );

        JaxRsResponse response = httpPostIndexRelationshipNameKeyValue( indexName, relationshipId, key, value );
        assertEquals( 201, response.getStatus() );
        assertNotNull( response.getHeaders()
                .get( "Location" )
                .get( 0 ) );
        assertEquals( Arrays.asList( (Long) relationshipId ), helper.getIndexedRelationships( indexName, key, value ) );
    }

    private JaxRsResponse httpGetIndexRelationshipNameKeyValue( String indexName, String key, String value )
    {
        return RestRequest.req()
                .get( functionalTestHelper.indexRelationshipUri( indexName, key, value ) );
    }

    private JaxRsResponse httpPostIndexRelationshipNameKeyValue( String indexName, long relationshipId, String key,
            String value )
    {
        return RestRequest.req()
                .post( functionalTestHelper.indexRelationshipUri( indexName ),
                        createJsonStringFor( relationshipId, key, value ) );
    }

    private String createJsonStringFor( final long relationshipId, final String key, final String value )
    {
        return "{\"key\": \"" + key + "\", \"value\": \"" + value + "\", \"uri\": \""
               + functionalTestHelper.relationshipUri( relationshipId ) + "\"}";
    }

    @Test
    public void shouldGetRelationshipRepresentationFromIndexUri() throws DatabaseBlockedException, JsonParseException
    {
        final String key = "key2";
        final String value = "value";

        final String indexName = "mindex";
        helper.createRelationshipIndex( indexName );
        final String relationshipType = "related-to";
        final long relationshipId = helper.createRelationship( relationshipType );

        JaxRsResponse response = httpPostIndexRelationshipNameKeyValue( indexName, relationshipId, key, value );

        assertEquals( Status.CREATED.getStatusCode(), response.getStatus() );
        String indexUri = response.getHeaders()
                .get( "Location" )
                .get( 0 );

        response = httpGet( indexUri );
        assertEquals( 200, response.getStatus() );

        String discovredEntity = response.getEntity();

        Map<String, Object> map = JsonHelper.jsonToMap( discovredEntity );
        assertNotNull( map.get( "self" ) );
    }

    private JaxRsResponse httpGet( String indexUri )
    {
        return request.get( indexUri );
    }

    @Test
    public void shouldGet404WhenRequestingIndexUriWhichDoesntExist() throws DatabaseBlockedException
    {
        String key = "key3";
        String value = "value";
        String indexName = "nosuchindex";
        String indexUri = functionalTestHelper.relationshipIndexUri() + indexName + "/" + key + "/" + value;
        JaxRsResponse response = httpGet( indexUri );
        assertEquals( Status.NOT_FOUND.getStatusCode(), response.getStatus() );
    }

    @Test
    public void shouldGet200AndArrayOfRelationshipRepsWhenGettingFromIndex() throws PropertyValueException
    {
        final long startNode = helper.createNode();
        final long endNode = helper.createNode();

        final String key = "key_get";
        final String value = "value";

        final String relationshipName1 = "related-to";
        final String relationshipName2 = "dislikes";

        String jsonString = jsonRelationshipCreationSpecification( relationshipName1, endNode, key, value );

        JaxRsResponse createRelationshipResponse = httpPostCreateRelationship( startNode, jsonString );
        assertEquals( 201, createRelationshipResponse.getStatus() );
        String relationshipLocation1 = createRelationshipResponse.getLocation()
                .toString();

        jsonString = jsonRelationshipCreationSpecification( relationshipName2, endNode, key, value );
        createRelationshipResponse = httpPostCreateRelationship( startNode, jsonString );
        assertEquals( 201, createRelationshipResponse.getStatus() );
        String relationshipLocation2 = createRelationshipResponse.getHeaders()
                .get( HttpHeaders.LOCATION )
                .get( 0 );

        String indexName = "matrix";
        JaxRsResponse indexCreationResponse = httpPostIndexRelationshipRoot( "{\"name\":\"" + indexName + "\"}" );
        assertEquals( 201, indexCreationResponse.getStatus() );

        JaxRsResponse indexedRelationshipResponse = httpPostIndexRelationshipNameKeyValue( indexName,
                functionalTestHelper.getRelationshipIdFromUri( relationshipLocation1 ), key, value );
        String indexLocation1 = indexedRelationshipResponse.getHeaders()
                .get( HttpHeaders.LOCATION )
                .get( 0 );
        indexedRelationshipResponse = httpPostIndexRelationshipNameKeyValue( indexName,
                functionalTestHelper.getRelationshipIdFromUri( relationshipLocation2 ), key, value );
        String indexLocation2 = indexedRelationshipResponse.getHeaders()
                .get( HttpHeaders.LOCATION )
                .get( 0 );

        Map<String, String> uriToName = new HashMap<String, String>();
        uriToName.put( indexLocation1.toString(), relationshipName1 );
        uriToName.put( indexLocation2.toString(), relationshipName2 );

        JaxRsResponse response = RestRequest.req()
                .get( functionalTestHelper.indexRelationshipUri( indexName, key, value ) );
        assertEquals( 200, response.getStatus() );
        Collection<?> items = (Collection<?>) JsonHelper.jsonToSingleValue( response.getEntity( String.class ) );
        int counter = 0;
        for ( Object item : items )
        {
            Map<?, ?> map = (Map<?, ?>) item;
            assertNotNull( map.get( "self" ) );
            String indexedUri = (String) map.get( "indexed" );
            assertEquals( uriToName.get( indexedUri ), map.get( "type" ) );
            counter++;
        }
        assertEquals( 2, counter );
        response.close();
    }

    private JaxRsResponse httpPostCreateRelationship( long startNode, String jsonString )
    {
        return RestRequest.req()
                .post( functionalTestHelper.dataUri() + "node/" + startNode + "/relationships", jsonString );
    }

    private String jsonRelationshipCreationSpecification( String relationshipName, long endNode, String key,
            String value )
    {
        return "{\"to\" : \"" + functionalTestHelper.dataUri() + "node/" + endNode + "\"," + "\"type\" : \""
               + relationshipName + "\", " + "\"data\" : {\"" + key + "\" : \"" + value + "\"}}";
    }

    @Test
    public void shouldGet200WhenGettingRelationshipFromIndexWithNoHits()
    {
        String indexName = "empty-index";
        helper.createRelationshipIndex( indexName );
        JaxRsResponse response = RestRequest.req()
                .get( functionalTestHelper.indexRelationshipUri( indexName, "non-existent-key", "non-existent-value" ) );
        assertEquals( 200, response.getStatus() );
    }

    @Test
    public void shouldGet200WhenQueryingIndex() throws PropertyValueException
    {
        String indexName = "bobTheIndex";
        String key = "bobsKey";
        String value = "bobsValue";
        long relationship = helper.createRelationship( "TYPE" );
        helper.addRelationshipToIndex( indexName, key, value, relationship );

        JaxRsResponse response = RestRequest.req()
                .get( functionalTestHelper.indexRelationshipUri( indexName ) + "?query=" + key + ":" + value );

        assertEquals( 200, response.getStatus() );
    }

    @Test
    public void shouldBeAbleToRemoveIndexing() throws DatabaseBlockedException, JsonParseException
    {
        String key1 = "kvkey1";
        String key2 = "kvkey2";
        String value1 = "value1";
        String value2 = "value2";
        String indexName = "kvrel";
        long relationship = helper.createRelationship( "some type" );
        helper.setRelationshipProperties( relationship,
                MapUtil.map( key1, value1, key1, value2, key2, value1, key2, value2 ) );
        helper.addRelationshipToIndex( indexName, key1, value1, relationship );
        helper.addRelationshipToIndex( indexName, key1, value2, relationship );
        helper.addRelationshipToIndex( indexName, key2, value1, relationship );
        helper.addRelationshipToIndex( indexName, key2, value2, relationship );
        assertEquals( 1, helper.getIndexedRelationships( indexName, key1, value1 ).size() );
        assertEquals( 1, helper.getIndexedRelationships( indexName, key1, value2 ).size() );
        assertEquals( 1, helper.getIndexedRelationships( indexName, key2, value1 ).size() );
        assertEquals( 1, helper.getIndexedRelationships( indexName, key2, value2 ).size() );
        JaxRsResponse response = RestRequest.req().delete(
                functionalTestHelper.relationshipIndexUri() + indexName + "/" + key1 + "/" + value1 + "/" + relationship );
        assertEquals( 204, response.getStatus() );
        assertEquals( 0, helper.getIndexedRelationships( indexName, key1, value1 ).size() );
        assertEquals( 1, helper.getIndexedRelationships( indexName, key1, value2 ).size() );
        assertEquals( 1, helper.getIndexedRelationships( indexName, key2, value1 ).size() );
        assertEquals( 1, helper.getIndexedRelationships( indexName, key2, value2 ).size() );
        response = RestRequest.req().delete(
                functionalTestHelper.relationshipIndexUri() + indexName + "/" + key2 + "/" + relationship );
        assertEquals( 204, response.getStatus() );
        assertEquals( 0, helper.getIndexedRelationships( indexName, key1, value1 ).size() );
        assertEquals( 1, helper.getIndexedRelationships( indexName, key1, value2 ).size() );
        assertEquals( 0, helper.getIndexedRelationships( indexName, key2, value1 ).size() );
        assertEquals( 0, helper.getIndexedRelationships( indexName, key2, value2 ).size() );
        response = RestRequest.req().delete(
                functionalTestHelper.relationshipIndexUri() + indexName + "/" + relationship );
        assertEquals( 204, response.getStatus() );
        assertEquals( 0, helper.getIndexedRelationships( indexName, key1, value1 ).size() );
        assertEquals( 0, helper.getIndexedRelationships( indexName, key1, value2 ).size() );
        assertEquals( 0, helper.getIndexedRelationships( indexName, key2, value1 ).size() );
        assertEquals( 0, helper.getIndexedRelationships( indexName, key2, value2 ).size() );

        // Delete the index
        response = RestRequest.req().delete( functionalTestHelper.indexRelationshipUri( indexName ) );
        assertEquals( 204, response.getStatus() );
    }

    @Test
    public void shouldBeAbleToIndexValuesContainingSpaces() throws Exception
    {
        final long startNodeId = helper.createNode();
        final long endNodeId = helper.createNode();
        final String relationshiptype = "tested-together";
        final long relationshipId = helper.createRelationship( relationshiptype, startNodeId, endNodeId );

        final String key = "key";
        final String value = "value with   spaces  in it";

        final String indexName = "spacey-values";
        helper.createRelationshipIndex( indexName );

        JaxRsResponse response = httpPostIndexRelationshipNameKeyValue( indexName, relationshipId, key, value );
        assertEquals( Status.CREATED.getStatusCode(), response.getStatus() );
        URI location = response.getLocation();
        response.close();
        response = httpGetIndexRelationshipNameKeyValue( indexName, key, URIHelper.encode( value ) );
        assertEquals( Status.OK.getStatusCode(), response.getStatus() );
        String responseEntity = response.getEntity();
        Collection<?> hits = (Collection<?>) JsonHelper.jsonToSingleValue( responseEntity );
        assertEquals( 1, hits.size() );
        response.close();

        CLIENT.resource( location )
                .delete();
        response = httpGetIndexRelationshipNameKeyValue( indexName, key, URIHelper.encode( value ) );
        assertEquals( 200, response.getStatus() );
        responseEntity = response.getEntity();
        hits = (Collection<?>) JsonHelper.jsonToSingleValue( responseEntity );
        assertEquals( 0, hits.size() );
        response.close();
    }

    @Test
    public void shouldRespondWith400WhenSendingCorruptJson() throws Exception
    {
        final String indexName = "botherable-index";
        helper.createRelationshipIndex( indexName );
        final String corruptJson = "{[}";
        JaxRsResponse response = RestRequest.req()
                .post( functionalTestHelper.indexRelationshipUri( indexName ), corruptJson );

        assertEquals( 400, response.getStatus() );
    }

    /**
     * Create a unique relationship in an index.
     */
    @Documented
    @Test
    public void get_or_create_relationship() throws Exception
    {
        final String index = "knowledge", key = "name", value = "Tobias";
        helper.createRelationshipIndex( index );
        long start = helper.createNode();
        long end = helper.createNode();
        gen.get()
           .expectedStatus( 201 /* created */)
           .payloadType( MediaType.APPLICATION_JSON_TYPE )
           .payload( "{\"key\": \"" + key + "\", \"value\":\"" + value +
                     "\", \"start\": \"" + functionalTestHelper.nodeUri( start ) +
                     "\", \"end\": \"" + functionalTestHelper.nodeUri( end ) +
                     "\", \"type\": \"" + index + "\"}" )
           .post( functionalTestHelper.relationshipIndexUri() + index + "/?unique" );
    }

    /**
     * Add a relationship to an index unless a relationship already exists for the given mapping.
     */
    @Documented
    @Test
    public void put_relationship_if_absent() throws Exception
    {
        final String index = "knowledge", key = "name", value = "Mattias";
        helper.createRelationshipIndex( index );
        gen.get()
           .expectedStatus( 201 /* created */)
           .payloadType( MediaType.APPLICATION_JSON_TYPE )
           .payload( "{\"key\": \"" + key + "\", \"value\":\"" + value +
                     "\", \"uri\": \"" + functionalTestHelper.relationshipUri( helper.createRelationship( index ) ) + "\"}" )
           .post( functionalTestHelper.relationshipIndexUri() + index + "/?unique" );
    }
}
