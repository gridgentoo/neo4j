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
package org.neo4j.shell.kernel.apps;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.Traverser.Order;
import org.neo4j.helpers.Service;
import org.neo4j.shell.App;
import org.neo4j.shell.AppCommandParser;
import org.neo4j.shell.OptionDefinition;
import org.neo4j.shell.OptionValueType;
import org.neo4j.shell.Output;
import org.neo4j.shell.Session;
import org.neo4j.shell.ShellException;

/**
 * Mimics the POSIX application "rmdir", but neo4j has relationships instead of
 * directories (if you look at Neo4j in a certain perspective).
 */
@Service.Implementation( App.class )
public class Rmrel extends GraphDatabaseApp
{
    /**
     * Constructs a new application which can delete relationships in Neo4j.
     */
    public Rmrel()
    {
        this.addOptionDefinition( "f", new OptionDefinition( OptionValueType.NONE,
            "Force deletion, i.e. disables the connectedness check" ) );
        this.addOptionDefinition( "d", new OptionDefinition( OptionValueType.NONE,
            "Also delete the node on the other side of the relationship if removing" +
            " this relationship results in it not having any relationships left" ) );
    }

    @Override
    public String getDescription()
    {
        return "Deletes a relationship, also ensuring the connectedness of the graph. That check can be ignored with -f\n" +
        		"Usage: rmrel <relationship id>\n" +
        		"   or  rmrel -f <relationship id>";
    }

    @Override
    protected String exec( AppCommandParser parser, Session session,
        Output out ) throws ShellException, RemoteException
    {
        assertCurrentIsNode( session );

        if ( parser.arguments().isEmpty() )
        {
            throw new ShellException(
                "Must supply relationship id to delete as the first argument" );
        }

        Node currentNode = this.getCurrent( session ).asNode();
        Relationship rel = findRel( currentNode, Long.parseLong(
            parser.arguments().get( 0 ) ) );
        rel.delete();
        
        Node otherNode = rel.getOtherNode( currentNode );
        boolean forceDeletion = parser.options().containsKey( "f" );
        if ( !forceDeletion )
        {
            ensureConnectedness( otherNode );
            ensureConnectedness( currentNode );
        }
        boolean deleteOtherNodeIfEmpty = parser.options().containsKey( "d" );
        if ( deleteOtherNodeIfEmpty && !otherNode.hasRelationship() )
        {
            out.println( "Also deleted " + getDisplayName( getServer(), session, otherNode, false ) +
                    " due to it not having any relationships left" );
            otherNode.delete();
        }
        return null;
    }

    private void ensureConnectedness( Node otherNode ) throws ShellException
    {
        if ( !hasPathToRefNode( otherNode ) )
        {
            throw new ShellException( "It would result in " + otherNode +
                " to be recursively decoupled with the reference node. Use -f to disable this check" );
        }
    }

    private Relationship findRel( Node currentNode, long relId )
        throws ShellException
    {
        Relationship rel = getServer().getDb().getRelationshipById( relId );
        if ( rel.getStartNode().equals( currentNode ) || rel.getEndNode().equals( currentNode ) )
        {
            return rel;
        }
        throw new ShellException( "No relationship " + relId +
            " connected to " + currentNode );
    }

    private Iterable<RelationshipType> getAllRelationshipTypes()
    {
        return this.getServer().getDb().getRelationshipTypes();
    }

    private boolean hasPathToRefNode( Node node )
    {
        List<Object> filterList = new ArrayList<Object>();
        for ( RelationshipType rel : this.getAllRelationshipTypes() )
        {
            filterList.add( rel );
            filterList.add( Direction.BOTH );
        }

        Node refNode = getServer().getDb().getReferenceNode();
        Traverser traverser = node.traverse( Order.DEPTH_FIRST,
            StopEvaluator.END_OF_GRAPH, ReturnableEvaluator.ALL,
            filterList.toArray() );
        for ( Node testNode : traverser )
        {
            if ( refNode.equals( testNode ) )
            {
                return true;
            }
        }
        return false;
    }
}
