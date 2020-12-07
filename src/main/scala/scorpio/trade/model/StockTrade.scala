package scorpio.trade.model

import java.time.ZonedDateTime

import org.mongodb.scala.bson.collection.immutable.Document
import scorpio.dao.mongo.{MongoEntity, MongoOperation, MongoUtils, TimedMongoOperation}

/**
  * Created by HONGBIN on 2017/1/1.
  */
case class StockTrade(
                     code: String,
                     name: String,
                     buy: Boolean,
                     quantity: Int,   // # of shares
                     price: Double,   // deal price per share
                     value: Double,   // price * quantity
                     payment: Double, // value + commission
                     dateTime: ZonedDateTime
                     ) extends MongoEntity[StockTrade] {
  override def toDocument = Document(
    "code" -> code,
    "name" -> name,
    "buy" -> buy,
    "quantity" -> quantity,
    "price" -> price,
    "value" -> value,
    "payment" -> payment,
    "datetime" -> MongoUtils.toBsonDateTime(dateTime)
  )
}

object StockTrade extends TimedMongoOperation[StockTrade] {

  override val collectionName = "StockTrade"

  override val dateTimeField = "datetime"

  override def fromDocument(document: Document) = StockTrade(
    code = MongoUtils.getString(document, "code"),
    name = MongoUtils.getString(document, "name"),
    buy = MongoUtils.getBoolean(document, "buy"),
    quantity = MongoUtils.getInt(document, "quantity"),
    price = MongoUtils.getDouble(document, "price"),
    value = MongoUtils.getDouble(document, "value"),
    payment = MongoUtils.getDouble(document, "payment"),
    dateTime = MongoUtils.getZonedDateTime(document, "datetime")
  )
}
