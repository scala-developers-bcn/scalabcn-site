name := "scalabcn-site"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  cache
)     

play.Project.playScalaSettings

libraryDependencies ++= Seq(
  "com.netflix.rxjava" % "rxjava-scala" % "0.18.1",
  "oauth.signpost" % "signpost-core" % "1.2",
  "oauth.signpost" % "signpost-commonshttp4" % "1.2", 
  "org.apache.httpcomponents" % "httpclient" % "4.2",
  "commons-io" % "commons-io" % "2.0"
)
