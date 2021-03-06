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

import org.junit.Test
import org.junit.Assert._
import org.neo4j.graphdb.{Relationship, Node}

class WhereTest extends DocumentingTestBase {
  def graphDescription = List("Andres KNOWS Tobias", "Andres KNOWS Peter")

  override val properties = Map(
    "Andres" -> Map("age" -> 36l, "belt" -> "white"),
    "Tobias" -> Map("age" -> 25l),
    "Peter" -> Map("age" -> 34l)
  )

  def section = "Where"

  @Test def filter_on_property() {
    testQuery(
      title = "Filter on node property",
      text = "To filter on a property, write your clause after the `WHERE` keyword.",
      queryText = """start n=node(%Andres%, %Tobias%) where n.age < 30 return n""",
      returns = """The node.""",
      (p) => assertEquals(List(node("Tobias")), p.columnAs[Node]("n").toList))
  }

  @Test def boolean_operations() {
    testQuery(
      title = "Boolean operations",
      text = "You can use the expected boolean operators `AND` and `OR`, and also the boolean function `NOT()`.",
      queryText = """start n=node(%Andres%, %Tobias%) where (n.age < 30 and n.name = "Tobias") or not(n.name = "Tobias")  return n""",
      returns = """The node.""",
      (p) => assertEquals(List(node("Andres"), node("Tobias")), p.columnAs[Node]("n").toList))
  }

  @Test def regular_expressions() {
    testQuery(
      title = "Regular expressions",
      text = "You can match on regular expressions by using `=~ /regexp/`, like this:",
      queryText = """start n=node(%Andres%, %Tobias%) where n.name =~ /Tob.*/ return n""",
      returns = """The node named Tobias.""",
      (p) => assertEquals(List(node("Tobias")), p.columnAs[Node]("n").toList))
  }

  @Test def has_property() {
    testQuery(
      title = "Property exists",
      text = "To only include nodes/relationships that have a property, just write out the identifier and the property you expect it to have.",
      queryText = """start n=node(%Andres%, %Tobias%) where n.belt return n""",
      returns = """The node named Andres.""",
      (p) => assertEquals(List(node("Andres")), p.columnAs[Node]("n").toList))
  }

  @Test def compare_if_property_exists() {
    testQuery(
      title = "Compare if property exists",
      text = "If you want to compare a property on a graph element, but only if it exists, use the nullable property syntax. It is the property" +
        " with the dot notation, followed by a question mark",
      queryText = """start n=node(%Andres%, %Tobias%) where n.belt? = 'white' return n""",
      returns = "All nodes, even those without the belt property",
      (p) => assertEquals(List(node("Andres"), node("Tobias")), p.columnAs[Node]("n").toList))
  }

  @Test def filter_on_relationship_type() {
    testQuery(
      title = "Filtering on relationship type",
      text = "You can put the exact relationship type in the `MATCH` pattern, but sometimes you want to be able to do more " +
        "advanced filtering on the type. You can use the special property `TYPE` to compare the type with something else. " +
        "In this example, the query does a regular expression comparison with the name of the relationship type.",
      queryText = """start n=node(%Andres%) match (n)-[r]->() where type(r) =~ /K.*/ return r""",
      returns = """The relationship that has a type whose name starts with K.""",
      (p) => assertEquals("KNOWS", p.columnAs[Relationship]("r").toList.head.getType.name()))
  }

  @Test def filter_on_null() {
    testQuery(
      title = "Filter on null values",
      text = "Sometimes you might want to test if a value or an identifier is null. This is done just like SQL does it, with IS NULL." +
        " Also like SQL, the negative is IS NOT NULL, althought NOT(IS NULL x) also works.",
      queryText = """start a=node(%Tobias%), b=node(%Andres%, %Peter%) match a<-[r?]-b where r is null return b""",
      returns = "Nodes that Tobias is not connected to",
      (p) => assertEquals(List(Map("b" -> node("Peter"))), p.toList))
  }
}