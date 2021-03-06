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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Parses a line from the client with the intention of interpreting it as
 * an "app" command, f.ex. like:
 * 
 * "ls -pf --long-option title.* 12"
 * 
 * o ls is the app.
 * o p and f are options, p w/o value and f has the value "title.*"
 *   (defined in {@link App#getOptionDefinition(String)}.
 * o long-option is also an option
 * o 12 is an argument.
 */
public class AppCommandParser
{
	private AppShellServer server;
	private String line;
	private String appName;
	private App app;
	private Map<String, String> options = new HashMap<String, String>();
	private List<String> arguments = new ArrayList<String>();
	
	/**
	 * @param server the server used to find apps.
	 * @param line the line from the client to interpret.
	 * @throws Exception if there's something wrong with the line.
	 */
	public AppCommandParser( AppShellServer server, String line )
		throws Exception
	{
		this.server = server;
		if ( line != null )
		{
			line = line.trim();
		}
		this.line = line;
		this.parse();
	}
	
	private void parse() throws Exception
	{
		if ( this.line == null || this.line.trim().length() == 0 )
		{
			return;
		}
		
		this.parseApp();
		this.parseParameters();
	}
	
	/**
	 * Extracts the app name (f.ex. ls or rm) from the supplied line.
	 * @param line the line to extract the app name from.
	 * @return the app name for {@code line}.
	 */
	public static String parseOutAppName( String line )
	{
        int index = findNextWhiteSpace( line, 0 );
        return index == -1 ? line : line.substring( 0, index );
	}
	
	private void parseApp() throws Exception
	{
		int index = findNextWhiteSpace( this.line, 0 );
		this.appName = index == -1 ?
			this.line : this.line.substring( 0, index );
		this.app = this.server.findApp( this.appName );
		if ( this.app == null )
		{
			throw new ShellException(
				"Unknown command '" + this.appName + "'" );
		}
	}
	
	private void parseParameters() throws ShellException
	{
		String rest = this.line.substring( this.appName.length() ).trim();
		String[] parsed = tokenizeStringWithQuotes( rest, false );
		for ( int i = 0; i < parsed.length; i++ )
		{
			String string = parsed[ i ];
			if ( isMultiCharOption( string ) )
			{
				String name = string.substring( 2 );
				i = fetchArguments( parsed, i, name );
			}
			else if ( this.isSingleCharOption( string ) )
			{
				String options = string.substring( 1 );
				for ( int o = 0; o < options.length(); o++ )
				{
					String name = String.valueOf( options.charAt( o ) );
					i = fetchArguments( parsed, i, name );
				}
			}
			else if ( string.length() > 0 )
			{
				this.arguments.add( string );
			}
		}
	}
	
	private boolean isOption( String string )
	{
	    return isSingleCharOption( string ) || isMultiCharOption( string );
	}
	
	private boolean isMultiCharOption( String string )
	{
	    return string.startsWith( "--" );
	}
	
	private boolean isSingleCharOption( String string )
	{
		return string.startsWith( "-" ) && !isANegativeNumber( string );
	}
	
	private boolean isANegativeNumber( String string )
	{
	    try
	    {
	        Integer.parseInt( string );
	        return true;
	    }
	    catch ( NumberFormatException e )
	    {
	        return false;
	    }
	}
	
	private int fetchArguments( String[] parsed, int whereAreWe,
		String optionName ) throws ShellException
	{
		String value = null;
		OptionDefinition definition =
		        this.app.getOptionDefinition( optionName );
		OptionValueType type = definition == null ?
		        OptionValueType.NONE : definition.getType();
		if ( type == OptionValueType.MUST )
		{
			whereAreWe++;
			String message = "Value required for '" + optionName + "'";
			this.assertHasIndex( parsed, whereAreWe, message );
			value = parsed[ whereAreWe ];
			if ( this.isOption( value ) )
			{
				throw new ShellException( message );
			}
		}
		else if ( type == OptionValueType.MAY )
		{
			if ( this.hasIndex( parsed, whereAreWe + 1 ) &&
				!this.isOption( parsed[ whereAreWe + 1 ] ) )
			{
				whereAreWe++;
				value = parsed[ whereAreWe ];
			}
		}
		this.options.put( optionName, value );
		return whereAreWe;
	}
	
	private boolean hasIndex( String[] array, int index )
	{
		return index >= 0 && index < array.length;
	}
	
	private void assertHasIndex( String[] array, int index, String message )
		throws ShellException
	{
		if ( !this.hasIndex( array, index ) )
		{
			throw new ShellException( message );
		}
	}
	
	private static int findNextWhiteSpace( String line, int fromIndex )
	{
		int index = line.indexOf( ' ', fromIndex );
		return index == -1 ? line.indexOf( '\t', fromIndex ) : index;
	}
	
	/**
	 * @return the name of the app (from {@link #getLine()}).
	 */
	public String getAppName()
	{
		return this.appName;
	}
	
	/**
	 * @return the app corresponding to the {@link #getAppName()}.
	 */
	public App app()
	{
		return this.app;
	}

	/**
	 * @return the supplied options (from {@link #getLine()}).
	 */
	public Map<String, String> options()
	{
		return this.options;
	}
	
	public String option( String name, String defaultValue )
	{
	    String result = options.get( name );
	    return result != null ? result : defaultValue;
	}
	
	public Number optionAsNumber( String name, Number defaultValue )
	{
	    String value = option( name, null );
	    if ( value != null )
	    {
	        if ( value.indexOf( ',' ) != -1 || value.indexOf( '.' ) != -1 )
	        {
	            return Double.valueOf( value );
	        }
	        else
	        {
	            return Integer.valueOf( value );
	        }
	    }
	    return defaultValue;
	}
	
	/**
	 * @return the arguments (from {@link #getLine()}).
	 */
	public List<String> arguments()
	{
		return this.arguments;
	}
	
	public String argumentWithDefault( int index, String defaultValue )
	{
	    return index < arguments.size() ? arguments.get( index ) : defaultValue;
	}
	
	public String argument( int index, String errorMessageIfItDoesnExist )
	        throws ShellException
	{
	    if ( index >= arguments.size() )
	    {
	        throw new ShellException( errorMessageIfItDoesnExist );
	    }
	    return arguments.get( index );
	}
	
	/**
	 * @return the entire line from the client.
	 */
	public String getLine()
	{
		return this.line;
	}
	
	/**
	 * @return the line w/o the app (just the options and arguments).
	 */
	public String getLineWithoutApp()
	{
		return this.line.substring( this.appName.length() ).trim();
	}

	/**
	 * Tokenizes a string, regarding quotes.
	 * @param string the string to tokenize.
	 * @return the tokens from the line.
	 */
	public static String[] tokenizeStringWithQuotes( String string )
	{
		return tokenizeStringWithQuotes( string, true );
	}

	/**
	 * Tokenizes a string, regarding quotes.
	 * @param string the string to tokenize.
	 * @param trim wether or not to trim each token or not.
	 * @return the tokens from the line.
	 */
	public static String[] tokenizeStringWithQuotes( String string,
		boolean trim )
	{
		if ( trim )
		{
			string = string.trim();
		}
		ArrayList<String> result = new ArrayList<String>();
		string = string.trim();
		boolean inside = string.startsWith( "\"" );
		StringTokenizer quoteTokenizer = new StringTokenizer( string, "\"" );
		while ( quoteTokenizer.hasMoreTokens() )
		{
			String token = quoteTokenizer.nextToken();
			if ( trim )
			{
				token = token.trim();
			}
			if ( token.length() == 0 )
			{
				// Skip it
			}
			else if ( inside )
			{
				// Don't split
				result.add( token );
			}
			else
			{
			    for ( String part : TextUtil.splitAndKeepEscapedSpaces( token, false ) )
			    {
			        result.add( part );
			    }
			}
			inside = !inside;
		}
		return result.toArray( new String[ result.size() ] );
	}
}
