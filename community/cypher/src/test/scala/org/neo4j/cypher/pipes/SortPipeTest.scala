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
package org.neo4j.cypher.pipes

import org.junit.Test
import org.junit.Assert._
import org.neo4j.cypher.SymbolTable
import org.scalatest.junit.JUnitSuite
import org.neo4j.cypher.commands.{EntityValue, ValueReturnItem, SortItem}

class SortPipeTest extends JUnitSuite{
  @Test def emptyInIsEmptyOut() {
    val source = new FakePipe(List())
    val sortPipe = new SortPipe(source, List(SortItem(ValueReturnItem(EntityValue("x")), true)))

    assertEquals(List(), sortPipe.toList)
  }

  @Test def simpleSortingIsSupported() {
    val source = new FakePipe(List(Map("x" -> "B"), Map("x" -> "A")))
    val sortPipe = new SortPipe(source, List(SortItem(ValueReturnItem(EntityValue("x")), true)))

    assertEquals(List(Map("x" -> "A"), Map("x" -> "B")), sortPipe.toList)
  }

  @Test def sortByTwoColumns() {
    val source = new FakePipe(List(
      Map("x" -> "B", "y" -> 20),
      Map("x" -> "A", "y" -> 100),
      Map("x" -> "B", "y" -> 10)))

    val sortPipe = new SortPipe(source, List(
      SortItem(ValueReturnItem(EntityValue("x")), true),
      SortItem(ValueReturnItem(EntityValue("y")), true)))

    assertEquals(List(
      Map("x" -> "A", "y" -> 100),
      Map("x" -> "B", "y" -> 10),
      Map("x" -> "B", "y" -> 20)), sortPipe.toList)
  }

  @Test def sortByTwoColumnsWithOneDescending() {
    val source = new FakePipe(List(
      Map("x" -> "B", "y" -> 20),
      Map("x" -> "A", "y" -> 100),
      Map("x" -> "B", "y" -> 10)))

    val sortPipe = new SortPipe(source, List(
      SortItem(ValueReturnItem(EntityValue("x")), true),
      SortItem(ValueReturnItem(EntityValue("y")), false)))

    assertEquals(List(
      Map("x" -> "A", "y" -> 100),
      Map("x" -> "B", "y" -> 20),
      Map("x" -> "B", "y" -> 10)), sortPipe.toList)
  }

  @Test def shouldHandleSortingWithNullValues() {
    val source = new FakePipe(List(
      Map("y" -> 1),
      Map("y" -> null),
      Map("y" -> 2)))

    val sortPipe = new SortPipe(source, List(SortItem(ValueReturnItem(EntityValue("y")), true)))

    assertEquals(List(
      Map("y" -> 1),
      Map("y" -> 2),
      Map("y" -> null)), sortPipe.toList)
  }

}

class FakePipe(data: Seq[Map[String, Any]], val symbols: SymbolTable) extends Pipe {
  def this(data: Seq[Map[String, Any]]) = this (data, new SymbolTable())

  def foreach[U](f: (Map[String, Any]) => U) {
    data.foreach(f(_))
  }
}