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
package org.neo4j.shell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.neo4j.graphdb.DynamicRelationshipType.withName;

import java.io.File;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.impl.util.FileUtils;
import org.neo4j.shell.impl.SameJvmClient;
import org.neo4j.shell.kernel.GraphDatabaseShellServer;

public class TestApps extends AbstractShellTest
{
    @Test
    public void variationsOfCdAndPws() throws Exception
    {
        Relationship[] relationships = createRelationshipChain( 3 );
        executeCommand( "cd" );
        executeCommand( "pwd", pwdOutputFor( relationships[0].getStartNode() ) );
        executeCommandExpectingException( "cd " + relationships[0].getStartNode().getId(), "stand" );
        executeCommand( "pwd", pwdOutputFor( relationships[0].getStartNode() ) );
        executeCommand( "cd " + relationships[0].getEndNode().getId() );
        executeCommand( "pwd", pwdOutputFor( relationships[0].getStartNode(), relationships[0].getEndNode() ) );
        executeCommandExpectingException( "cd " + relationships[2].getEndNode().getId(), "connected" );
        executeCommand( "pwd", pwdOutputFor( relationships[0].getStartNode(), relationships[0].getEndNode() ) );
        executeCommand( "cd -a " + relationships[2].getEndNode().getId() );
        executeCommand( "pwd", pwdOutputFor( relationships[0].getStartNode(), relationships[0].getEndNode(), relationships[2].getEndNode() ) );
        executeCommand( "cd .." );
        executeCommand( "pwd", pwdOutputFor( relationships[0].getStartNode(), relationships[0].getEndNode() ) );
        executeCommand( "cd " + relationships[1].getEndNode().getId() );
        executeCommand( "pwd", pwdOutputFor( relationships[0].getStartNode(), relationships[0].getEndNode(), relationships[1].getEndNode() ) );
    }

    @Test
    public void canSetPropertiesAndLsWithFilters() throws Exception
    {
        RelationshipType type1 = DynamicRelationshipType.withName( "KNOWS" );
        RelationshipType type2 = DynamicRelationshipType.withName( "LOVES" );
        Relationship[] relationships = createRelationshipChain( type1, 2 );
        Node node = relationships[0].getEndNode();
        createRelationshipChain( node, type2, 1 );
        executeCommand( "cd " + node.getId() );
        executeCommand( "ls", "<-", "->" );
        executeCommand( "ls -p", "!Neo" );
        setProperty( node, "name", "Neo" );
        executeCommand( "ls -p", "Neo" );
        executeCommand( "ls", "<-", "->", "Neo", type1.name(), type2.name() );
        executeCommand( "ls -r", "<-", "->", "!Neo" );
        executeCommand( "ls -rf .*:out", "!<-", "->", "!Neo", type1.name(), type2.name() );
        executeCommand( "ls -rf .*:in", "<-", "!->", "!Neo", type1.name(), "!" + type2.name() );
        executeCommand( "ls -rf KN.*:in", "<-", "!->", type1.name(), "!" + type2.name() );
        executeCommand( "ls -rf LOVES:in", "!<-", "!->", "!" + type1.name(), "!" + type2.name() );
        executeCommand( "ls -pf something", "!<-", "!->", "!Neo" );
        executeCommand( "ls -pf name", "!<-", "!->", "Neo" );
        executeCommand( "ls -pf name:Something", "!<-", "!->", "!Neo" );
        executeCommand( "ls -pf name:Neo", "!<-", "!->", "Neo" );
    }

    @Test
    public void canSetAndRemoveProperties() throws Exception
    {
        Relationship[] relationships = createRelationshipChain( 2 );
        Node node = relationships[0].getEndNode();
        executeCommand( "cd " + node.getId() );
        String name = "Mattias";
        executeCommand( "set name " + name );
        int age = 31;
        executeCommand( "set age -t int " + age );
        executeCommand( "set \"some property\" -t long[] \"[1234,5678]" );
        assertEquals( name, node.getProperty( "name" ) );
        assertEquals( age, node.getProperty( "age" ) );
        long[] value = (long[]) node.getProperty( "some property" );
        assertTrue( Arrays.equals( new long[] { 1234L, 5678L }, value ) );

        executeCommand( "rm age" );
        assertNull( node.getProperty( "age", null ) );
        assertEquals( name, node.getProperty( "name" ) );
    }

    @Test
    public void canCreateRelationshipsAndNodes() throws Exception
    {
        RelationshipType type1 = withName( "type1" );
        RelationshipType type2 = withName( "type2" );
        RelationshipType type3 = withName( "type3" );

        // No type supplied
        executeCommandExpectingException( "mkrel -c", "type" );

        executeCommand( "mkrel -ct " + type1.name() );
        Relationship relationship = db.getReferenceNode().getSingleRelationship( type1, Direction.OUTGOING );
        Node node = relationship.getEndNode();
        executeCommand( "mkrel -t " + type2.name() + " " + node.getId() );
        Relationship otherRelationship = db.getReferenceNode().getSingleRelationship( type2, Direction.OUTGOING );
        assertEquals( node, otherRelationship.getEndNode() );

        // With properties
        executeCommand( "mkrel -ct " + type3.name() + " --np \"{'name':'Neo','destiny':'The one'}\" --rp \"{'number':11}\"" );
        Relationship thirdRelationship = db.getReferenceNode().getSingleRelationship( type3, Direction.OUTGOING );
        assertEquals( 11, thirdRelationship.getProperty( "number" ) );
        Node thirdNode = thirdRelationship.getEndNode();
        assertEquals( "Neo", thirdNode.getProperty( "name" ) );
        assertEquals( "The one", thirdNode.getProperty( "destiny" ) );
        executeCommand( "cd -r " + thirdRelationship.getId() );
        executeCommand( "mv number other-number" );
        assertNull( thirdRelationship.getProperty( "number", null ) );
        assertEquals( 11, thirdRelationship.getProperty( "other-number" ) );
    }

    @Test
    public void rmrelCanLeaveStrandedIslands() throws Exception
    {
        Relationship[] relationships = createRelationshipChain( 4 );
        executeCommand( "cd -a " + relationships[1].getEndNode().getId() );

        Relationship relToDelete = relationships[2];
        executeCommandExpectingException( "rmrel " + relToDelete.getId(), "decoupled" );
        assertRelationshipExists( relToDelete );

        Node otherNode = relToDelete.getEndNode();
        executeCommand( "rmrel -fd " + relToDelete.getId() );
        assertRelationshipDoesntExist( relToDelete );
        assertNodeExists( otherNode );
    }

    @Test
    public void rmrelCanLeaveStrandedNodes() throws Exception
    {
        Relationship[] relationships = createRelationshipChain( 1 );
        Node otherNode = relationships[0].getEndNode();

        executeCommandExpectingException( "rmrel " + relationships[0].getId(), "decoupled" );
        assertRelationshipExists( relationships[0] );
        assertNodeExists( otherNode );

        executeCommand( "rmrel -f " + relationships[0].getId() );
        assertRelationshipDoesntExist( relationships[0] );
        assertNodeExists( otherNode );
    }

    @Test
    public void rmrelCanDeleteStrandedNodes() throws Exception
    {
        Relationship[] relationships = createRelationshipChain( 1 );
        Node otherNode = relationships[0].getEndNode();

        executeCommand( "rmrel -fd " + relationships[0].getId(), "not having any relationships" );
        assertRelationshipDoesntExist( relationships[0] );
        assertNodeDoesntExist( otherNode );
    }

    @Test
    public void rmrelCanDeleteRelationshipSoThatCurrentNodeGetsStranded() throws Exception
    {
        Relationship[] relationships = createRelationshipChain( 2 );
        executeCommand( "cd " + relationships[0].getEndNode().getId() );
        deleteRelationship( relationships[0] );
        Node currentNode = relationships[1].getStartNode();
        executeCommand( "rmrel -fd " + relationships[1].getId(), "not having any relationships" );
        assertNodeExists( currentNode );
        assertFalse( currentNode.hasRelationship() );
        executeCommand( "pwd" );
        executeCommand( "cd -a " + db.getReferenceNode().getId() );
        executeCommand( "pwd" );
    }

    @Test
    public void rmnodeCanDeleteStrandedNodes() throws Exception
    {
        Relationship[] relationships = createRelationshipChain( 1 );
        Node strandedNode = relationships[0].getEndNode();
        deleteRelationship( relationships[0] );
        executeCommand( "rmnode " + strandedNode.getId() );
        assertNodeDoesntExist( strandedNode );
    }

    @Test
    public void rmnodeCanDeleteConnectedNodes() throws Exception
    {
        Relationship[] relationships = createRelationshipChain( 2 );
        Node middleNode = relationships[0].getEndNode();
        executeCommandExpectingException( "rmnode " + middleNode.getId(), "still has relationships" );
        assertNodeExists( middleNode );
        Node endNode = relationships[1].getEndNode();
        executeCommand( "rmnode -f " + middleNode.getId(), "deleted" );
        assertNodeDoesntExist( middleNode );
        assertRelationshipDoesntExist( relationships[0] );
        assertRelationshipDoesntExist( relationships[1] );

        assertNodeExists( endNode );
        executeCommand( "cd -a " + endNode.getId() );
        executeCommand( "rmnode " + endNode.getId() );
        executeCommand( "pwd", Pattern.quote( "(?)" ) );
    }

    @Test
    public void pwdWorksOnDeletedNode() throws Exception
    {
        Relationship[] relationships = createRelationshipChain( 1 );
        executeCommand( "cd " + relationships[0].getEndNode().getId() );

        // Delete the relationship and node we're standing on
        Transaction tx = db.beginTx();
        relationships[0].getEndNode().delete();
        relationships[0].delete();
        tx.success();
        tx.finish();

        Relationship[] otherRelationships = createRelationshipChain( 1 );
        executeCommand( "pwd", "\\(0\\)-->\\(\\?\\)" );
        executeCommand( "cd -a " + otherRelationships[0].getEndNode().getId() );
        executeCommand( "ls" );
    }

    @Test
    public void startEvenIfReferenceNodeHasBeenDeleted() throws Exception
    {
        String storeDir = "target/test-data/db";
        FileUtils.deleteRecursively( new File( storeDir ) );
        GraphDatabaseService newDb = new EmbeddedGraphDatabase( storeDir );
        Transaction tx = newDb.beginTx();
        newDb.getReferenceNode().delete();
        Node node = newDb.createNode();
        String name = "Test";
        node.setProperty( "name", name );
        tx.success();
        tx.finish();

        GraphDatabaseShellServer server = new GraphDatabaseShellServer( newDb );
        ShellClient client = new SameJvmClient( server );
        executeCommand( server, client, "pwd", Pattern.quote( "(?)" ) );
        executeCommand( server, client, "ls " + node.getId(), "Test" );
        executeCommand( server, client, "cd -a " + node.getId() );
        executeCommand( server, client, "ls", "Test" );
        newDb.shutdown();
    }
    
    @Test
    public void getDbinfo() throws Exception
    {
        // It's JSON coming back from dbinfo command
        executeCommand( "dbinfo -g Kernel", "\\{", "\\}", "StoreId" );
    }
}
