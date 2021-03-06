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
package org.neo4j.cypher.pipes.aggregation

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

import org.junit.Assert._
import org.junit.Test
import org.neo4j.cypher.SyntaxException
import org.scalatest.junit.JUnitSuite
import org.neo4j.cypher.commands.EntityValue

class SumFunctionTest extends JUnitSuite {
  @Test def singleValueReturnsThatNumber() {
    val result = sumOn(1)

    assertEquals(1, result)
    assertTrue(result.isInstanceOf[Int])
  }

  @Test def singleValueOfDecimalReturnsDecimal() {
    val result = sumOn(1.0d)

    assertEquals(1.0, result)
    assertTrue(result.isInstanceOf[Double])
  }

  @Test def mixOfIntAndDoubleYieldsDouble() {
    val result = sumOn(1, 1.0d)

    assertEquals(2.0, result)
    assertTrue(result.isInstanceOf[Double])
  }

  @Test def mixedLotsOfStuff() {
    val result = sumOn(1.byteValue(), 1.shortValue())

    assertEquals(2, result)
    assertTrue(result.isInstanceOf[Int])
  }

  @Test def noNumbersEqualsZero() {
    val result = sumOn()

    assertEquals(0, result)
    assertTrue(result.isInstanceOf[Int])
  }

  @Test def nullDoesNotChangeTheSum() {
    val result = sumOn(1, null)

    assertEquals(1, result)
    assertTrue(result.isInstanceOf[Int])
  }

  @Test(expected = classOf[SyntaxException]) def noNumberValuesThrowAnException() {
    sumOn(1, "wut")
  }

  def sumOn(values: Any*): Any = {
    val func = new SumFunction(EntityValue("x"))

    values.foreach(value => {
      func(Map("x" -> value))
    })

    func.result
  }
}