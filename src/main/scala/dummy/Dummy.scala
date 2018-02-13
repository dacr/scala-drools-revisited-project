/*
 * Copyright 2012 David Crosson
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dummy

import dummy.model._
import java.io.FileInputStream
import java.io.InputStreamReader

import org.kie.api._
import org.kie.api.io._
import org.kie.api.runtime._
import org.kie.internal.builder._
import org.kie.internal.io._

import java.io.File
import collection.JavaConverters._
import org.slf4j.LoggerFactory

object Dummy {
  val logger = LoggerFactory.getLogger(Dummy.getClass())

  def main(args: Array[String]) {
    logger.warn("# test me through test cases...")
    logger.warn("run 'sbt test'")
  }




  def using[R, T <% { def dispose() }](getres: => T)(doit: T => R): R = {
    val res = getres
    try doit(res) finally res.dispose
  }

}
