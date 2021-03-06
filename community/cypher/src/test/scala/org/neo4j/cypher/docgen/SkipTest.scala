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
package org.neo4j.cypher.docgen

/**
 * Copyright (c) 2002-2011 "Neo Technology,"
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
import org.neo4j.graphdb.Node
import org.junit.Assert.assertEquals
import org.junit.Test

class SkipTest extends DocumentingTestBase {
  def graphDescription = List("A KNOWS B", "A KNOWS C", "A KNOWS D", "A KNOWS E")

  def section: String = "Skip"

  @Test def returnFromThree() {
    testQuery(
      title = "Skip first three",
      text = "To return a subset of the result, starting from third result, use this syntax:",
      queryText = "start n=node(%A%, %B%, %C%, %D%, %E%) return n order by n.name skip 3",
      returns = "The first three nodes are skipped, and only the last two are returned.",
      (p) => assertEquals(List(node("D"), node("E")), p.columnAs[Node]("n").toList))
  }

  @Test def returnFromOneLimitTwo() {
    testQuery(
      title = "Return middle two",
      text = "To return a subset of the result, starting from somewhere in the middle, use this syntax:",
      queryText = "start n=node(%A%, %B%, %C%, %D%, %E%) return n order by n.name skip 1 limit 2",
      returns = "Two nodes from the middle are returned",
      (p) => assertEquals(List(node("B"), node("C")), p.columnAs[Node]("n").toList))
  }
}

