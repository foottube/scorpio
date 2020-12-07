package scorpio.gateway.mail

import java.time.{Instant, ZoneId, ZonedDateTime}

import org.mongodb.scala.bson.collection.immutable.Document
import scorpio.dao.mongo.{MongoOperation, MongoUtils}
import scorpio.dao.mongo.{MongoEntity, MongoOperation, MongoUtils}

/**
  * Created by HONGBIN on 2017/1/1.
  */
case class MailMessage(
                        folder: String,
                        from: String,
                        sentInstant: Instant,
                        subject: String,
                        content: String
                      ) extends MongoEntity[MailMessage] {
  override def toDocument = Document(
    "_id" -> 1,
    "folder" -> folder,
    "from" -> from,
    "sentInstant" -> sentInstant.toEpochMilli,
    "subject" -> subject,
    "content" -> content
  )

  override def toString: String = s"From $from to $folder (at ${ZonedDateTime.ofInstant(sentInstant, ZoneId.of("UTC"))}) with subject $subject: $content"
}

object MailMessage extends MongoOperation[MailMessage] {

  override val collectionName = "MailMessage"

  override def fromDocument(document: Document) = MailMessage(
    folder = MongoUtils.getString(document, "folder"),
    from = MongoUtils.getString(document, "from"),
    sentInstant = Instant.ofEpochMilli(MongoUtils.getLong(document, "sentInstant")),
    subject = MongoUtils.getString(document, "subject"),
    content = MongoUtils.getString(document, "content")
  )
}

