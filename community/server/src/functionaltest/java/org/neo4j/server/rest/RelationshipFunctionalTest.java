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

import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response.Status;

import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.server.database.DatabaseBlockedException;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.domain.JsonParseException;
import org.neo4j.test.GraphDescription;
import org.neo4j.test.GraphDescription.Graph;
import org.neo4j.test.GraphDescription.NODE;
import org.neo4j.test.GraphDescription.PROP;
import org.neo4j.test.GraphDescription.REL;
import org.neo4j.test.TestData.Title;

public class RelationshipFunctionalTest extends AbstractRestFunctionalTestBase
{

    @Test
    @Graph( nodes = { @NODE( name = "Romeo", setNameProperty = true ),
            @NODE( name = "Juliet", setNameProperty = true ) }, relationships = { @REL( start = "Romeo", end = "Juliet", type = "LOVES", properties = { @PROP( key = "cost", value = "high", type = GraphDescription.PropType.STRING ) } ) } )
    @Title( "Remove properties from a relationship" )
    public void shouldReturn204WhenPropertiesAreRemovedFromRelationship()
            throws DatabaseBlockedException
    {
        data.get();
        Relationship loves = getNode( "Romeo" ).getRelationships().iterator().next();
        gen.get().expectedStatus( Status.NO_CONTENT.getStatusCode() ).delete(
                getRelationshipUri( loves ) ).entity();

    }

    @Test
    @Graph( "I know you" )
    public void get_Relationship_by_ID() throws JsonParseException
    {
        String response = gen.get().expectedStatus(
                com.sun.jersey.api.client.ClientResponse.Status.OK.getStatusCode() ).get(
                getRelationshipUri( data.get().get( "I" ).getSingleRelationship(
                        DynamicRelationshipType.withName( "know" ),
                        Direction.OUTGOING ) ) ).entity();
        assertTrue(JsonHelper.jsonToMap( response ).containsKey( "start" ));

    }

    @Test
    @Documented
    @Graph( nodes = { @NODE( name = "Romeo", setNameProperty = true ),
            @NODE( name = "Juliet", setNameProperty = true ) }, relationships = { @REL( start = "Romeo", end = "Juliet", type = "LOVES", properties = { @PROP( key = "cost", value = "high", type = GraphDescription.PropType.STRING ) } ) } )
    @Title( "Remove property from a relationship" )
    public void shouldReturn204WhenPropertyIsRemovedFromRelationship()
            throws DatabaseBlockedException
    {
        data.get();
        Relationship loves = getNode( "Romeo" ).getRelationships().iterator().next();
        gen.get().description(
                startGraph( "Remove property from a relationship1" ) );
        gen.get().expectedStatus( Status.NO_CONTENT.getStatusCode() ).delete(
                getPropertiesUri( loves ) + "/cost" ).entity();

    }

    @Test
    @Documented( )
    @Title( "Remove non-existent property from a relationship" )
    @Graph( nodes = { @NODE( name = "Romeo", setNameProperty = true ),
            @NODE( name = "Juliet", setNameProperty = true ) }, relationships = { @REL( start = "Romeo", end = "Juliet", type = "LOVES", properties = { @PROP( key = "cost", value = "high", type = GraphDescription.PropType.STRING ) } ) } )
    public void shouldReturn404WhenPropertyWhichDoesNotExistRemovedFromRelationship()
    {
        data.get();
        Relationship loves = getNode( "Romeo" ).getRelationships().iterator().next();
        gen.get().expectedStatus( Status.NOT_FOUND.getStatusCode() ).delete(
                getPropertiesUri( loves ) + "/non-existent" ).entity();
    }

    @Test
    @Graph( "I know you" )
    @Documented
    @Title( "Remove properties from a non-existing relationship" )
    public void shouldReturn404WhenPropertiesRemovedFromARelationshipWhichDoesNotExist()
    {
        data.get();
        gen.get().expectedStatus( Status.NOT_FOUND.getStatusCode() ).delete(
                "http://localhost:7474/db/data/relationship/1234/properties" ).entity();

    }

    @Test
    @Graph( "I know you" )
    @Documented
    @Title( "Remove property from a non-existing relationship" )
    public void shouldReturn404WhenPropertyRemovedFromARelationshipWhichDoesNotExist()
    {
        data.get();
        gen.get().expectedStatus( Status.NOT_FOUND.getStatusCode() ).delete(
                "http://localhost:7474/db/data/relationship/1234/properties/cost" ).entity();

    }

    @Test
    @Graph( nodes = { @NODE( name = "Romeo", setNameProperty = true ),
            @NODE( name = "Juliet", setNameProperty = true ) }, relationships = { @REL( start = "Romeo", end = "Juliet", type = "LOVES", properties = { @PROP( key = "cost", value = "high", type = GraphDescription.PropType.STRING ) } ) } )
    @Title( "Delete relationship" )
    public void removeRelationship()
    {
        data.get();
        Relationship loves = getNode( "Romeo" ).getRelationships().iterator().next();
        gen.get().description( startGraph( "Delete relationship1" ) );
        gen.get().expectedStatus( Status.NO_CONTENT.getStatusCode() ).delete(
                getRelationshipUri( loves ) ).entity();

    }
}
