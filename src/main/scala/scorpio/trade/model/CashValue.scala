package scorpio.trade.model

import java.time.ZonedDateTime

import org.mongodb.scala.bson.collection.immutable.Document
import scorpio.dao.mongo.{MongoEntity, MongoUtils, TimedMongoOperation}

/**
  * Created by HONGBIN on 2017/1/15.
  */
case class CashValue(
               amount: Double,
               datetime: ZonedDateTime
               ) extends MongoEntity[CashValue] {
  override def toDocument = Document(
    "amount" -> amount,
    "datetime" -> MongoUtils.toBsonDateTime(datetime)
  )
}

object CashValue extends TimedMongoOperation[CashValue] {

  override val collectionName = "Cash"

  override val dateTimeField = "datetime"

  override def fromDocument(document: Document) = CashValue(
    amount = MongoUtils.getDouble(document, "amount"),
    datetime = MongoUtils.getZonedDateTime(document, "datetime")
  )

}
