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
package org.neo4j.server.webadmin.console;

import groovy.lang.Binding;

import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;

import com.tinkerpop.gremlin.Gremlin;
import com.tinkerpop.gremlin.Imports;
import com.tinkerpop.gremlin.console.NullResultHookClosure;

public class GremlinWebConsole
{
    private static final Groovysh ERROR_STATE_PLACEHOLDER_FOR_ERRANT_GROOVY_SHELLS = null;
    private final Groovysh groovy;

    public GremlinWebConsole( Binding bindings, IO io )
    {
        groovy = new Groovysh( bindings, io );

        groovy.setResultHook( new NullResultHookClosure( groovy ) );
        for ( String imps : Imports.getImports() )
        {
            groovy.execute( "import " + imps );
        }
        groovy.setResultHook( new GremlinResultHook( groovy, io ) );
        Gremlin.load();
    }

    protected GremlinWebConsole()
    {
        groovy = ERROR_STATE_PLACEHOLDER_FOR_ERRANT_GROOVY_SHELLS;
    }

    public void execute( String script )
    {
        groovy.execute( script );
    }
}
