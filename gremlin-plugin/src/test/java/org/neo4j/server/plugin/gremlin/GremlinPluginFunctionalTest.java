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
package org.neo4j.server.plugin.gremlin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.helpers.Pair;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.server.rest.AbstractRestFunctionalTestBase;
import org.neo4j.server.rest.JSONPrettifier;
import org.neo4j.test.GraphDescription.Graph;
import org.neo4j.test.TestData.Title;

public class GremlinPluginFunctionalTest extends AbstractRestFunctionalTestBase
{
    private static final String ENDPOINT = "http://localhost:7474/db/data/ext/GremlinPlugin/graphdb/execute_script";

    /**
     * Scripts can be sent as URL-encoded In this example, the graph has been
     * autoindexed by Neo4j, so we can look up the name property on nodes.
     */
    @Test
    @Title( "Send a Gremlin Script - URL encoded" )
    @Documented
    @Graph( value = { "I know you" }, autoIndexNodes = true )
    public void testGremlinPostURLEncoded() throws UnsupportedEncodingException
    {
        data.get();
        String script = "g.idx('node_auto_index')[[name:'I']]._().out()";
        gen().expectedStatus( Status.OK.getStatusCode() ).description(
                formatGroovy( script ) );
        String response = gen().payload(
                "script=" + URLEncoder.encode( script, "UTF-8" ) ).payloadType(
                MediaType.APPLICATION_FORM_URLENCODED_TYPE ).post( ENDPOINT ).entity();
        assertTrue( response.contains( "you" ) );
    }

    /**
     * Send a Gremlin Script, URL-encoded with UTF-8 encoding, with additional
     * parameters.
     */
    @Title( "Send a Gremlin Script with variables in a JSON Map - URL encoded" )
    @Documented
    @Graph( value = { "I know you" } )
    public void testGremlinPostWithVariablesURLEncoded()
            throws UnsupportedEncodingException
    {
        final String script = "g.v(me).out;";
        final String params = "{ \"me\" : " + data.get().get( "I" ).getId()
                              + " }";
        gen().description( formatGroovy( script ) );
        String response = gen().expectedStatus( Status.OK.getStatusCode() ).payload(
                "script=" + URLEncoder.encode( script, "UTF-8" ) + "&params="
                        + URLEncoder.encode( params, "UTF-8" ) )

        .payloadType( MediaType.APPLICATION_FORM_URLENCODED_TYPE ).post(
                ENDPOINT ).entity();
        assertTrue( response.contains( "you" ) );
    }

    /**
     * Send a Gremlin Script, as JSON payload and additional parameters
     */
    @Test
    @Title( "Send a Gremlin Script with variables in a JSON Map" )
    @Documented
    @Graph( value = { "I know you" } )
    public void testGremlinPostWithVariablesAsJson()
            throws UnsupportedEncodingException
    {
        String response = doRestCall( "g.v(me).out", Status.OK, Pair.of("me", data.get().get( "I" ).getId()+"") );
        assertTrue( response.contains( "you" ) );
    }
    
    private String doRestCall( String script, Status status,
            Pair<String, String> ...params )
    {
        // TODO Auto-generated method stub
        return super.doGremlinRestCall( ENDPOINT, script, status, params );
    }

    /**
     * Import a graph form a http://graphml.graphdrawing.org/[GraphML] file can
     * be achieved through the Gremlin GraphMLReader. The following script
     * imports a small GraphML file from an URL into Neo4j, resulting in the
     * depicted graph. It then returns a list of all nodes in the graph.
     */
    @Test
    @Documented
    @Title( "Load a sample graph" )
    public void testGremlinImportGraph() throws UnsupportedEncodingException
    {
        String script = "g.loadGraphML('https://raw.github.com/neo4j/gremlin-plugin/master/src/data/graphml1.xml');"
                        + "g.V;";
        String response = doRestCall( script, Status.OK);
        assertTrue( response.contains( "you" ) );
        assertTrue( response.contains( "him" ) );
    }

    /**
     * Exporting a graph can be done by simple emitting the appropriate String.
     */
    @Test
    @Documented
    @Title( "Emit a sample graph" )
    @Graph( value = { "I know you", "I know him" } )
    public void emitGraph() throws UnsupportedEncodingException
    {
        String script = "writer = new GraphMLWriter(g);"
                        + "out = new java.io.ByteArrayOutputStream();"
                        + "writer.outputGraph(out);"
                        + "result = out.toString();";
        String response = doRestCall( script, Status.OK );
        assertTrue( response.contains( "graphml" ) );
        assertTrue( response.contains( "you" ) );
    }

    /**
     * To set variables in the bindings for the Gremlin Script Engine on the
     * server, you can include a +params+ parameter with a String representing a
     * JSON map of variables to set to initial values. These can then be
     * accessed as normal variables within the script.
     */
    @Test
    @Documented
    @Title( "Set script variables" )
    public void setVariables() throws UnsupportedEncodingException
    {
        String script = "meaning_of_life";
        String payload = "{\"script\":\"" + script + "\","
                         + "\"params\":{\"meaning_of_life\" : 42.0}}";
        description( formatGroovy( script ) );
        String response = gen().expectedStatus( Status.OK.getStatusCode() ).payload(
                JSONPrettifier.parse( payload ) ).payloadType(
                MediaType.APPLICATION_JSON_TYPE ).post( ENDPOINT ).entity();
        assertTrue( response.contains( "42.0" ) );
    }

    /**
     * The following script returns a sorted list of all nodes connected via
     * outgoing relationships to node 1, sorted by their `name`-property.
     */
    @Test
    @Documented
    @Title( "Sort a result using raw Groovy operations" )
    @Graph( value = { "I know you", "I know him" }, autoIndexNodes = true )
    public void testSortResults() throws UnsupportedEncodingException
    {
        data.get();
        String script = "g.idx('node_auto_index').get('name','I').toList()._().out().sort{it.name}.toList()";
        String response = doRestCall( script, Status.OK );
        assertTrue( response.contains( "you" ) );
        assertTrue( response.contains( "him" ) );
        assertTrue( response.indexOf( "you" ) > response.indexOf( "him" ) );
    }

    /**
     * The following script returns a sorted list of all nodes connected via
     * outgoing relationships to node 1, sorted by their `name`-property.
     */
    @Test
    @Title( "Return paths from a Gremlin script" )
    @Documented
    @Graph( value = { "I know you", "I know him" } )
    public void testScriptWithPaths()
    {
        String script = "g.v(%I%).out.name.paths";
        String response = doRestCall( script, Status.OK );
        System.out.println( response );
        assertTrue( response.contains( ", you]" ) );
    }

    @Test
    public void testLineBreaks() throws UnsupportedEncodingException
    {
        // be aware that the string is parsed in Java before hitting the wire,
        // so escape the backslash once in order to get \n on the wire.
        String script = "1\\n2";
        String response = doRestCall( script, Status.OK );
        assertTrue( response.contains( "2" ) );
    }

    /**
     * To send a Script JSON encoded, set the payload Content-Type Header. In
     * this example, find all the things that my friends like, and return a
     * table listing my friends by their name, and the names of the things they
     * like in a table with two columns, ignoring the third named step variable
     * +I+. Remember that everything in Gremlin is an iterator - in order to
     * populate the result table +t+, iterate through the pipes with +>> -1+.
     */
    @Test
    @Title( "Send a Gremlin Script - JSON encoded with table results" )
    @Documented
    @Graph( value = { "I know Joe", "I like cats", "Joe like cats",
            "Joe like dogs" } )
    public void testGremlinPostJSONWithTableResult()
    {
        String script = "i = g.v(%I%);"
                        + "t= new Table();"
                        + "i.as('I').out('know').as('friend').out('like').as('likes').table(t,['friend','likes']){it.name}{it.name} >> -1;t;";
        String response = doRestCall( script, Status.OK );
        assertTrue( response.contains( "cats" ) );
    }

    /**
     * Send an arbitrary Groovy script - Lucene sorting.
     * 
     * This example demonstrates that you via the Groovy runtime embedded with
     * the server have full access to all of the servers Java APIs. The below
     * example creates Nodes in the database both via the Blueprints and the
     * Neo4j API indexes the nodes via the native Neo4j Indexing API constructs
     * a custom Lucene sorting and searching returns a Neo4j IndexHits result
     * iterator.
     */
    @Test
    @Documented
    @Graph( value = { "I know Joe", "I like cats", "Joe like cats",
            "Joe like dogs" } )
    public void sendArbtiraryGroovy()
    {
        String script = ""
                        + "import org.neo4j.graphdb.index.*;"
                        + "import org.neo4j.index.lucene.*;"
                        + "import org.apache.lucene.search.*;"
                        + "neo4j = g.getRawGraph();"
                        + "tx = neo4j.beginTx();"
                        + "meVertex = g.addVertex([name:'me']);"
                        + "meNode = meVertex.getRawVertex();"
                        + "youNode = neo4j.createNode();"
                        + "youNode.setProperty('name','you');"
                        + "idxManager = neo4j.index();"
                        + "personIndex = idxManager.forNodes('persons');"
                        + "personIndex.add(meNode,'name',meVertex.name);"
                        + "personIndex.add(youNode,'name',youNode.getProperty('name'));"
                        + "tx.success();"
                        + "tx.finish();"
                        + "query = new QueryContext( 'name:*' ).sort( new Sort(new SortField( 'name',SortField.STRING, true ) ) );"
                        + "results = personIndex.query( query );";
        String response = doRestCall( script, Status.OK );
        assertTrue( response.contains( "me" ) );

    }

    /**
     * Imagine a user being part of different groups. A group can have different
     * roles, and a user can be part of different groups. He also can have
     * different roles in different groups apart from the membership. The
     * association of a User, a Group and a Role can be referred to as a
     * _HyperEdge_. However, it can be easily modeled in a property graph as a
     * node that captures this n-ary relationship, as depicted below in the
     * +U1G2R1+ node.
     * 
     * To find out in what roles a user is for a particular groups (here
     * 'Group2'), the following script can traverse this HyperEdge node and
     * provide answers.
     */
    @Test
    @Title( "HyperEdges - find user roles in groups" )
    @Documented
    @Graph( value = { "User1 in Group1", "User1 in Group2",
            "Group2 canHave Role2", "Group2 canHave Role1",
            "Group1 canHave Role1", "Group1 canHave Role2", "Group1 isA Group",
            "Group2 isA Group", "Role1 isA Role", "Role2 isA Role",
            "User1 hasRoleInGroup U1G2R1", "U1G2R1 hasRole Role1",
            "U1G2R1 hasGroup Group2", "User1 hasRoleInGroup U1G1R2",
            "U1G1R2 hasRole Role2", "U1G1R2 hasGroup Group1" } )
    public void findGroups()
    {
        String script = "" + "g.v(%User1%)"
                        + ".out('hasRoleInGroup').as('hyperedge')."
                        + "out('hasGroup').filter{it.name=='Group2'}."
                        + "back('hyperedge').out('hasRole').name";
        String response = doRestCall( script, Status.OK );
        assertTrue( response.contains( "Role1" ) );
        assertFalse( response.contains( "Role2" ) );

    }

    /**
     * This example is showing a group count in Germlin, for instance the
     * counting of the different relationship types connected to some the start
     * node. The result is collected into a variable that then is returned.
     */
    @Test
    @Documented
    @Graph( { "Peter knows Ian", "Ian knows Peter", "Peter likes Bikes" } )
    public void group_count() throws UnsupportedEncodingException, Exception
    {
        String script = "m = [:];" + "g.v(%Peter%).bothE().label.groupCount(m) >> -1;m";
        String response = doRestCall( script, Status.OK );
        assertTrue( response.contains( "knows=2" ) );
    }
    
//    g.v(0).bothE().sideEffect{g.removeEdge(it);
    /**
     * This example is showing a group count in Germlin, for instance the
     * counting of the different relationship types connected to some the start
     * node. The result is collected into a variable that then is returned.
     */
    @Test
    @Documented
    @Graph( { "Peter knows Ian", "Ian knows Peter", "Peter likes Bikes" } )
    public void modify_the_graph_while_traversing() throws UnsupportedEncodingException, Exception
    {
        assertTrue( getNode( "Peter" ).hasRelationship() );
        String script = "g.v(%Peter%).bothE()each{g.removeEdge(it);};";
        String response = doRestCall( script, Status.OK );
        assertFalse( getNode( "Peter" ).hasRelationship() );
    }

    
    /**
     * Multiple traversals can be combined into a single result, using splitting
     * and merging pipes in a lazy fashion.
     */
    @Test
    @Documented
    @Graph( value = { "Peter knows Ian", "Ian knows Peter", "Marie likes Peter" }, autoIndexNodes = true )
    public void collect_multiple_traversal_results()
            throws UnsupportedEncodingException, Exception
    {
        String script = "g.idx('node_auto_index')[['name':'Peter']].copySplit(_().out('knows'), _().in('likes')).fairMerge.name";
        String response = doRestCall( script, Status.OK );
        assertTrue( response.contains( "Marie" ) );
        assertTrue( response.contains( "Ian" ) );
    }

    @Test
    public void getExtension()
    {
        String entity = gen.get().expectedStatus( Status.OK.getStatusCode() ).get(
                ENDPOINT ).entity();
        assertTrue( entity.contains( "map" ) );

    }

    

    /**
     * In order to return only certain sections of a Gremlin result,
     * you can use +drop()+ and +take()+ to skip and chunk the
     * result set. Also, note the use of the +filter{}+ closure to filter
     * nodes.
     */
    @Test
    @Graph( value = { "George knows Sara", "George knows Ian" }, autoIndexNodes = true )
    public void chunking_and_offsetting_in_Gremlin() throws UnsupportedEncodingException
    {
        String script = " g.v(%George%).outE[[label:'knows']].inV.filter{ it.name == 'Sara'}.drop(0).take(100)._()";
        String response = doRestCall( script, Status.OK );
        assertTrue( response.contains( "Sara" ) );
        assertFalse( response.contains( "Ian" ) );
    }
    
    /**
     * This example demonstrates basic
     * collaborative filtering - ordering a traversal after occurence counts and
     * substracting objects that are not interesting in the final result.
     * 
     * Here, we are finding Friends-of-Friends that are not Joes friends already.
     * The same can be applied to graphs of users that +LIKE+ things and others.
     */
    @Documented
    @Test
    @Graph( value = { "Joe knows Bill", "Joe knows Sara", "Sara knows Bill", "Sara knows Ian", "Bill knows Derrick",
            "Bill knows Ian", "Sara knows Jill" }, autoIndexNodes = true )
    public void collaborative_filtering() throws UnsupportedEncodingException
    {
        String script = "x=[];fof=[:];" +
        		"g.v(%Joe%).out('knows').aggregate(x).out('knows').except(x).groupCount(fof)>>-1;fof.sort{a,b -> b.value <=> a.value}";
        String response = doRestCall( script, Status.OK );
        assertFalse( response.contains( "v["+ data.get().get( "Bill").getId() ) );
        assertFalse( response.contains( "v["+ data.get().get( "Sara").getId() ) );
        assertTrue( response.contains( "v["+ data.get().get( "Ian").getId() ) );
        assertTrue( response.contains( "v["+ data.get().get( "Jill").getId() ) );
        assertTrue( response.contains( "v["+ data.get().get( "Derrick").getId() ) );
    }

    @Test
    @Ignore
    @Graph( value = { "Peter knows Ian", "Ian knows Peter", "Marie likes Peter" }, autoIndexNodes = true, autoIndexRelationships=true )
    public void test_Gremlin_load()
    {
        data.get();
        String script = "nodeIndex = g.idx('node_auto_index');"
                        + "edgeIndex = g.idx('relationship_auto_index');"
                        + ""
                        + "node = { uri, properties -> "
                        + "existing = nodeIndex.get('uri', uri);"
                        + "properties['uri'] = uri;"
                        + "if (existing) {    "
                        + "return existing[0];  "
                        + "}  else {"
                        + "    return g.addVertex(properties);"
                        + "};"
                        + "};"
                        + "Object.metaClass.makeNode = node;"
                        + "edge = { type, source_uri, target_uri, properties ->"
                        + "  source = nodeIndex.get('uri', source_uri) >> 1;"
                        + "  target = nodeIndex.get('uri', target_uri) >> 1;"
                        + "  nodeKey = source.id + '-' + target.id;"
                        + "  existing = edgeIndex.get('nodes', nodeKey);"
                        + "  if (existing) {" + "    return existing;" + "  };"
                        + "  properties['nodes'] = nodeKey;"
                        + "  g.addEdge(source, target, type, properties);"
                        + "};" + "Object.metaClass.makeEdge = edge;";
        String payload = "{\"script\":\"" + script + "\"}";
        description( formatGroovy( script ) );
        gen.get().expectedStatus( Status.OK.getStatusCode() ).payload(
                JSONPrettifier.parse( payload ) );
        String response = gen.get().post( ENDPOINT ).entity();
        for (int i = 0; i<1000;i++) {
            String uri = "uri"+i;
            payload = "{\"script\":\"n = Object.metaClass.makeNode('"+uri+"',[:]\"}";
            gen.get().expectedStatus( Status.OK.getStatusCode() ).payload(
                    JSONPrettifier.parse( payload ) );
            response = gen.get().post( ENDPOINT ).entity();
            assertTrue( response.contains( uri ) );
        }
        for (int i = 0; i<999;i++) {
            String uri = "uri";
            payload = "{\"script\":\"n = Object.metaClass.makeEdge('knows','"+uri+i+"','"+uri+(i+1)+"'[:]\"}";
            gen.get().expectedStatus( Status.OK.getStatusCode() ).payload(
                    JSONPrettifier.parse( payload ) );
            response = gen.get().post( ENDPOINT ).entity();
            assertTrue( response.contains( uri ) );
        }
    }
}
