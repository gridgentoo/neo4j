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
package org.neo4j.graphalgo.path;

import static common.Neo4jAlgoTestCase.MyRelTypes.R1;
import static common.SimpleGraphBuilder.KEY_ID;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.neo4j.graphalgo.GraphAlgoFactory.shortestPath;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static org.neo4j.helpers.collection.IteratorUtil.count;
import static org.neo4j.kernel.Traversal.expanderForTypes;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.impl.path.ShortestPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipExpander;
import org.neo4j.helpers.Predicate;
import org.neo4j.kernel.Traversal;

import common.Neo4jAlgoTestCase;

public class TestShortestPath extends Neo4jAlgoTestCase
{
    protected PathFinder<Path> instantiatePathFinder( int maxDepth )
    {
        return instantiatePathFinder( Traversal.expanderForTypes( MyRelTypes.R1,
                Direction.BOTH ), maxDepth );
    }
    
    protected PathFinder<Path> instantiatePathFinder( RelationshipExpander expander, int maxDepth )
    {
        return GraphAlgoFactory.shortestPath(
                expander, maxDepth );
    }
    
    @Test
    public void testSimplestGraph()
    {
        // Layout:
        //    __
        //   /  \
        // (s)  (t)
        //   \__/
        graph.makeEdge( "s", "t" );
        graph.makeEdge( "s", "t" );

        PathFinder<Path> finder = instantiatePathFinder( 1 );
        Iterable<Path> paths = finder.findAllPaths( graph.getNode( "s" ), graph.getNode( "t" ) );
        assertPaths( paths, "s,t", "s,t" );
        assertPaths( asList( finder.findSinglePath( graph.getNode( "s" ), graph.getNode( "t" ) ) ), "s,t" );
    }
    
    @Test
    public void testAnotherSimpleGraph()
    {
        // Layout:
        //   (m)
        //   /  \
        // (s)  (o)---(t)
        //   \  /       \
        //   (n)---(p)---(q)
        graph.makeEdge( "s", "m" );
        graph.makeEdge( "m", "o" );
        graph.makeEdge( "s", "n" );
        graph.makeEdge( "n", "p" );
        graph.makeEdge( "p", "q" );
        graph.makeEdge( "q", "t" );
        graph.makeEdge( "n", "o" );
        graph.makeEdge( "o", "t" );

        PathFinder<Path> finder = instantiatePathFinder( 6 );
        Iterable<Path> paths =
                finder.findAllPaths( graph.getNode( "s" ), graph.getNode( "t" ) );
        assertPaths( paths, "s,m,o,t", "s,n,o,t" );
    }
    
    @Test
    public void testCrossedCircle()
    {
        // Layout:
        //    (s)
        //   /   \
        // (3)   (1)
        //  | \ / |
        //  | / \ |
        // (4)   (5)
        //   \   /
        //    (t)
        graph.makeEdge( "s", "1" );
        graph.makeEdge( "s", "3" );
        graph.makeEdge( "1", "2" );
        graph.makeEdge( "1", "4" );
        graph.makeEdge( "3", "2" );
        graph.makeEdge( "3", "4" );
        graph.makeEdge( "2", "t" );
        graph.makeEdge( "4", "t" );
        
        PathFinder<Path> singleStepFinder = instantiatePathFinder( 3 );
        Iterable<Path> paths = singleStepFinder.findAllPaths( graph.getNode( "s" ),
                graph.getNode( "t" ) );
        assertPaths( paths, "s,1,2,t", "s,1,4,t", "s,3,2,t", "s,3,4,t" );

        PathFinder<Path> finder = instantiatePathFinder( 3 );
        paths = finder.findAllPaths( graph.getNode( "s" ), graph.getNode( "t" ) );
        assertPaths( paths, "s,1,2,t", "s,1,4,t", "s,3,2,t", "s,3,4,t" );
    }
    
    @Test
    public void testDirectedFinder()
    {
        // Layout:
        // 
        // (a)->(b)->(c)->(d)->(e)->(f)-------\
        //    \                                v
        //     >(g)->(h)->(i)->(j)->(k)->(l)->(m)
        //
        graph.makeEdgeChain( "a,b,c,d,e,f,m" );
        graph.makeEdgeChain( "a,g,h,i,j,k,l,m" );
        
        PathFinder<Path> finder = instantiatePathFinder(
                Traversal.expanderForTypes( MyRelTypes.R1, Direction.OUTGOING ), 4 );
        assertPaths( finder.findAllPaths( graph.getNode( "a" ), graph.getNode( "j" ) ),
                "a,g,h,i,j" );
    }
    
    @Test
    public void testExactDepthFinder()
    {
        // Layout (a to k):
        //
        //     (a)--(c)--(g)--(k)
        //    /                /
        //  (b)-----(d)------(j)
        //   |        \      /
        //  (e)--(f)--(h)--(i)
        // 
        graph.makeEdgeChain( "a,c,g,k" );
        graph.makeEdgeChain( "a,b,d,j,k" );
        graph.makeEdgeChain( "b,e,f,h,i,j" );
        graph.makeEdgeChain( "d,h" );
        
        RelationshipExpander expander = Traversal.expanderForTypes( MyRelTypes.R1, Direction.OUTGOING );
        Node a = graph.getNode( "a" );
        Node k = graph.getNode( "k" );
        assertPaths( GraphAlgoFactory.pathsWithLength( expander, 3 ).findAllPaths( a, k ), "a,c,g,k" );
        assertPaths( GraphAlgoFactory.pathsWithLength( expander, 4 ).findAllPaths( a, k ), "a,b,d,j,k" );
        assertPaths( GraphAlgoFactory.pathsWithLength( expander, 5 ).findAllPaths( a, k ) );
        assertPaths( GraphAlgoFactory.pathsWithLength( expander, 6 ).findAllPaths( a, k ), "a,b,d,h,i,j,k" );
        assertPaths( GraphAlgoFactory.pathsWithLength( expander, 7 ).findAllPaths( a, k ), "a,b,e,f,h,i,j,k" );
        assertPaths( GraphAlgoFactory.pathsWithLength( expander, 8 ).findAllPaths( a, k ) );
    }
    
    @Test
    public void makeSureShortestPathsReturnsNoLoops()
    {
        // Layout:
        //
        // (a)-->(b)==>(c)-->(e)
        //        ^    /
        //         \  v
        //         (d)
        //
        
        graph.makeEdgeChain( "a,b,c,d,b,c,e" );
        Node a = graph.getNode( "a" );
        Node e = graph.getNode( "e" );
        assertPaths( instantiatePathFinder( 6 ).findAllPaths( a, e ), "a,b,c,e", "a,b,c,e" );
    }
    
    @Test
    public void testExactDepthPathsReturnsNoLoops()
    {
        // Layout:
        //
        // (a)-->(b)==>(c)-->(e)
        //        ^    /
        //         \  v
        //         (d)
        //
        
        graph.makeEdgeChain( "a,b,c,d,b,c,e" );
        Node a = graph.getNode( "a" );
        Node e = graph.getNode( "e" );
        assertPaths( GraphAlgoFactory.pathsWithLength(
                Traversal.expanderForTypes( MyRelTypes.R1 ), 3 ).findAllPaths( a, e ), "a,b,c,e", "a,b,c,e" );
        assertPaths( GraphAlgoFactory.pathsWithLength(
                Traversal.expanderForTypes( MyRelTypes.R1 ), 4 ).findAllPaths( a, e ), "a,b,d,c,e" );
        assertPaths( GraphAlgoFactory.pathsWithLength(
                Traversal.expanderForTypes( MyRelTypes.R1 ), 6 ).findAllPaths( a, e ) );
    }

    @Test
    public void withFilters() throws Exception
    {
        graph.makeEdgeChain( "a,b,c,d" );
        graph.makeEdgeChain( "a,g,h,d" );
        Node a = graph.getNode( "a" );
        Node d = graph.getNode( "d" );
        Node b = graph.getNode( "b" );
        b.setProperty( "skip", true );
        Predicate<Node> filter = new Predicate<Node>()
        {
            @Override
            public boolean accept( Node item )
            {
                boolean skip = (Boolean) item.getProperty( "skip", false );
                return !skip;
            }
        };
        assertPaths( GraphAlgoFactory.shortestPath(
                Traversal.expanderForAllTypes().addNodeFilter( filter ), 10 ).findAllPaths( a, d ), "a,g,h,d" );
    }
    
    @Test
    public void testFinderShouldNotFindAnythingBeyondLimit()
    {
        graph.makeEdgeChain( "a,b,c,d,e" );

        PathFinder<Path> finderLimitZero = instantiatePathFinder(Traversal.emptyExpander(), 0 );
        PathFinder<Path> finderLimitOne = instantiatePathFinder(Traversal.emptyExpander(), 1 );
        PathFinder<Path> finderLimitTwo = instantiatePathFinder(Traversal.emptyExpander(), 2 );

        assertPaths( finderLimitZero.findAllPaths( graph.getNode( "a" ), graph.getNode( "b" ) ) );
        assertPaths( finderLimitOne.findAllPaths( graph.getNode( "a" ), graph.getNode( "c" ) ) );
        assertPaths( finderLimitOne.findAllPaths( graph.getNode( "a" ), graph.getNode( "d" ) ) );
        assertPaths( finderLimitTwo.findAllPaths( graph.getNode( "a" ), graph.getNode( "d" ) ) );
        assertPaths( finderLimitTwo.findAllPaths( graph.getNode( "a" ), graph.getNode( "e" ) ) );
    }
    
    @Test
    public void makeSureDescentStopsWhenPathIsFound() throws Exception
    {
        /*
         * (a)==>(b)==>(c)==>(d)==>(e)
         *   \
         *    v
         *    (f)-->(g)-->(h)-->(i)
         */
        
        graph.makeEdgeChain( "a,b,c,d,e" );
        graph.makeEdgeChain( "a,b,c,d,e" );
        graph.makeEdgeChain( "a,f,g,h,i" );
        Node a = graph.getNode( "a" );
        Node b = graph.getNode( "b" );
        Node c = graph.getNode( "c" );
        final Set<Node> allowedNodes = new HashSet<Node>( Arrays.asList( a, b, c ) );
        
        PathFinder<Path> finder = new ShortestPath( 100, Traversal.expanderForAllTypes( Direction.OUTGOING ) )
        {
            @Override
            protected Collection<Node> filterNextLevelNodes( Collection<Node> nextNodes )
            {
                for ( Node node : nextNodes )
                {
                    if ( !allowedNodes.contains( node ) )
                    {
                        fail( "Node " + node.getProperty( KEY_ID ) + " shouldn't be expanded" );
                    }
                }
                return nextNodes;
            }
        };
        finder.findAllPaths( a, c );
    }
    
    @Test
    public void makeSureRelationshipNotConnectedIssueNotThere() throws Exception
    {
        /*
         *                                  (g)
         *                                  / ^
         *                                 v   \
         * (a)<--(b)<--(c)<--(d)<--(e)<--(f)   (i)
         *                                 ^   /
         *                                  \ v
         *                                  (h)
         */
        
        graph.makeEdgeChain( "i,g,f,e,d,c,b,a" );
        graph.makeEdgeChain( "i,h,f" );
        
        PathFinder<Path> finder = instantiatePathFinder(
                Traversal.expanderForTypes( MyRelTypes.R1, Direction.INCOMING ), 10 );
        Node start = graph.getNode( "a" );
        Node end = graph.getNode( "i" );
        assertPaths( finder.findAllPaths( start, end ), "a,b,c,d,e,f,g,i", "a,b,c,d,e,f,h,i" );
    }
    
    @Test
    public void makeSureShortestPathCanBeFetchedEvenIfANodeHasLoops() throws Exception
    {
        // Layout:
        //
        // = means loop :)
        //
        //   (m)
        //   /  \
        // (s)  (o)=
        //   \  /
        //   (n)=
        //    |
        //   (p)
        graph.makeEdgeChain( "m,s,n,p" );
        graph.makeEdgeChain( "m,o,n" );
        graph.makeEdge( "o", "o" );
        graph.makeEdge( "n", "n" );

        PathFinder<Path> finder = instantiatePathFinder( 3 );
        Iterable<Path> paths =
                finder.findAllPaths( graph.getNode( "m" ), graph.getNode( "p" ) );
        assertPaths( paths, "m,s,n,p", "m,o,n,p" );
    }

    @Test
    public void makeSureAMaxResultCountCanIsObeyed()
    {
        // Layout:
        //
        //   (a)--(b)--(c)--(d)--(e)
        //    |                 / | \
        //   (f)--(g)---------(h) |  \
        //    |                   |   |
        //   (i)-----------------(j)  |
        //    |                       |
        //   (k)----------------------
        // 
        graph.makeEdgeChain( "a,b,c,d,e" );
        graph.makeEdgeChain( "a,f,g,h,e" );
        graph.makeEdgeChain(   "f,i,j,e" );
        graph.makeEdgeChain(     "i,k,e" );
        
        RelationshipExpander expander = Traversal.expanderForTypes( MyRelTypes.R1, Direction.OUTGOING );
        Node a = graph.getNode( "a" );
        Node e = graph.getNode( "e" );
        assertEquals( 4, count( shortestPath( expander, 10, 10 ).findAllPaths( a, e ) ) );
        assertEquals( 4, count( shortestPath( expander, 10, 4 ).findAllPaths( a, e ) ) );
        assertEquals( 3, count( shortestPath( expander, 10, 3 ).findAllPaths( a, e ) ) );
        assertEquals( 2, count( shortestPath( expander, 10, 2 ).findAllPaths( a, e ) ) );
        assertEquals( 1, count( shortestPath( expander, 10, 1 ).findAllPaths( a, e ) ) );
    }
    
    @Test
    public void unfortunateRelationshipOrderingInTriangle()
    {
        /*
         *            (b)
         *           ^   \
         *          /     v
         *        (a)---->(c)
         *
         * Relationships are created in such a way that they are iterated in the worst order,
         * i.e. (S) a-->b, (E) c<--b, (S) a-->c
         */
        graph.makeEdgeChain( "a,b,c" );
        graph.makeEdgeChain( "a,c" );
        Node a = graph.getNode( "a" );
        Node c = graph.getNode( "c" );
        
        assertPathDef( shortestPath( expanderForTypes( R1, OUTGOING ), 2 ).findSinglePath( a, c ), "a", "c" );
        assertPathDef( shortestPath( expanderForTypes( R1, INCOMING ), 2 ).findSinglePath( c, a ), "c", "a" );
    }
}
