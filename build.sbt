name := "scala-drools-revisited-Project"

version := "6"

scalaVersion := "2.13.1"

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/junitresults")

enablePlugins(JavaAppPackaging)

lazy val versions = new {
  val drools    = "7.27.0.Final"
  val shttp     = "1.7.1"
  val json4s    = "3.6.7"
  val logback   = "1.2.3"
  val scalatest = "3.0.8"
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
  "org.json4s"              %% "json4s-native"     % versions.json4s,
  "org.scalatest"           %% "scalatest"         % versions.scalatest % "test"
)

initialCommands in console := """
   import dummy._
"""
