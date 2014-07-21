import sbt._
import Keys._
import com.github.retronym.SbtOneJar

object BuildSettings {
	val buildOrganization = "com.watcher"
	val buildVersion      = "0.1"
	val buildScalaVersion = "2.10.1"

	val buildSettings = Defaults.defaultSettings ++ Seq (
		organization := buildOrganization,
		version      := buildVersion,
		scalaVersion := buildScalaVersion
	)
}

object Resolvers {
	val typesafeRepo = "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
	val playJsonSnapRepo = "Mandubian repository snapshots" at "https://github.com/mandubian/mandubian-mvn/raw/master/snapshots/"
	val playJsonRelRepo = "Mandubian repository releases" at "https://github.com/mandubian/mandubian-mvn/raw/master/releases/"
	val mavenRepo = "Maven repository" at "http://repo1.maven.org/maven2/"
	//http://repo1.maven.org/maven2/net/databinder/dispatch-mime_2.10/0.8.9/dispatch-mime_2.10-0.8.9.jar
}

object Dependencies {
	val akkaVersion = "2.1.2"

	val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
	val akkaRemote = "com.typesafe.akka" %% "akka-remote" % akkaVersion
	val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
	val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
	val akkaCluster = "com.typesafe.akka" %% "akka-cluster-experimental" % akkaVersion

	val javaMail = "javax.mail" % "mail" % "1.4.1"
	val json4s = "org.json4s" %% "json4s-jackson" % "3.2.4"
	val jodaTime = "joda-time" % "joda-time" % "2.1"
	val jodaConvert = "org.joda" % "joda-convert" % "1.2"
	val slf4j = "org.slf4j" % "slf4j-nop" % "1.6.4"
	val dispatch = "net.databinder.dispatch" %% "dispatch-core" % "0.10.0"

	val akkaDependencies = Seq(akkaActor, akkaRemote, akkaSlf4j, akkaTestkit, akkaCluster)
	val miscDependencies = Seq(jodaTime, jodaConvert, slf4j, json4s, javaMail, dispatch)
	val allDependencies = akkaDependencies ++ miscDependencies
}

object TurbineDB extends Build {
	import Resolvers._
	import BuildSettings._
	import Defaults._

	lazy val fileWatcher = 
		Project ("file-watcher", file("."))
			.settings ( buildSettings : _* )
			.settings ( SbtOneJar.oneJarSettings : _* )
			.settings ( resolvers ++= Seq(typesafeRepo, playJsonSnapRepo, playJsonRelRepo, mavenRepo) )
			.settings ( libraryDependencies ++= Dependencies.allDependencies )
			.settings ( scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature") )
}