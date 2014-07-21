package com.watcher

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try,Success,Failure}
import java.io.{File,PrintWriter}
import java.nio.file.{Path,Paths,Files}
import java.nio.file.StandardCopyOption._
import dispatch._
import com.ning.http.client.RequestBuilder
import akka.actor.{Actor,ActorLogging}


object FileWatcherActor {
	sealed trait FileSystemChange
	case class Created(fileOrDir: File) extends FileSystemChange
	case class Deleted(fileOrDir: File) extends FileSystemChange
}

import FileWatcherActor._

class FileWatcherActor(dirConfig: WatchedDirectoryConfig, emailSender: EmailSender) extends Actor with ActorLogging {
	val watchServiceTask = new WatchServiceTask(self)
	val pathId = dirConfig.path.replaceAll("/","")
	val watchThread = new Thread(watchServiceTask, s"WatchService-${pathId}")
	val extensionEndpointMap = dirConfig.extensions.map(ext => ext.extension -> ext.endpoint).toMap
	val failureNotify = dirConfig.failureNotify
	val ignoredExtensions = dirConfig.ignore

	override def preStart() {
		watchThread.setDaemon(true)
		watchThread.start()
		watchServiceTask watchRecursively Paths.get(dirConfig.path)
	}

	override def postStop() {
		watchThread.interrupt()
	}

	def getExtension(file: File): String = {
		file.getName.split('.').last
	}

	def shouldIgnore(file: File): Boolean = {
		ignoredExtensions.contains(getExtension(file))
	}

	def getEndpointConfig(file: File): EndpointConfig = {
		val ext = getExtension(file)
		extensionEndpointMap.get(ext).getOrElse(null)
	}

	def createRequestBuilder(endpoint: EndpointConfig, file: File): RequestBuilder = {
		val userOpt = endpoint.user
		val passOpt = endpoint.pass
		val eUrl = endpoint.url.replaceAll("\\$FILENAME",file.getName)

		if(userOpt.isDefined && passOpt.isDefined) {
			url(eUrl).as(userOpt.get, passOpt.get)
		} else {
			url(eUrl)
		}
	}

	def postFileToEndpoint(file: File): Try[String] = {
		val endpointConf = getEndpointConfig(file)

		if(endpointConf != null) {
			Try {
				val fileUploadRequest = createRequestBuilder(endpointConf, file) <<< file
				val res = Http(fileUploadRequest OK as.String)
				res()
			}
		} else {
			Failure(new Exception(s"No endpoint configured for file: ${file.getAbsolutePath}"))
		}
	}

	def writeResult(result: String, uploaded: File) {
		val resultFile = new File(uploaded.getAbsolutePath + ".result")
		val writer = new PrintWriter(resultFile)
		try { 
			writer.write(result) 
		} finally {
			writer.close()
		}
	}

	def sendFailureNotification(file: File, error: String) {
		val endpoint = getEndpointConfig(file)
		val enpointString = if(endpoint != null) endpoint.toString else "Null"
		log.info(s"Sending failure email. Failed posting to ${endpoint} with error: ${error}")
		emailSender.sendFailureEmail(failureNotify, file.getAbsolutePath, enpointString, error)
	}

	def changeFileToProcessedState(file: File) {
		val originalFilePath = Paths.get(file.getAbsolutePath)
		val processedFilePath = Paths.get(file.getAbsolutePath + ".processed")
		Files.move(originalFilePath, processedFilePath, REPLACE_EXISTING)
	}

	def receive = {
		case Created(file) =>

			if(shouldIgnore(file)) {
				log.info(s"Ignoring file: ${file.getAbsolutePath}")
			} else {
				val upload = postFileToEndpoint(file)
				changeFileToProcessedState(file)

				upload match {
					case Success(resp) => writeResult(resp, file)
					case Failure(err) => sendFailureNotification(file, err.toString)
				}

				println(s"File Processed: ${file.getAbsolutePath}")
			}

		case Deleted(fileOrDir) =>
			// Nothing to do for deleted file
		case _ =>
	}
}