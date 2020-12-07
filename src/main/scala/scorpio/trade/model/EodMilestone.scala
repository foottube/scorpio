package scorpio.trade.model

import java.time.ZonedDateTime

import org.mongodb.scala.bson.collection.immutable.Document
import scorpio.dao.mongo.{MongoEntity, MongoUtils, TimedMongoOperation}

/**
  * Created by HONGBIN on 2017/1/22.
  */
case class EodMilestone(
                       dateTime: ZonedDateTime
                       ) extends MongoEntity[EodMilestone] {

  override def toDocument = Document(
    "dateTime" -> MongoUtils.toBsonDateTime(dateTime)
  )

}

object EodMilestone extends TimedMongoOperation[EodMilestone] {
  override val collectionName = "EodMilestone"

  override val dateTimeField = "dateTime"

  override def fromDocument(document: Document) = EodMilestone(MongoUtils.getZonedDateTime(document, "dateTime"))
}
