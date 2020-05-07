/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
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
package org.neo4j.cypher.internal.plandescription

import org.neo4j.cypher.internal.ir.ordering.ProvidedOrder

sealed abstract class Argument extends Product {

  def name: String = productPrefix
}

object Arguments {
  object Details {
    def apply(details: String): Details = Details(Seq(details))
  }

  case class Details(info: Seq[String]) extends Argument

  case class Time(value: Long) extends Argument

  case class Rows(value: Long) extends Argument

  case class DbHits(value: Long) extends Argument

  case class Memory(value: Long) extends Argument

  case class GlobalMemory(value: Long) extends Argument

  case class Order(order: ProvidedOrder) extends Argument

  case class PageCacheHits(value: Long) extends Argument

  case class PageCacheMisses(value: Long) extends Argument

  case class PageCacheHitRatio(value: Double) extends Argument

  case class EstimatedRows(value: Double) extends Argument

  case class PipelineInfo(pipelineId: Int, fused: Boolean) extends Argument

  // This is the version of cypher
  case class Version(value: String) extends Argument {

    override def name = "version"
  }

  case class RuntimeVersion(value: String) extends Argument {

    override def name = "runtime-version"
  }

  case class Planner(value: String) extends Argument {

    override def name = "planner"
  }

  case class PlannerImpl(value: String) extends Argument {

    override def name = "planner-impl"
  }

  case class PlannerVersion(value: String) extends Argument {

    override def name = "planner-version"
  }

  case class Runtime(value: String) extends Argument {

    override def name = "runtime"
  }

  case class RuntimeImpl(value: String) extends Argument {

    override def name = "runtime-impl"
  }

  case class SourceCode(className: String, sourceCode: String) extends Argument {

    override def name: String = "source:" + className
  }

  case class ByteCode(className: String, disassembly: String) extends Argument {

    override def name: String = "bytecode:" + className
  }
}