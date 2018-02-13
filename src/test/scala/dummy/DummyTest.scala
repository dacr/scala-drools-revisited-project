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

import java.util.concurrent.TimeUnit

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
import org.kie.api.runtime.KieSession
import org.kie.api.runtime.KieSessionConfiguration
import org.kie.api.runtime.conf.ClockTypeOption


case class Hello(message:String)
case class HelloResponse(message:String)


class DummyTest extends FunSuite {
  val logger = LoggerFactory.getLogger(Dummy.getClass())

  
  
  
  test("fired up test") {
    val found = Dummy.analyze(Dummy.model1, "dummy/people/KB-People.drl")
    val all = found.asScala collect { case x:Information => x}
    all.foreach{i=> info(i.toString)}
    
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




  test("events stream test") {

    val kServices = KieServices.Factory.get
    val kContainer = kServices.getKieClasspathContainer()
    val conf = kServices.newKieBaseConfiguration()
    val kbase = kContainer.newKieBase("StreamEventsKB", conf)


    val cpuPeakType = kbase.getFactType("dummy.streamevents", "CpuPeak")
    val startEventType = kbase.getFactType("dummy.streamevents", "StartEvent")

    cpuPeakType should not be(null)
    startEventType should not be(null)

    val ksEnv = kServices.newEnvironment()
    val ksConf = kServices.newKieSessionConfiguration()
    ksConf.setOption(ClockTypeOption.get("pseudo"))

    val found = using(kbase.newKieSession(ksConf,ksEnv)) { session =>
      session.setGlobal("logger", LoggerFactory.getLogger("KBEvents-KIE"))

      val clock = session.getSessionClock().asInstanceOf[SessionPseudoClock]


      session.fireAllRules()
      session.getObjects()
    }

  }


}
