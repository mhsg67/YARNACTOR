
lazy val YARNCommon = project.in(file("YARNCommon")).
	settings(
		name := "YARNCommon",
		version := "1.0",
		scalaVersion := "2.11.4",
		exportJars := true,
		libraryDependencies ++= Seq(
			"com.typesafe.akka" %% "akka-actor" % "2.3.7",
			"com.typesafe" % "config" % "1.2.1",
			"joda-time" % "joda-time" % "2.5",
			"org.joda" % "joda-convert" % "1.2",
			"org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
		)
)

lazy val YARNResourceManager = project.in(file("YARNResourceManager")).dependsOn(YARNCommon).
	settings(
		name := "YARNResourceManager",
		version := "1.0",
		scalaVersion := "2.11.4",
		exportJars := true,
		mainClass in (Compile,run):= Some("ca.usask.agents.yarnactor.resourcemanager.agents.main"),
		libraryDependencies ++= Seq(
			"com.typesafe.akka" %% "akka-actor" % "2.3.7",
			"com.typesafe.akka" % "akka-remote_2.11" % "2.3.7",
			"com.typesafe" % "config" % "1.2.1",
			"joda-time" % "joda-time" % "2.5",
			"org.joda" % "joda-convert" % "1.2",
			"org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
			"com.typesafe.akka" %% "akka-camel" % "2.3.7",
			"org.apache.camel" % "camel-netty" % "2.14.1",
			"org.apache.camel" % "camel-jetty" % "2.14.1",
			"org.slf4j" % "slf4j-simple" % "1.7.10",
			"com.typesafe.play" % "play-json_2.11" % "2.4.0-M2"
		)
)


lazy val YARNJobManager = project.in(file("YARNJobManager")).dependsOn(YARNCommon).
	settings(
		name := "YARNJobManager",
		version := "1.0",
		scalaVersion := "2.11.4",
		exportJars := true,
		mainClass in (Compile,run):= Some("ca.usask.agents.yarnactor.jobmanager.agents.main"),
		libraryDependencies ++= Seq(
			"com.typesafe.akka" %% "akka-actor" % "2.3.7",
			"com.typesafe.akka" % "akka-remote_2.11" % "2.3.7",
			"com.typesafe" % "config" % "1.2.1",
			"joda-time" % "joda-time" % "2.5", 
			"org.joda" % "joda-convert" % "1.2",
			"org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"		
		)
)

lazy val YARNNodeManager = project.in(file("YARNNodeManager")).dependsOn(YARNCommon,YARNJobManager).
	settings(
		name := "YARNNodeManager",
		version := "1.0",
		scalaVersion := "2.11.4",
		exportJars := true,
		mainClass in (Compile,run):= Some("ca.usask.agents.yarnactor.nodemanager.agents.main"),
		libraryDependencies ++= Seq(
			"com.typesafe.akka" %% "akka-actor" % "2.3.7",
			"com.typesafe.akka" % "akka-remote_2.11" % "2.3.7",
			"com.typesafe" % "config" % "1.2.1",
			"joda-time" % "joda-time" % "2.5",
			"org.joda" % "joda-convert" % "1.2",
			"org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"		
		)
)
