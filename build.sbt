name := "ScalaDroolsDummyProject"

version := "5"

scalaVersion := "2.12.4"

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/junitresults")

enablePlugins(JavaAppPackaging)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xexperimental",
  "-feature",
  "-language:implicitConversions",
  "-language:reflectiveCalls"
)

lazy val versions = new {
  val drools    = "7.6.0.Final"
  val shttp     = "1.1.5"
  val logback   = "1.2.3"
  val scalatest = "3.0.5"
}


libraryDependencies ++= Seq(
    "drools-compiler",
    "drools-core"
).map("org.drools" % _ % versions.drools)


libraryDependencies ++= Seq(
  "ch.qos.logback"          %  "logback-classic"   % versions.logback,
  "com.softwaremill.sttp"   %% "core"              % versions.shttp,
  "com.softwaremill.sttp"   %% "json4s"            % versions.shttp,
  "com.softwaremill.sttp"   %% "akka-http-backend" % versions.shttp,
  "org.scalatest"           %% "scalatest"         % versions.scalatest % "test"
)

initialCommands in console := """
   import dummy._
"""

//resolvers += "jboss-releases" at "https://repository.jboss.org/nexus/content/repositories/releases"

//resolvers += "jboss-jsr94" at "http://repository.jboss.org/nexus/content/groups/public-jboss"

//resolvers += "sonatype-public" at "https://oss.sonatype.org/content/groups/public"

