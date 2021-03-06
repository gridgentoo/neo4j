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
package org.neo4j.cypher

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
import java.math.BigDecimal
import java.lang.Character

/**
 * Comparer is a trait that enables it's subclasses to compare to AnyRef with each other.
 */
trait Comparer {
  def compareValuesOfSameType(l: AnyRef, r: AnyRef): Int = (l, r) match {
    case (left: Comparable[AnyRef], right: Comparable[AnyRef]) => left.compareTo(right)
    case _ => throw new RuntimeException("This shouldn't happen")
  }

  def compareValuesOfDifferentTypes(l: Any, r: Any): Int = (l, r) match {
    case (left: Long, right: Number) => BigDecimal.valueOf(left).compareTo(BigDecimal.valueOf(right.doubleValue()))
    case (left: Number, right: Long) => BigDecimal.valueOf(left.doubleValue()).compareTo(BigDecimal.valueOf(right))
    case (left: Number, right: Number) => java.lang.Double.compare(left.doubleValue(), right.doubleValue())
    case (left: String, right: Character) => left.compareTo(right.toString)
    case (left: Character, right: String) => left.toString.compareTo(right.toString)
    case (null, null) => 0
    case (null, _) => 1
    case (_, null) => -1
    case (left, right) => {
      throw new SyntaxException("Don't know how to compare that. Left: " + left.toString + "; Right: " + right.toString)
    }
  }

  def areComparableOfSameType(l: AnyRef, r: AnyRef): Boolean = {
    l.isInstanceOf[Comparable[_]] &&
      r.isInstanceOf[Comparable[_]] &&
      l.getClass.isInstance(r)
  }

  def compare(left: Any, right: Any): Int = {
    if (left == Nil || right == Nil) {
      throw new RuntimeException("Can't compare against NULL")
    }

    val l = left.asInstanceOf[AnyRef]
    val r = right.asInstanceOf[AnyRef]

    val comparisonResult: Int =
      if (areComparableOfSameType(l, r)) {
        compareValuesOfSameType(l, r)
      } else {
        compareValuesOfDifferentTypes(left, right)
      }
    comparisonResult
  }

}











