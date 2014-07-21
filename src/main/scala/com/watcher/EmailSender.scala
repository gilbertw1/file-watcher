package com.watcher

import javax.mail._
import javax.mail.internet._
import java.util.Properties

object EmailSender {
	def apply(config: EmailConfig): EmailSender = {
		new EmailSender(config.host, config.user, config.pass, config.port, config.enabled)
	}
}

class EmailSender(host: String, user: String, pass: String, port: java.lang.Integer, enabled: Boolean) {

	private val authenticator = new SMTPAuthenticator(user, pass)

	private val properties = {
		val props = new Properties
		props.put("mail.transport.protocol", "smtp")
		props.put("mail.smtp.host", host)
		props.put("mail.smtp.port", port)
		props.put("mail.smtp.auth", "true")
		props.put("mail.smtp.starttls.enable","true")
		props
	}

	def sendFailureEmail(sendTo: String, failedFile: String, url: String, error: String) {
		if(enabled) {
			val mailSession = Session.getDefaultInstance(properties, authenticator)
			val transport = mailSession.getTransport("smtps")
			val message = new MimeMessage(mailSession)
			message.setText(s"Failed to post file (${failedFile}) to url (${url})\nError: ${error}")
			message.setSubject("Failed File Upload")
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(sendTo))
			transport.connect(host, port, user, pass)
			transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO))
			transport.close()
		}
	}
}

class SMTPAuthenticator(user: String, pass: String) extends Authenticator {
    override def getPasswordAuthentication(): PasswordAuthentication = {
       return new PasswordAuthentication(user, pass)
    }
}