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
package org.neo4j.server.rest.web;

import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.neo4j.helpers.collection.MapUtil.map;
import static org.neo4j.server.rest.repr.RepresentationTestAccess.serialize;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.server.ServerTestUtils;
import org.neo4j.server.database.Database;
import org.neo4j.server.database.DatabaseBlockedException;
import org.neo4j.server.rest.domain.EndNodeNotFoundException;
import org.neo4j.server.rest.domain.GraphDbHelper;
import org.neo4j.server.rest.domain.StartNodeNotFoundException;
import org.neo4j.server.rest.domain.TraverserReturnType;
import org.neo4j.server.rest.paging.FakeClock;
import org.neo4j.server.rest.paging.LeaseManager;
import org.neo4j.server.rest.repr.BadInputException;
import org.neo4j.server.rest.repr.ListRepresentation;
import org.neo4j.server.rest.repr.NodeRepresentation;
import org.neo4j.server.rest.repr.NodeRepresentationTest;
import org.neo4j.server.rest.repr.RelationshipRepresentation;
import org.neo4j.server.rest.repr.RelationshipRepresentationTest;
import org.neo4j.server.rest.web.DatabaseActions.RelationshipDirection;

public class DatabaseActionsTest
{
    private static DatabaseActions actions;
    private static GraphDbHelper graphdbHelper;
    private static Database database;
    private static String databasePath;
    private static LeaseManager leaseManager;

    @BeforeClass
    public static void clearDb() throws IOException
    {
        databasePath = ServerTestUtils.createTempDir()
                .getAbsolutePath();
        database = new Database( ServerTestUtils.EMBEDDED_GRAPH_DATABASE_FACTORY, databasePath );

        graphdbHelper = new GraphDbHelper( database );
        leaseManager = new LeaseManager( new FakeClock() );
        actions = new DatabaseActions( database, leaseManager );
    }

    @AfterClass
    public static void shutdownDatabase() throws IOException
    {
        database.shutdown();
        FileUtils.forceDelete( new File( databasePath ) );
    }

    private long createNode( Map<String, Object> properties ) throws DatabaseBlockedException
    {

        long nodeId;
        Transaction tx = database.graph.beginTx();
        try
        {
            Node node = database.graph.createNode();
            for ( Map.Entry<String, Object> entry : properties.entrySet() )
            {
                node.setProperty( entry.getKey(), entry.getValue() );
            }
            nodeId = node.getId();
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        return nodeId;
    }

    @Test
    public void createdNodeShouldBeInDatabase() throws Exception
    {
        NodeRepresentation noderep = actions.createNode( Collections.<String, Object>emptyMap() );

        Transaction tx = database.graph.beginTx();
        try
        {
            assertNotNull( database.graph.getNodeById( noderep.getId() ) );
        }
        finally
        {
            tx.finish();
        }
    }

    @Test
    public void nodeInDatabaseShouldBeRetreivable() throws DatabaseBlockedException, NodeNotFoundException
    {
        long nodeId = new GraphDbHelper( database ).createNode();
        assertNotNull( actions.getNode( nodeId ) );
    }

    @Test
    public void shouldBeAbleToStorePropertiesInAnExistingNode() throws DatabaseBlockedException,
            PropertyValueException, NodeNotFoundException
    {
        long nodeId = graphdbHelper.createNode();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "foo", "bar" );
        properties.put( "baz", 17 );
        actions.setAllNodeProperties( nodeId, properties );

        Transaction tx = database.graph.beginTx();
        try
        {
            Node node = database.graph.getNodeById( nodeId );
            assertHasProperties( node, properties );
        }
        finally
        {
            tx.finish();
        }
    }

    @Test( expected = PropertyValueException.class )
    public void shouldFailOnTryingToStoreMixedArraysAsAProperty() throws Exception
    {
        long nodeId = graphdbHelper.createNode();
        Map<String, Object> properties = new HashMap<String, Object>();
        Object[] dodgyArray = new Object[3];
        dodgyArray[0] = 0;
        dodgyArray[1] = 1;
        dodgyArray[2] = "two";
        properties.put( "foo", dodgyArray );

        actions.setAllNodeProperties( nodeId, properties );
    }

    @Test
    public void shouldOverwriteExistingProperties() throws DatabaseBlockedException, PropertyValueException,
            NodeNotFoundException
    {

        long nodeId;
        Transaction tx = database.graph.beginTx();
        try
        {
            Node node = database.graph.createNode();
            node.setProperty( "remove me", "trash" );
            nodeId = node.getId();
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "foo", "bar" );
        properties.put( "baz", 17 );
        actions.setAllNodeProperties( nodeId, properties );
        tx = database.graph.beginTx();
        try
        {
            Node node = database.graph.getNodeById( nodeId );
            assertHasProperties( node, properties );
            assertNull( node.getProperty( "remove me", null ) );
        }
        finally
        {
            tx.finish();
        }
    }

    @Test
    public void shouldBeAbleToGetPropertiesOnNode() throws DatabaseBlockedException, NodeNotFoundException
    {

        long nodeId;
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "foo", "bar" );
        properties.put( "neo", "Thomas A. Anderson" );
        properties.put( "number", 15L );
        Transaction tx = database.graph.beginTx();
        try
        {
            Node node = database.graph.createNode();
            for ( Map.Entry<String, Object> entry : properties.entrySet() )
            {
                node.setProperty( entry.getKey(), entry.getValue() );
            }
            nodeId = node.getId();
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        Map<String, Object> readProperties = serialize( actions.getAllNodeProperties( nodeId ) );
        assertEquals( properties, readProperties );
    }

    @Test
    public void shouldRemoveNodeWithNoRelationsFromDBOnDelete() throws DatabaseBlockedException, NodeNotFoundException,
            OperationFailureException
    {
        long nodeId;
        Transaction tx = database.graph.beginTx();
        try
        {
            Node node = database.graph.createNode();
            nodeId = node.getId();
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        int nodeCount = graphdbHelper.getNumberOfNodes();
        actions.deleteNode( nodeId );
        assertEquals( nodeCount - 1, graphdbHelper.getNumberOfNodes() );
    }

    @Test
    public void shouldBeAbleToSetPropertyOnNode() throws DatabaseBlockedException, PropertyValueException,
            NodeNotFoundException
    {
        long nodeId = createNode( Collections.<String, Object>emptyMap() );
        String key = "foo";
        Object value = "bar";
        actions.setNodeProperty( nodeId, key, value );
        assertEquals( Collections.singletonMap( key, value ), graphdbHelper.getNodeProperties( nodeId ) );
    }

    @Test
    public void shouldBeAbleToGetPropertyOnNode() throws DatabaseBlockedException, NodeNotFoundException,
            NoSuchPropertyException, BadInputException
    {
        String key = "foo";
        Object value = "bar";
        long nodeId = createNode( Collections.singletonMap( key, value ) );
        assertEquals( value, serialize( actions.getNodeProperty( nodeId, key ) ) );
    }

    @Test
    public void shouldBeAbleToRemoveNodeProperties() throws DatabaseBlockedException, NodeNotFoundException
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "foo", "bar" );
        properties.put( "number", 15 );
        long nodeId = createNode( properties );
        actions.removeAllNodeProperties( nodeId );

        Transaction tx = database.graph.beginTx();
        try
        {
            Node node = database.graph.getNodeById( nodeId );
            assertEquals( false, node.getPropertyKeys()
                    .iterator()
                    .hasNext() );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Test
    public void shouldStoreRelationshipsBetweenTwoExistingNodes() throws Exception
    {
        int relationshipCount = graphdbHelper.getNumberOfRelationships();
        actions.createRelationship( graphdbHelper.createNode(), graphdbHelper.createNode(), "LOVES",
                Collections.<String, Object>emptyMap() );
        assertEquals( relationshipCount + 1, graphdbHelper.getNumberOfRelationships() );
    }

    @Test
    public void shouldStoreSuppliedPropertiesWhenCreatingRelationship() throws Exception
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "string", "value" );
        properties.put( "integer", 17 );
        long relId = actions.createRelationship( graphdbHelper.createNode(), graphdbHelper.createNode(), "LOVES",
                properties )
                .getId();

        Transaction tx = database.graph.beginTx();
        try
        {
            Relationship rel = database.graph.getRelationshipById( relId );
            for ( String key : rel.getPropertyKeys() )
            {
                assertTrue( "extra property stored", properties.containsKey( key ) );
            }
            for ( Map.Entry<String, Object> entry : properties.entrySet() )
            {
                assertEquals( entry.getValue(), rel.getProperty( entry.getKey() ) );
            }
        }
        finally
        {
            tx.finish();
        }
    }

    @Test
    public void shouldNotCreateRelationshipBetweenNonExistentNodes() throws Exception
    {
        long nodeId = graphdbHelper.createNode();
        Map<String, Object> properties = Collections.<String, Object>emptyMap();
        try
        {
            actions.createRelationship( nodeId, nodeId * 1000, "Loves", properties );
            fail();
        }
        catch ( EndNodeNotFoundException e )
        {
            // ok
        }
        try
        {
            actions.createRelationship( nodeId * 1000, nodeId, "Loves", properties );
            fail();
        }
        catch ( StartNodeNotFoundException e )
        {
            // ok
        }
    }

    @Test
    public void shouldAllowCreateRelationshipWithSameStartAsEndNode() throws Exception
    {
        long nodeId = graphdbHelper.createNode();
        Map<String, Object> properties = Collections.<String, Object>emptyMap();
        RelationshipRepresentation rel = actions.createRelationship( nodeId, nodeId, "Loves", properties );
        assertNotNull( rel );

    }

    @Test
    public void shouldBeAbleToRemoveNodeProperty() throws DatabaseBlockedException, NodeNotFoundException,
            NoSuchPropertyException
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "foo", "bar" );
        properties.put( "number", 15 );
        long nodeId = createNode( properties );
        actions.removeNodeProperty( nodeId, "foo" );

        Transaction tx = database.graph.beginTx();
        try
        {
            Node node = database.graph.getNodeById( nodeId );
            assertEquals( 15, node.getProperty( "number" ) );
            assertEquals( false, node.hasProperty( "foo" ) );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Test
    public void shouldReturnTrueIfNodePropertyRemoved() throws DatabaseBlockedException, NodeNotFoundException,
            NoSuchPropertyException
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "foo", "bar" );
        properties.put( "number", 15 );
        long nodeId = createNode( properties );
        actions.removeNodeProperty( nodeId, "foo" );
    }

    @Test( expected = NoSuchPropertyException.class )
    public void shouldReturnFalseIfNodePropertyNotRemoved() throws DatabaseBlockedException, NodeNotFoundException,
            NoSuchPropertyException
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "foo", "bar" );
        properties.put( "number", 15 );
        long nodeId = createNode( properties );
        actions.removeNodeProperty( nodeId, "baz" );
    }

    @Test
    public void shouldBeAbleToRetrieveARelationship() throws DatabaseBlockedException, RelationshipNotFoundException
    {
        long relationship = graphdbHelper.createRelationship( "ENJOYED" );
        assertNotNull( actions.getRelationship( relationship ) );
    }

    @Test
    public void shouldBeAbleToGetPropertiesOnRelationship() throws DatabaseBlockedException,
            RelationshipNotFoundException
    {

        long relationshipId;
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "foo", "bar" );
        properties.put( "neo", "Thomas A. Anderson" );
        properties.put( "number", 15L );
        Transaction tx = database.graph.beginTx();
        try
        {
            Node startNode = database.graph.createNode();
            Node endNode = database.graph.createNode();
            Relationship relationship = startNode.createRelationshipTo( endNode,
                    DynamicRelationshipType.withName( "knows" ) );
            for ( Map.Entry<String, Object> entry : properties.entrySet() )
            {
                relationship.setProperty( entry.getKey(), entry.getValue() );
            }
            relationshipId = relationship.getId();
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        Map<String, Object> readProperties = serialize( actions.getAllRelationshipProperties( relationshipId ) );
        assertEquals( properties, readProperties );
    }

    @Test
    public void shouldBeAbleToRetrieveASinglePropertyFromARelationship() throws DatabaseBlockedException,
            NoSuchPropertyException, RelationshipNotFoundException, BadInputException
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "foo", "bar" );
        properties.put( "neo", "Thomas A. Anderson" );
        properties.put( "number", 15L );

        long relationshipId = graphdbHelper.createRelationship( "LOVES" );
        graphdbHelper.setRelationshipProperties( relationshipId, properties );

        Object relationshipProperty = serialize( actions.getRelationshipProperty( relationshipId, "foo" ) );
        assertEquals( "bar", relationshipProperty );
    }

    @Test
    public void shouldBeAbleToDeleteARelationship() throws DatabaseBlockedException, RelationshipNotFoundException
    {
        long relationshipId = graphdbHelper.createRelationship( "LOVES" );

        actions.deleteRelationship( relationshipId );
        try
        {
            graphdbHelper.getRelationship( relationshipId );
            fail();
        }
        catch ( NotFoundException e )
        {
        }
    }

    @Test
    public void shouldBeAbleToRetrieveRelationshipsFromNode() throws DatabaseBlockedException, NodeNotFoundException
    {
        long nodeId = graphdbHelper.createNode();
        graphdbHelper.createRelationship( "LIKES", nodeId, graphdbHelper.createNode() );
        graphdbHelper.createRelationship( "LIKES", graphdbHelper.createNode(), nodeId );
        graphdbHelper.createRelationship( "HATES", nodeId, graphdbHelper.createNode() );

        verifyRelReps( 3,
                actions.getNodeRelationships( nodeId, RelationshipDirection.all, Collections.<String>emptyList() ) );
        verifyRelReps( 1,
                actions.getNodeRelationships( nodeId, RelationshipDirection.in, Collections.<String>emptyList() ) );
        verifyRelReps( 2,
                actions.getNodeRelationships( nodeId, RelationshipDirection.out, Collections.<String>emptyList() ) );

        verifyRelReps( 3,
                actions.getNodeRelationships( nodeId, RelationshipDirection.all, Arrays.asList( "LIKES", "HATES" ) ) );
        verifyRelReps( 1,
                actions.getNodeRelationships( nodeId, RelationshipDirection.in, Arrays.asList( "LIKES", "HATES" ) ) );
        verifyRelReps( 2,
                actions.getNodeRelationships( nodeId, RelationshipDirection.out, Arrays.asList( "LIKES", "HATES" ) ) );

        verifyRelReps( 2, actions.getNodeRelationships( nodeId, RelationshipDirection.all, Arrays.asList( "LIKES" ) ) );
        verifyRelReps( 1, actions.getNodeRelationships( nodeId, RelationshipDirection.in, Arrays.asList( "LIKES" ) ) );
        verifyRelReps( 1, actions.getNodeRelationships( nodeId, RelationshipDirection.out, Arrays.asList( "LIKES" ) ) );

        verifyRelReps( 1, actions.getNodeRelationships( nodeId, RelationshipDirection.all, Arrays.asList( "HATES" ) ) );
        verifyRelReps( 0, actions.getNodeRelationships( nodeId, RelationshipDirection.in, Arrays.asList( "HATES" ) ) );
        verifyRelReps( 1, actions.getNodeRelationships( nodeId, RelationshipDirection.out, Arrays.asList( "HATES" ) ) );
    }

    @Test
    public void shouldNotGetAnyRelationshipsWhenRetrievingFromNodeWithoutRelationships()
            throws DatabaseBlockedException, NodeNotFoundException
    {
        long nodeId = graphdbHelper.createNode();

        verifyRelReps( 0,
                actions.getNodeRelationships( nodeId, RelationshipDirection.all, Collections.<String>emptyList() ) );
        verifyRelReps( 0,
                actions.getNodeRelationships( nodeId, RelationshipDirection.in, Collections.<String>emptyList() ) );
        verifyRelReps( 0,
                actions.getNodeRelationships( nodeId, RelationshipDirection.out, Collections.<String>emptyList() ) );
    }

    @Test
    public void shouldBeAbleToSetRelationshipProperties() throws DatabaseBlockedException, PropertyValueException,
            RelationshipNotFoundException
    {
        long relationshipId = graphdbHelper.createRelationship( "KNOWS" );
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "foo", "bar" );
        properties.put( "number", 10 );
        actions.setAllRelationshipProperties( relationshipId, properties );
        assertEquals( properties, graphdbHelper.getRelationshipProperties( relationshipId ) );
    }

    @Test
    public void shouldBeAbleToSetRelationshipProperty() throws DatabaseBlockedException, PropertyValueException,
            RelationshipNotFoundException
    {
        long relationshipId = graphdbHelper.createRelationship( "KNOWS" );
        String key = "foo";
        Object value = "bar";
        actions.setRelationshipProperty( relationshipId, key, value );
        assertEquals( Collections.singletonMap( key, value ), graphdbHelper.getRelationshipProperties( relationshipId ) );
    }

    @Test
    public void shouldRemoveRelationProperties() throws DatabaseBlockedException, RelationshipNotFoundException
    {
        long relId = graphdbHelper.createRelationship( "PAIR-PROGRAMS_WITH" );
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( "foo", "bar" );
        map.put( "baz", 22 );
        graphdbHelper.setRelationshipProperties( relId, map );

        actions.removeAllRelationshipProperties( relId );

        assertTrue( graphdbHelper.getRelationshipProperties( relId )
                .isEmpty() );
    }

    @Test
    public void shouldRemoveRelationshipProperty() throws DatabaseBlockedException, RelationshipNotFoundException,
            NoSuchPropertyException
    {
        long relId = graphdbHelper.createRelationship( "PAIR-PROGRAMS_WITH" );
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( "foo", "bar" );
        map.put( "baz", 22 );
        graphdbHelper.setRelationshipProperties( relId, map );

        actions.removeRelationshipProperty( relId, "foo" );
        assertEquals( 1, graphdbHelper.getRelationshipProperties( relId )
                .size() );
    }

    @SuppressWarnings( "unchecked" )
    private void verifyRelReps( int expectedSize, ListRepresentation repr )
    {
        List<Object> relreps = serialize( repr );
        assertEquals( expectedSize, relreps.size() );
        for ( Object relrep : relreps )
        {
            RelationshipRepresentationTest.verifySerialisation( (Map<String, Object>) relrep );
        }
    }

    private void assertHasProperties( PropertyContainer container, Map<String, Object> properties )
    {
        for ( Map.Entry<String, Object> entry : properties.entrySet() )
        {
            assertEquals( entry.getValue(), container.getProperty( entry.getKey() ) );
        }
    }

    @Test
    public void shouldBeAbleToIndexNode() throws DatabaseBlockedException
    {
        String key = "mykey";
        String value = "myvalue";
        long nodeId = graphdbHelper.createNode();
        String indexName = "node";

        actions.createNodeIndex( MapUtil.map( "name", indexName ) );

        assertFalse( serialize( actions.getIndexedNodes( indexName, key, value ) ).iterator()
                .hasNext() );
        actions.addToNodeIndex( indexName, key, value, nodeId );
        assertEquals( Arrays.asList( nodeId ), graphdbHelper.getIndexedNodes( indexName, key, value ) );
    }

    @Test
    public void shouldBeAbleToFulltextIndex() throws DatabaseBlockedException
    {
        String key = "key";
        String value = "the value with spaces";
        long nodeId = graphdbHelper.createNode();
        String indexName = "fulltext-node";
        graphdbHelper.createNodeFullTextIndex( indexName );
        assertFalse( serialize( actions.getIndexedNodes( indexName, key, value ) ).iterator()
                .hasNext() );
        actions.addToNodeIndex( indexName, key, value, nodeId );
        assertEquals( Arrays.asList( nodeId ), graphdbHelper.getIndexedNodes( indexName, key, value ) );
        assertEquals( Arrays.asList( nodeId ), graphdbHelper.getIndexedNodes( indexName, key, "the value with spaces" ) );
        assertEquals( Arrays.asList( nodeId ), graphdbHelper.queryIndexedNodes( indexName, key, "the" ) );
        assertEquals( Arrays.asList( nodeId ), graphdbHelper.queryIndexedNodes( indexName, key, "value" ) );
        assertEquals( Arrays.asList( nodeId ), graphdbHelper.queryIndexedNodes( indexName, key, "with" ) );
        assertEquals( Arrays.asList( nodeId ), graphdbHelper.queryIndexedNodes( indexName, key, "spaces" ) );
        assertEquals( Arrays.asList( nodeId ), graphdbHelper.queryIndexedNodes( indexName, key, "*spaces*" ) );
        assertTrue( graphdbHelper.getIndexedNodes( indexName, key, "nohit" )
                .isEmpty() );
    }

    @Test
    public void shouldBeAbleToGetReferenceNode() throws DatabaseBlockedException, NodeNotFoundException
    {
        NodeRepresentation rep = actions.getReferenceNode();
        actions.getNode( rep.getId() );
    }

    @Test
    public void shouldGetExtendedNodeRepresentationsWhenGettingFromIndex() throws DatabaseBlockedException
    {
        String key = "mykey3";
        String value = "value";

        long nodeId = graphdbHelper.createNode();
        String indexName = "node";
        graphdbHelper.addNodeToIndex( indexName, key, value, nodeId );
        int counter = 0;
        for ( Object rep : serialize( actions.getIndexedNodes( indexName, key, value ) ) )
        {
            Map<String, Object> serialized = (Map<String, Object>) rep;
            NodeRepresentationTest.verifySerialisation( serialized );
            assertNotNull( serialized.get( "indexed" ) );
            counter++;
        }
        assertEquals( 1, counter );
    }

    @Test
    public void shouldBeAbleToRemoveNodeFromIndex() throws DatabaseBlockedException
    {
        String key = "mykey2";
        String value = "myvalue";
        String value2 = "myvalue2";
        String indexName = "node";
        long nodeId = graphdbHelper.createNode();
        actions.addToNodeIndex( indexName, key, value, nodeId );
        actions.addToNodeIndex( indexName, key, value2, nodeId );
        assertEquals( 1, graphdbHelper.getIndexedNodes( indexName, key, value )
                .size() );
        assertEquals( 1, graphdbHelper.getIndexedNodes( indexName, key, value2 )
                .size() );
        actions.removeFromNodeIndex( indexName, key, value, nodeId );
        assertEquals( 0, graphdbHelper.getIndexedNodes( indexName, key, value )
                .size() );
        assertEquals( 1, graphdbHelper.getIndexedNodes( indexName, key, value2 )
                .size() );
        actions.removeFromNodeIndex( indexName, key, value2, nodeId );
        assertEquals( 0, graphdbHelper.getIndexedNodes( indexName, key, value )
                .size() );
        assertEquals( 0, graphdbHelper.getIndexedNodes( indexName, key, value2 )
                .size() );
    }

    @Test
    public void shouldBeAbleToRemoveNodeFromIndexWithoutKeyValue() throws DatabaseBlockedException
    {
        String key1 = "kvkey1";
        String key2 = "kvkey2";
        String value = "myvalue";
        String value2 = "myvalue2";
        String indexName = "node";
        long nodeId = graphdbHelper.createNode();
        actions.addToNodeIndex( indexName, key1, value, nodeId );
        actions.addToNodeIndex( indexName, key1, value2, nodeId );
        actions.addToNodeIndex( indexName, key2, value, nodeId );
        actions.addToNodeIndex( indexName, key2, value2, nodeId );
        assertEquals( 1, graphdbHelper.getIndexedNodes( indexName, key1, value )
                .size() );
        assertEquals( 1, graphdbHelper.getIndexedNodes( indexName, key1, value2 )
                .size() );
        assertEquals( 1, graphdbHelper.getIndexedNodes( indexName, key2, value )
                .size() );
        assertEquals( 1, graphdbHelper.getIndexedNodes( indexName, key2, value2 )
                .size() );
        actions.removeFromNodeIndexNoValue( indexName, key1, nodeId );
        assertEquals( 0, graphdbHelper.getIndexedNodes( indexName, key1, value )
                .size() );
        assertEquals( 0, graphdbHelper.getIndexedNodes( indexName, key1, value2 )
                .size() );
        assertEquals( 1, graphdbHelper.getIndexedNodes( indexName, key2, value )
                .size() );
        assertEquals( 1, graphdbHelper.getIndexedNodes( indexName, key2, value2 )
                .size() );
        actions.removeFromNodeIndexNoKeyValue( indexName, nodeId );
        assertEquals( 0, graphdbHelper.getIndexedNodes( indexName, key1, value )
                .size() );
        assertEquals( 0, graphdbHelper.getIndexedNodes( indexName, key1, value2 )
                .size() );
        assertEquals( 0, graphdbHelper.getIndexedNodes( indexName, key2, value )
                .size() );
        assertEquals( 0, graphdbHelper.getIndexedNodes( indexName, key2, value2 )
                .size() );
    }

    private long createBasicTraversableGraph() throws DatabaseBlockedException
    {
        // (Root)
        // / \
        // (Mattias) (Johan)
        // / / \
        // (Emil) (Peter) (Tobias)

        long startNode = graphdbHelper.createNode( MapUtil.map( "name", "Root" ) );
        long child1_l1 = graphdbHelper.createNode( MapUtil.map( "name", "Mattias" ) );
        graphdbHelper.createRelationship( "knows", startNode, child1_l1 );
        long child2_l1 = graphdbHelper.createNode( MapUtil.map( "name", "Johan" ) );
        graphdbHelper.createRelationship( "knows", startNode, child2_l1 );
        long child1_l2 = graphdbHelper.createNode( MapUtil.map( "name", "Emil" ) );
        graphdbHelper.createRelationship( "knows", child2_l1, child1_l2 );
        long child1_l3 = graphdbHelper.createNode( MapUtil.map( "name", "Peter" ) );
        graphdbHelper.createRelationship( "knows", child1_l2, child1_l3 );
        long child2_l3 = graphdbHelper.createNode( MapUtil.map( "name", "Tobias" ) );
        graphdbHelper.createRelationship( "loves", child1_l2, child2_l3 );
        return startNode;
    }

    private long[] createMoreComplexGraph() throws DatabaseBlockedException
    {
        // (a)
        // / \
        // v v
        // (b)<---(c) (d)-->(e)
        // \ / \ / /
        // v v v v /
        // (f)--->(g)<----

        long a = graphdbHelper.createNode();
        long b = graphdbHelper.createNode();
        long c = graphdbHelper.createNode();
        long d = graphdbHelper.createNode();
        long e = graphdbHelper.createNode();
        long f = graphdbHelper.createNode();
        long g = graphdbHelper.createNode();
        graphdbHelper.createRelationship( "to", a, c );
        graphdbHelper.createRelationship( "to", a, d );
        graphdbHelper.createRelationship( "to", c, b );
        graphdbHelper.createRelationship( "to", d, e );
        graphdbHelper.createRelationship( "to", b, f );
        graphdbHelper.createRelationship( "to", c, f );
        graphdbHelper.createRelationship( "to", f, g );
        graphdbHelper.createRelationship( "to", d, g );
        graphdbHelper.createRelationship( "to", e, g );
        graphdbHelper.createRelationship( "to", c, g );
        return new long[] { a, g };
    }

    private void createRelationshipWithProperties( long start, long end, Map<String, Object> properties )
    {
        long rel = graphdbHelper.createRelationship( "to", start, end );
        graphdbHelper.setRelationshipProperties( rel, properties );
    }

    private long[] createDijkstraGraph( boolean includeOnes ) throws DatabaseBlockedException
    {
        /* Layout:
         *                       (y)
         *                        ^
         *                        [2]  _____[1]___
         *                          \ v           |
         * (start)--[1]->(a)--[9]-->(x)<-        (e)--[2]->(f)
         *                |         ^ ^^  \       ^
         *               [1]  ---[7][5][4] -[3]  [1]
         *                v  /       | /      \  /
         *               (b)--[1]-->(c)--[1]->(d)
         */

        Map<String, Object> costOneProperties = includeOnes ? map( "cost", (double) 1 ) : map();
        long start = graphdbHelper.createNode();
        long a = graphdbHelper.createNode();
        long b = graphdbHelper.createNode();
        long c = graphdbHelper.createNode();
        long d = graphdbHelper.createNode();
        long e = graphdbHelper.createNode();
        long f = graphdbHelper.createNode();
        long x = graphdbHelper.createNode();
        long y = graphdbHelper.createNode();

        createRelationshipWithProperties( start, a, costOneProperties );
        createRelationshipWithProperties( a, x, map( "cost", (double) 9 ) );
        createRelationshipWithProperties( a, b, costOneProperties );
        createRelationshipWithProperties( b, x, map( "cost", (double) 7 ) );
        createRelationshipWithProperties( b, c, costOneProperties );
        createRelationshipWithProperties( c, x, map( "cost", (double) 5 ) );
        createRelationshipWithProperties( c, x, map( "cost", (double) 4 ) );
        createRelationshipWithProperties( c, d, costOneProperties );
        createRelationshipWithProperties( d, x, map( "cost", (double) 3 ) );
        createRelationshipWithProperties( d, e, costOneProperties );
        createRelationshipWithProperties( e, x, costOneProperties );
        createRelationshipWithProperties( e, f, map( "cost", (double) 2 ) );
        createRelationshipWithProperties( x, y, map( "cost", (double) 2 ) );
        return new long[] { start, x };
    }

    @Test
    public void shouldBeAbleToTraverseWithDefaultParameters() throws DatabaseBlockedException
    {
        long startNode = createBasicTraversableGraph();
        List<Object> hits = serialize( actions.traverse( startNode, new HashMap<String, Object>(),
                TraverserReturnType.node ) );
        assertEquals( 2, hits.size() );
    }

    @Test
    public void shouldBeAbleToTraverseDepthTwo() throws DatabaseBlockedException
    {
        long startNode = createBasicTraversableGraph();
        List<Object> hits = serialize( actions.traverse( startNode, MapUtil.map( "max_depth", 2 ),
                TraverserReturnType.node ) );
        assertEquals( 3, hits.size() );
    }

    @Test
    public void shouldBeAbleToTraverseEverything() throws DatabaseBlockedException
    {
        long startNode = createBasicTraversableGraph();
        List<Object> hits = serialize( actions.traverse(
                startNode,
                MapUtil.map( "return_filter", MapUtil.map( "language", "javascript", "body", "true;" ), "max_depth", 10 ),
                TraverserReturnType.node ) );
        assertEquals( 6, hits.size() );
        hits = serialize( actions.traverse( startNode,
                MapUtil.map( "return_filter", MapUtil.map( "language", "builtin", "name", "all" ), "max_depth", 10 ),
                TraverserReturnType.node ) );
        assertEquals( 6, hits.size() );
    }

    @Test
    public void shouldBeAbleToUseCustomReturnFilter() throws DatabaseBlockedException
    {
        long startNode = createBasicTraversableGraph();
        List<Object> hits = serialize( actions.traverse( startNode, MapUtil.map( "prune_evaluator", MapUtil.map(
                "language", "builtin", "name", "none" ), "return_filter", MapUtil.map( "language", "javascript",
                "body", "position.endNode().getProperty( 'name' ).contains( 'o' )" ) ), TraverserReturnType.node ) );
        assertEquals( 3, hits.size() );
    }

    @Test
    public void shouldBeAbleToTraverseWithMaxDepthAndPruneEvaluatorCombined() throws DatabaseBlockedException
    {
        long startNode = createBasicTraversableGraph();
        List<Object> hits = serialize( actions.traverse( startNode,
                MapUtil.map( "max_depth", 2, "prune_evaluator", MapUtil.map( "language", "javascript", "body",
                        "position.endNode().getProperty('name').equals('Emil')" ) ), TraverserReturnType.node ) );
        assertEquals( 3, hits.size() );
        hits = serialize( actions.traverse( startNode,
                MapUtil.map( "max_depth", 1, "prune_evaluator", MapUtil.map( "language", "javascript", "body",
                        "position.endNode().getProperty('name').equals('Emil')" ) ), TraverserReturnType.node ) );
        assertEquals( 2, hits.size() );
    }

    @Test
    public void shouldBeAbleToGetRelationshipsIfSpecified() throws DatabaseBlockedException
    {
        long startNode = createBasicTraversableGraph();
        ListRepresentation traverse = actions.traverse( startNode, new HashMap<String, Object>(),
                TraverserReturnType.relationship );
        List<Object> hits = serialize( traverse );
        for ( Object hit : hits )
        {
            RelationshipRepresentationTest.verifySerialisation( (Map<String, Object>) hit );
        }
    }

    @Test
    public void shouldBeAbleToGetPathsIfSpecified() throws DatabaseBlockedException
    {
        long startNode = createBasicTraversableGraph();
        List<Object> hits = serialize( actions.traverse( startNode, new HashMap<String, Object>(),
                TraverserReturnType.path ) );

        for ( Object hit : hits )
        {
            Map<String, Object> map = (Map<String, Object>) hit;
            assertThat( map, hasKey( "start" ) );
            assertThat( map, hasKey( "end" ) );
            assertThat( map, hasKey( "length" ) );
        }
    }

    @Test
    public void shouldBeAbleToGetFullPathsIfSpecified() throws DatabaseBlockedException
    {
        long startNode = createBasicTraversableGraph();
        List<Object> hits = serialize( actions.traverse( startNode, new HashMap<String, Object>(),
                TraverserReturnType.fullpath ) );

        for ( Object hit : hits )
        {
            Map<String, Object> map = (Map<String, Object>) hit;
            Collection<Object> relationships = (Collection<Object>) map.get( "relationships" );
            for ( Object relationship : relationships )
            {
                RelationshipRepresentationTest.verifySerialisation( (Map<String, Object>) relationship );
            }
            Collection<Object> nodes = (Collection<Object>) map.get( "nodes" );
            for ( Object node : nodes )
            {
                NodeRepresentationTest.verifySerialisation( (Map<String, Object>) node );
            }
            assertThat( map, hasKey( "start" ) );
            assertThat( map, hasKey( "end" ) );
            assertThat( map, hasKey( "length" ) );
        }
    }

    @Test
    public void shouldBeAbleToGetShortestPaths() throws Exception
    {
        long[] nodes = createMoreComplexGraph();

        // /paths
        List<Object> result = serialize( actions.findPaths(
                nodes[0],
                nodes[1],
                MapUtil.map( "max_depth", 2, "algorithm", "shortestPath", "relationships",
                        MapUtil.map( "type", "to", "direction", "out" ) ) ) );
        assertPaths( 2, nodes, 2, result );

        // /path
        Map<String, Object> path = serialize( actions.findSinglePath(
                nodes[0],
                nodes[1],
                MapUtil.map( "max_depth", 2, "algorithm", "shortestPath", "relationships",
                        MapUtil.map( "type", "to", "direction", "out" ) ) ) );
        assertPaths( 1, nodes, 2, Arrays.<Object>asList( path ) );

        // /path {single: false} (has no effect)
        path = serialize( actions.findSinglePath(
                nodes[0],
                nodes[1],
                MapUtil.map( "max_depth", 2, "algorithm", "shortestPath", "relationships",
                        MapUtil.map( "type", "to", "direction", "out" ), "single", false ) ) );
        assertPaths( 1, nodes, 2, Arrays.<Object>asList( path ) );
    }

    @Test
    public void shouldBeAbleToGetPathsUsingDijkstra() throws Exception
    {
        long[] nodes = createDijkstraGraph( true );

        // /paths
        List<Object> result = serialize( actions.findPaths(
                nodes[0],
                nodes[1],
                map( "algorithm", "dijkstra", "cost_property", "cost", "relationships",
                        map( "type", "to", "direction", "out" ) ) ) );
        assertPaths( 1, nodes, 6, result );

        // /path
        Map<String, Object> path = serialize( actions.findSinglePath(
                nodes[0],
                nodes[1],
                map( "algorithm", "dijkstra", "cost_property", "cost", "relationships",
                        map( "type", "to", "direction", "out" ) ) ) );
        assertPaths( 1, nodes, 6, Arrays.<Object>asList( path ) );
        assertEquals( 6.0d, path.get( "weight" ) );
    }

    @Test
    public void shouldBeAbleToGetPathsUsingDijkstraWithDefaults() throws Exception
    {
        long[] nodes = createDijkstraGraph( false );

        // /paths
        List<Object> result = serialize( actions.findPaths(
                nodes[0],
                nodes[1],
                map( "algorithm", "dijkstra", "cost_property", "cost", "default_cost", 1, "relationships",
                        map( "type", "to", "direction", "out" ) ) ) );
        assertPaths( 1, nodes, 6, result );

        // /path
        Map<String, Object> path = serialize( actions.findSinglePath(
                nodes[0],
                nodes[1],
                map( "algorithm", "dijkstra", "cost_property", "cost", "default_cost", 1, "relationships",
                        map( "type", "to", "direction", "out" ) ) ) );
        assertPaths( 1, nodes, 6, Arrays.<Object>asList( path ) );
        assertEquals( 6.0d, path.get( "weight" ) );
    }

    @Test( expected = NotFoundException.class )
    public void shouldHandleNoFoundPathsCorrectly()
    {
        long[] nodes = createMoreComplexGraph();
        serialize( actions.findSinglePath(
                nodes[0],
                nodes[1],
                map( "max_depth", 2, "algorithm", "shortestPath", "relationships",
                        map( "type", "to", "direction", "in" ), "single", false ) ) );
    }

    private void assertPaths( int numPaths, long[] nodes, int length, List<Object> result )
    {
        assertEquals( numPaths, result.size() );
        for ( Object path : result )
        {
            Map<String, Object> serialized = (Map<String, Object>) path;
            assertTrue( serialized.get( "start" )
                    .toString()
                    .endsWith( "/" + nodes[0] ) );
            assertTrue( serialized.get( "end" )
                    .toString()
                    .endsWith( "/" + nodes[1] ) );
            assertEquals( length, serialized.get( "length" ) );
        }
    }
}
