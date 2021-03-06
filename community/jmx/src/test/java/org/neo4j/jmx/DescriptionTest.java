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
package org.neo4j.jmx;

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Hashtable;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.ObjectName;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public class DescriptionTest
{
    private static AbstractGraphDatabase graphdb;

    @BeforeClass
    public static void startDb()
    {
        graphdb = new EmbeddedGraphDatabase( "target" + File.separator + "var" + File.separator
                                             + DescriptionTest.class.getSimpleName() );
    }

    @AfterClass
    public static void stopDb()
    {
        if ( graphdb != null ) graphdb.shutdown();
        graphdb = null;
    }

    @Test
    public void canGetBeanDescriptionFromMBeanInterface() throws Exception
    {
        assertEquals( Kernel.class.getAnnotation( Description.class ).value(), kernelMBeanInfo().getDescription() );
    }

    @Test
    public void canGetMethodDescriptionFromMBeanInterface() throws Exception
    {
        for ( MBeanAttributeInfo attr : kernelMBeanInfo().getAttributes() )
        {
            try
            {
                assertEquals(
                        Kernel.class.getMethod( "get" + attr.getName() ).getAnnotation( Description.class ).value(),
                        attr.getDescription() );
            }
            catch ( NoSuchMethodException ignored )
            {
                assertEquals(
                        Kernel.class.getMethod( "is" + attr.getName() ).getAnnotation( Description.class ).value(),
                        attr.getDescription() );
            }
        }
    }

    private MBeanInfo kernelMBeanInfo() throws Exception
    {
        Kernel kernel = graphdb.getManagementBean( Kernel.class );
        ObjectName query = kernel.getMBeanQuery();
        Hashtable<String, String> properties = new Hashtable<String, String>( query.getKeyPropertyList() );
        properties.put( "name", Kernel.NAME );
        MBeanInfo beanInfo = getPlatformMBeanServer().getMBeanInfo( new ObjectName( query.getDomain(), properties ) );
        return beanInfo;
    }
}
