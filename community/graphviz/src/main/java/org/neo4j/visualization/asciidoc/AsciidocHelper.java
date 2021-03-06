/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.visualization.asciidoc;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.visualization.graphviz.AsciiDocStyle;
import org.neo4j.visualization.graphviz.GraphvizWriter;
import org.neo4j.walk.Walker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class AsciidocHelper
{

    private static final String ILLEGAL_STRINGS = "[:\\(\\)\t;&/\\\\]";

    public static String createGraphViz( String title, GraphDatabaseService graph, String identifier )
    {
        OutputStream out = new ByteArrayOutputStream();
        GraphvizWriter writer = new GraphvizWriter(new AsciiDocStyle());
        try
        {
            writer.emit( out, Walker.fullGraph( graph ) );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }

        String safeTitle = title.replaceAll(ILLEGAL_STRINGS, "");

        return "." + title + "\n[\"dot\", \""
               + ( safeTitle + "-" + identifier ).replace( " ", "-" )
               + ".svg\", \"neoviz\"]\n" +
                "----\n" +
                out.toString() +
                "----\n";
    }

    public static String createOutputSnippet( final String output )
    {
        return "[source]\n----\n"+output+"\n----\n";
    }

    public static String createQueryResultSnippet( final String output )
    {
        return "[queryresult]\n----\n" + output + "\n----\n";
    }

    public static String createCypherSnippet( final String query )
    {
        String[] keywordsToBreakOn = new String[] {"start", "match", "where", "return", "skip", "limit", "order by",
                "asc", "ascending", "desc", "descending"};

        String result = "[source,cypher]\n----\n" + query + "\n----\n";

        for(String keyword : keywordsToBreakOn)
        {
            String upperKeyword = keyword.toUpperCase();
            result = result.
                    replace(keyword, upperKeyword).
                    replace(" " + upperKeyword + " ", "\n" + upperKeyword + " ");
        }

        //cut to max 123 chars for PDF compliance
        String[] lines = result.split( "\n" );
        String finalRes = "";
        for(String line : lines) {
            line = line.trim();
            if (line.length() > 123 ) {
                line = line.replaceAll( ", ", ",\n      " );
            }
            finalRes += line + "\n";
        }
        return finalRes;
    }
}
