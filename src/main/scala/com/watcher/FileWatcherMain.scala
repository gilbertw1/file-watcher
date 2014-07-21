package com.watcher

import scala.io.Source
import akka.actor.{ActorSystem, Props}

object FileWatcherMain extends App {
	if(args.size < 1) {
		println("No Config File Specified")
		println("Usage: file-watcher.jar <config.json>")
		System.exit(0)
	}
	val configJson = Source.fromFile(args(0)).mkString
	val config = FileWatcherConfig(configJson)
	val system = ActorSystem("FileWatcherSystem")
	val emailSender = EmailSender(config.emailConfig)

	config.watchedDirectories.foreach { dirConfig =>
		val pathId = dirConfig.path.replaceAll("/","")
		system.actorOf(Props(new FileWatcherActor(dirConfig, emailSender)), s"FileWatcherActor-${pathId}")
	}

	println("File System Watcher Started!")
}