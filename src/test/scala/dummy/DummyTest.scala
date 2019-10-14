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
import java.util.{Map=>JMap, HashMap=>JHashMap}
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
import org.kie.api.KieServices
import org.kie.api.definition.`type`.{Role, Timestamp}
import org.kie.api.event.rule.{ObjectDeletedEvent, ObjectInsertedEvent, ObjectUpdatedEvent, RuleRuntimeEventListener}
import org.kie.api.runtime.KieSession
import org.kie.api.runtime.KieSessionConfiguration
import org.kie.api.runtime.conf.ClockTypeOption

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

case class Hello(message:String)
case class HelloResponse(message:String)

@Role(Role.Type.EVENT)
@Timestamp("timestamp")
trait GenericEvent {
  val kind: String
  val props: JMap[String, String]
  val timestamp: Date
}

case class Event(
  kind:String,
  props:JMap[String,String]=new JHashMap(),
  timestamp:Date=new Date()
) extends GenericEvent

case class EventDecision(name:String)

class DummyTest extends FunSuite {
  val logger = LoggerFactory.getLogger("DummyTest")

  
  
  
  test("fired up test") {
    def model = {
      val martine = Someone(name="Martine", age=30, nicknames=List("titine", "titi").asJava, attributes=Map("hairs"->"brown").asJava)
      val martin  = Someone(name="Martin", age=40, nicknames=List("tintin", "titi").asJava, attributes=Map("hairs"->"black").asJava)
      val jack    = Someone(name="Jack", age=12, nicknames=List("jacquouille").asJava, attributes=Map("eyes"->"blue").asJava)
      val martineCar = Car(martine, "Ford", 2010, Color.blue)
      val martinCar  = Car(martin, "GM", 2010, Color.black)
      val martinCar2 = Car(martin, "Ferrari", 2012, Color.red)
      val martinCar3 = Car(martin, "Porshe", 2011, Color.red)

      val martinHome = Home(martin, None)
      val jackHome   = Home(jack, Some(Address("221B Baker Street", "London", "England")))

      List(
        martine,
        martin,
        jack,
        martineCar,
        martinCar,
        martinCar2,
        martinCar3,
        martinHome,
        jackHome
      )
    }

    //System.setProperty("drools.dialect.java.compiler", "JANINO")
    val config = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration()
    config.setProperty("drools.dialect.mvel.strict", "false")
    val kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(config)

    val res = ResourceFactory.newClassPathResource("dummy/people/KB-People.drl")
    kbuilder.add(res, ResourceType.DRL)

    val errors = kbuilder.getErrors();
    if (errors.size() > 0) {
      for (error <- errors.asScala) logger.error(error.getMessage())
      throw new IllegalArgumentException("Problem with the Knowledge base");
    }

    val kbase = kbuilder.newKieBase()

    val found = using(kbase.newKieSession()) { session =>
      session.setGlobal("logger", LoggerFactory.getLogger("KBPeople"))
      model.foreach(session.insert(_))
      session.fireAllRules()
      session.getObjects()
    }

    val all = found.asScala collect { case x:Information => x}
    //all.foreach{i=> info(i.toString)}
    
    val valuableInfos = all collect { case x:InformationRemarkable => x}
    val partialInfos = all collect { case x:InformationRequest => x}
    
    valuableInfos should have size(2)
    partialInfos should have size(2)
    
    partialInfos.map(_.someone.name) should contain("Martine")
  }




  test("minimalist kie api usage test") {
    val kServices = KieServices.Factory.get
    val kContainer = kServices.getKieClasspathContainer()
    val conf = kServices.newKieBaseConfiguration()
    val kbase = kContainer.newKieBase("HelloKB", conf)

    using(kbase.newKieSession) { session =>
      session.setGlobal("logger", LoggerFactory.getLogger("HelloKB"))

      session.insert(Hello("world"))

      session.fireAllRules()

      val messages = session.getObjects().asScala.collect {case HelloResponse(msg) => msg}
      messages should have size(1)
      messages.headOption.value should equal("cool")
    }
  }




  test("map kie api usage test") {
    val kServices = KieServices.Factory.get
    val kContainer = kServices.getKieClasspathContainer()
    val conf = kServices.newKieBaseConfiguration()
    val kbase = kContainer.newKieBase("MapKB", conf)

    using(kbase.newKieSession) { session =>
      session.setGlobal("logger", LoggerFactory.getLogger("MapKB"))

      val fact1 = Event("truc")
      val fact2 = Event("machin", Map("x"->"1").asJava)
      val fact3 = Event("machin", Map("x"->"2", "y"->"3").asJava)
      
      session.insert(fact1)
      session.insert(fact2)
      session.insert(fact3)

      session.fireAllRules()

      val messages = session.getObjects().asScala.collect {case EventDecision(msg) => msg}
      messages should have size(3)
      messages.filter(_.contains("truc")) should have size(1)
      messages.filter(_.contains("machin")) should have size(2)
    }
  }


  
  test("event test using drools expert classic API") {
    val kconfig = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration()
    kconfig.setProperty("drools.dialect.mvel.strict", "false")

    val kbconfig = KnowledgeBaseFactory.newKnowledgeBaseConfiguration()
    kbconfig.setOption( EventProcessingOption.STREAM )

    val ksconfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration()
    ksconfig.setOption( ClockTypeOption.get("pseudo") )

    val kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(kconfig)
    val res = ResourceFactory.newClassPathResource("dummy/events/KB-Events.drl")
    kbuilder.add(res, ResourceType.DRL)

    val errors = kbuilder.getErrors();
    if (errors.size() > 0) {
      for (error <- errors.asScala) logger.error(error.getMessage())
      throw new IllegalArgumentException("Problem with the Knowledge base");
    }

    val kbase = KnowledgeBaseFactory.newKnowledgeBase( kbconfig )
    kbase.addPackages(kbuilder.getKnowledgePackages)

    val cpuPeakType = kbase.getFactType("dummy.events", "CpuPeak")
    val startEventType = kbase.getFactType("dummy.events", "StartEvent")
    cpuPeakType should not be(null)
    startEventType should not be(null)

    val found = using(kbase.newKieSession(ksconfig, null)) { session =>
      session.setGlobal("logger", LoggerFactory.getLogger("KBEvents-CLASSIC"))

      val clock = session.getSessionClock().asInstanceOf[SessionPseudoClock]

      val start1 = startEventType.newInstance()
      startEventType.set(start1, "host", "dummyHost")
      startEventType.set(start1, "name", "fakeApp")
      session.insert(start1)

      clock.advanceTime(10, TimeUnit.SECONDS)

      val peak1 = cpuPeakType.newInstance
      cpuPeakType.set(peak1, "host", "dummyHost")
      cpuPeakType.set(peak1, "value", 94)
      session.insert(peak1)

      clock.advanceTime(45, TimeUnit.MINUTES)

      val start2 = startEventType.newInstance()
      startEventType.set(start2, "host", "dummyHost")
      startEventType.set(start2, "name", "fakeApp")
      session.insert(start2)


      session.fireAllRules()
      session.getObjects()
    }

    found.size() should be > 0
  }
  
  
  
  
  
  test("event test using KIE API (KIE = Knowledge Is Everywhere)") {

    val kServices = KieServices.Factory.get
    val kContainer = kServices.getKieClasspathContainer()
    val conf = kServices.newKieBaseConfiguration()
    val kbase = kContainer.newKieBase("EventsKB", conf)


    val cpuPeakType = kbase.getFactType("dummy.events", "CpuPeak")
    val startEventType = kbase.getFactType("dummy.events", "StartEvent")

    cpuPeakType should not be(null)
    startEventType should not be(null)

    val ksEnv = kServices.newEnvironment()
    val ksConf = kServices.newKieSessionConfiguration()
    ksConf.setOption(ClockTypeOption.get("pseudo"))

    val found = using(kbase.newKieSession(ksConf,ksEnv)) { session =>
      session.setGlobal("logger", LoggerFactory.getLogger("KBEvents-KIE"))

      val clock = session.getSessionClock().asInstanceOf[SessionPseudoClock]

      val start1 = startEventType.newInstance()
      startEventType.set(start1, "host", "dummyHost")
      startEventType.set(start1, "name", "fakeApp")
      session.insert(start1)

      clock.advanceTime(10, TimeUnit.SECONDS)

      val peak1 = cpuPeakType.newInstance
      cpuPeakType.set(peak1, "host", "dummyHost")
      cpuPeakType.set(peak1, "value", 94)
      session.insert(peak1)

      clock.advanceTime(45, TimeUnit.MINUTES)

      val start2 = startEventType.newInstance()
      startEventType.set(start2, "host", "dummyHost")
      startEventType.set(start2, "name", "fakeApp")
      session.insert(start2)

      session.fireAllRules()
      session.getObjects()
    }

  }




  test("events very simple stream oriented test") {

    val kServices = KieServices.Factory.get
    val kContainer = kServices.getKieClasspathContainer()
    val conf = kServices.newKieBaseConfiguration()
    val kbase = kContainer.newKieBase("StreamPingPongKB", conf)


    val ksEnv = kServices.newEnvironment()
    val ksConf = kServices.newKieSessionConfiguration()
    ksConf.setOption(ClockTypeOption.get("pseudo"))

    val pingType = kbase.getFactType("dummy.streamevents", "Ping")
    pingType should not be(null)
    def ping() = {
      pingType.newInstance()
    }

    val found = using(kbase.newKieSession(ksConf,ksEnv)) { session =>
      session.setGlobal("logger", LoggerFactory.getLogger("KBPingPong"))

      //val clock = session.getSessionClock().asInstanceOf[SessionPseudoClock]

      val pongPromise = Promise[Object]()

      session.addEventListener(new RuleRuntimeEventListener {
        override def objectDeleted(event: ObjectDeletedEvent): Unit = {}
        override def objectUpdated(event: ObjectUpdatedEvent): Unit = {}
        override def objectInserted(event: ObjectInsertedEvent): Unit = {
          Option(event.getRule).map { rule =>
            pongPromise.complete(Success(event.getObject))
          }
        }
      })


      val engine = Future {
        blocking {
          session.fireUntilHalt()
        }
      }
      session.insert(ping())
      val r = Await.result(pongPromise.future, 10.seconds)

      r.getClass.getCanonicalName should include("Pong")

      session.halt()
      Await.ready(engine, 10.seconds)

      session.getObjects().size should be >(1)
    }

  }


}
