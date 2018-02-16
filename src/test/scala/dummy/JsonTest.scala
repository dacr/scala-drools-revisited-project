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

import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.{HashMap => JHashMap, Map => JMap}

import org.scalatest._
import Matchers._
import OptionValues._
import dummy.Dummy.{logger, using}

import collection.JavaConverters._
import model._
import org.drools.core.impl.KnowledgeBaseFactory
import org.kie.api.conf.EventProcessingOption
import org.kie.api.io.ResourceType
import org.kie.api.runtime.conf.ClockTypeOption
import org.kie.api.time.SessionPseudoClock
import org.kie.internal.builder.KnowledgeBuilderFactory
import org.kie.internal.io.ResourceFactory
import org.slf4j.LoggerFactory
import org.kie.api.{KieBase, KieServices}
import org.kie.api.definition.`type`.{FactType, Role, Timestamp}
import org.kie.api.event.rule.{ObjectDeletedEvent, ObjectInsertedEvent, ObjectUpdatedEvent, RuleRuntimeEventListener}
import org.kie.api.runtime.KieSession
import org.kie.api.runtime.KieSessionConfiguration
import org.kie.api.runtime.conf.ClockTypeOption

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

class JsonTest extends FunSuite {
  val logger = LoggerFactory.getLogger("DummyTest")

  
  
  
  test("drools with json tests") {
    val kServices = KieServices.Factory.get
    val kContainer = kServices.getKieClasspathContainer()
    val conf = kServices.newKieBaseConfiguration()
    val kbase = kContainer.newKieBase("JsonKB", conf)

    using(kbase.newKieSession) { session =>
      session.setGlobal("logger", LoggerFactory.getLogger("JsonKB"))
  
      val json=
        """
          |{
          |  "_kind": "dummy.json.Bidule",
          |  "name": "John Doe"
          |  "age": 42
          |}
          |""".stripMargin

      // convert prototype implementation
      def json2object(json:String, kbase:KieBase):Object = {
        val KIND_KEY="_kind"
        import org.json4s._
        import org.json4s.native.JsonMethods._
        implicit val formats = org.json4s.DefaultFormats
        val parsed = parse(json)
        val kind = (parsed\KIND_KEY).extract[String]
        val kindType:FactType = kind.split("""[.](?=[^.]+$)""", 2) match {
          case Array(kindPackage, kindName) => kbase.getFactType(kindPackage, kindName)
          case Array(kindName) => kbase.getFactType("", kindName) // TODO: probably wrong, and also should never arise
        }
        
        val instance = kindType.newInstance()
        // TODO : quick & dirty impl change to a recursive one
        for {
          JObject(props) <- parsed
          (key,jvalue) <- props
          if key != KIND_KEY
          if Option(kindType.getField(key)).isDefined
        } {
          jvalue match {
            case JNull => kindType.set(instance, key, null)
            case JDouble(num) => kindType.set(instance, key, num)
            case JDecimal(num) => kindType.set(instance, key, num)
            case JInt(num) => kindType.set(instance, key, num)
            case JBool(bool) => kindType.set(instance, key, bool)
            case JString(str)=> kindType.set(instance, key, str)
            case _ => 
          }
        }
        
        instance
        
      }
      
      val fact1 = json2object(json, kbase)
      session.insert(fact1)
      
      session.fireAllRules()
      
      val results = session.getObjects.asScala.collect{case message:String => message}
      results should have size(1)
      results.headOption.value should be ("blah")
    }
  }
}
