name := "scalabcn-site"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  "com.newrelic.agent.java" % "newrelic-agent" % "3.5.0"
)     

play.Project.playScalaSettings
