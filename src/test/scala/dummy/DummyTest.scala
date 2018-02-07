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


class DummyTest extends FunSuite {
  val logger = LoggerFactory.getLogger(Dummy.getClass())

  
  
  
  ignore("fired up test") {
    val found = Dummy.analyze(Dummy.model1, "dummy/people/KB-People.drl")
    val all = found.asScala collect { case x:Information => x}
    all.foreach{i=> info(i.toString)}
    
    val valuableInfos = all collect { case x:InformationRemarkable => x}
    val partialInfos = all collect { case x:InformationRequest => x}
    
    valuableInfos should have size(2)
    partialInfos should have size(2)
    
    partialInfos.map(_.someone.name) should contain("Martine")
  }
  
  
  test("event test using classic API") {
    try {
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
      val updateEventType = kbase.getFactType("dummy.events", "UpdateEvent")

      val peak1 = cpuPeakType.newInstance
      cpuPeakType.set(peak1, "host", "dummyHost")
      cpuPeakType.set(peak1, "value", 94)

      val update1 = updateEventType.newInstance()
      updateEventType.set(update1, "host", "dummyHost")
      updateEventType.set(update1, "name", "fakeApp")


      val found = using(kbase.newKieSession(ksconfig, null)) { session =>
        session.setGlobal("logger", LoggerFactory.getLogger("KBEvents"))

        val clock = session.getSessionClock().asInstanceOf[SessionPseudoClock]

        session.insert(update1)
        clock.advanceTime(10, TimeUnit.SECONDS)
        session.insert(peak1)

        session.fireAllRules()
        session.getObjects()
      }

      found.size() should be > 0
    } catch {
      case e:Exception => e.printStackTrace()
    }
    
  }
  
  
  
  
  
  ignore("event test using KIE (Knowledge Is Everywhere") {
    try {

      val kServices = KieServices.Factory.get
      val kContainer = kServices.getKieClasspathContainer()
      val conf = kServices.newKieBaseConfiguration()
      val kbase = kContainer.newKieBase("EventsKB", conf)
      
      /*
      val config = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration()
      //config.setProperty("drools.dialect.mvel.strict", "false")
      //config.setOption( EventProcessingOption.STREAM )
      config.setOption(ClockTypeOption.get("pseudo"))
      val kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(config)
      val res = ResourceFactory.newClassPathResource("KB-Events.drl")
      kbuilder.add(res, ResourceType.DRL)

      val errors = kbuilder.getErrors();
      if (errors.size() > 0) {
        for (error <- errors.asScala) logger.error(error.getMessage())
        throw new IllegalArgumentException("Problem with the Knowledge base");
      }
      */


      val cpuPeakType = kbase.getFactType("dummy", "CpuPeak")
      val updateEventType = kbase.getFactType("dummy", "UpdateEvent")

      val peak1 = cpuPeakType.newInstance
      cpuPeakType.set(peak1, "host", "dummyHost")
      cpuPeakType.set(peak1, "value", 94)

      val update1 = updateEventType.newInstance()
      updateEventType.set(update1, "host", "dummyHost")
      updateEventType.set(update1, "name", "fakeApp")


      val found = using(kbase.newKieSession()) { session =>
        session.setGlobal("logger", LoggerFactory.getLogger("KBEvents"))

        val clock = session.getSessionClock().asInstanceOf[SessionPseudoClock]

        session.insert(update1)
        clock.advanceTime(10, TimeUnit.SECONDS)
        session.insert(peak1)

        session.fireAllRules()
        session.getObjects()
      }

      found.size() should be > 0
    } catch {
      case e:Exception => e.printStackTrace()
    }
  }
  
  
}
