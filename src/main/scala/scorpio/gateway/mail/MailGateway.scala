package scorpio.gateway.mail

import java.time.Instant
import java.util.Properties
import javax.mail._
import javax.mail.internet.{InternetAddress, MimeMessage, MimeMultipart}

import com.typesafe.config.ConfigFactory
import scorpio.common.Log

/**
  * Created by HONGBIN on 2016/11/1.
  */

case class MailConfig(
                       defaultFolder: String,
                       smtpHost: String,
                       smtpPort: Int,
                       imapHost: String,
                       imapPort: Int,
                       username: String,
                       password: String
                     )


object MailGateway extends Log {

  val config = ConfigFactory.load()

  val mailConfig = MailConfig(
    config.getString("mail.defaultFolder"),
    config.getString("mail.smtpHost"),
    config.getInt("mail.smtpPort"),
    config.getString("mail.imapHost"),
    config.getInt("mail.imapPort"),
    config.getString("mail.username"),
    config.getString("mail.password")
  )

  val replyFrom: String = config.getString("mail.replyFrom")

  val imapSession: Session = {
    val props = new Properties()
    props.put("mail.store.protocol", "imap")
    props.put("mail.imap.ssl.enable", true.asInstanceOf[Object])
    props.put("mail.imap.host", mailConfig.imapHost)
    props.put("mail.imap.port", mailConfig.imapPort.asInstanceOf[Object])

    Session.getInstance(props)
  }

  def sendMessage(from: String, to: String, subject: String, content: String): Unit = {
    val props: Properties = new Properties()
    props.put("mail.smtp.ssl.enable", true.asInstanceOf[Object])
    props.put("mail.smtp.host", mailConfig.smtpHost)
    props.put("mail.smtp.port", mailConfig.smtpPort.asInstanceOf[Object])
    props.put("mail.smtp.auth", true.asInstanceOf[Object])

    val session = Session.getDefaultInstance(props, new Authenticator {
      override def getPasswordAuthentication: PasswordAuthentication = {
        new PasswordAuthentication(mailConfig.username, mailConfig.password)
      }
    })

    val message = new MimeMessage(session)
    message.setFrom(new InternetAddress(from))
    message.addRecipient(Message.RecipientType.TO, new InternetAddress(to))
    message.setSubject(subject)
    message.setText(content)

    logger.debug(s"Send mail from $from to $to with subject $subject and content $content")

    Transport.send(message)
  }

  def replyMessage(success: Boolean, content: String, msg: MailMessage): Unit = {
    MailGateway.sendMessage(
      from = replyFrom,
      to = msg.from,
      subject = if (success) s"SUCCESS - ${msg.subject}" else s"FAIL - ${msg.subject}",
      content = content
    )
  }

  def pollMessage(folderName: String = mailConfig.defaultFolder): MailMessage = {

    logger.info(s"Poll message from $folderName")

    val store = imapSession.getStore("imap")
    try {
      store.connect(mailConfig.username, mailConfig.password)
      val folder = store.getFolder(folderName)
      try {
        folder.open(Folder.READ_ONLY)
        val size = folder.getMessageCount
        logger.debug(s"$folderName has $size messages")
        val message = folder.getMessage(size)
        logger.debug(s"Retrieved $message from $folderName")
        buildMessage(folderName, message)
      } finally {
        if (folder.isOpen) folder.close(false)
      }
    } finally {
      store.close()
    }
  }

  def pollMessageReceivedAfter(instant: Instant, folderName: String = mailConfig.defaultFolder): Seq[MailMessage] = {

    logger.info(s"Poll message received after $instant from $folderName")

    val store = imapSession.getStore("imap")
    var messages: List[MailMessage] = Nil
    try {
      store.connect(mailConfig.username, mailConfig.password)
      val folder = store.getFolder(folderName)
      try {
        folder.open(Folder.READ_ONLY)
        var size = folder.getMessageCount
        logger.debug(s"$folderName has $size messages")
        var message = folder.getMessage(size)
        var sentTime = Instant.ofEpochMilli(message.getSentDate.getTime)
        while (sentTime.isAfter(instant) && size > 0) {
          logger.debug(s"Retrieved $message from $folderName")
          messages = buildMessage(folderName, message) :: messages
          size = size - 1
          if (size > 0) {
            message = folder.getMessage(size)
            sentTime = Instant.ofEpochMilli(message.getSentDate.getTime)
          }
        }
      } finally {
        if (folder.isOpen) folder.close(false)
      }
    } finally {
      store.close()
    }
    messages
  }

  private def buildMessage(folderName: String, message: Message): MailMessage = {
    val contents = List.newBuilder[String]
    def parseMultipart(multipart: MimeMultipart): Unit = {
      (0 until multipart.getCount) foreach { idx =>
        multipart.getBodyPart(idx) match {
          case multi: BodyPart if multi.getContent.isInstanceOf[MimeMultipart] => parseMultipart(multi.getContent.asInstanceOf[MimeMultipart])
          case part: BodyPart if part.getContentType.contains("text/plain") =>
            contents += part.getContent.toString
          case other: BodyPart => logger.debug(s"Unknown MIME type ${other.getContentType}")
        }
      }
    }
    // There's problem when parsing MimeMultipart messages. Skip this step for now. Only use subject to convey information.
    // TODO: fix the problem in parsing MimeMultipart messages
//    message.getContent match {
//      case multipart: MimeMultipart => parseMultipart(multipart)
//      case other => contents += other.toString
//    }
    MailMessage(
      folder = folderName,
      from = message.getFrom.head.toString,
      sentInstant = Instant.ofEpochMilli(message.getSentDate.getTime),
      subject = message.getSubject,
      content = contents.result().map(_.trim).mkString("\n")
    )
  }

}
