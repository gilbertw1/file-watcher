package com.watcher

import org.json4s._
import org.json4s.jackson._
import org.json4s.jackson.JsonMethods._

object FileWatcherConfig {
	implicit val formats = Serialization.formats(NoTypeHints)

	def apply(json: String): FileWatcherConfig = {
		parse(json).extract[FileWatcherConfig]
	}
}

case class FileWatcherConfig (
	emailConfig: EmailConfig,
	watchedDirectories: List[WatchedDirectoryConfig]
)

case class EmailConfig (
	host: String,
	user: String,
	pass: String,
	port: Int,
	enabled: Boolean
)

case class WatchedDirectoryConfig (
	path: String,
	failureNotify: String,
	ignore: List[String],
	extensions: List[ExtensionConfig]
)

case class ExtensionConfig (
	extension: String,
	endpoint: EndpointConfig
)

case class EndpointConfig (
	url: String,
	user: Option[String] = None,
	pass: Option[String] = None
)