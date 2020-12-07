package scorpio.trade.model

import java.time.ZonedDateTime

import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters.lte
import org.mongodb.scala.model.Sorts.{descending, orderBy}
import scorpio.marketdata.model.StockPrice
import scorpio.dao.mongo.{MongoEntity, MongoOperation, MongoUtils, TimedMongoOperation}
import scorpio.trade.model.CashValue.findTopN

/**
  * Created by HONGBIN on 2017/1/14.
  */
case class StockPosition(
                   code: String,
                   name: String,
                   quantity: Int,
                   cost: Double,
                   payment: Double
                   ) extends MongoEntity[StockPosition] {
  override def toDocument = Document(
    "code" -> code,
    "name" -> name,
    "quantity" -> quantity,
    "cost" -> cost,
    "payment" -> payment
  )
}

object StockPosition extends MongoOperation[StockPosition] {

  override val collectionName = "StockPosition"

  override def fromDocument(document: Document) = StockPosition(
    code = MongoUtils.getString(document, "code"),
    name = MongoUtils.getString(document, "name"),
    quantity = MongoUtils.getInt(document, "quantity"),
    cost = MongoUtils.getDouble(document, "cost"),
    payment = MongoUtils.getDouble(document, "payment")
  )
}

case class StockPositionValue(
                             position: StockPosition,
                             price: StockPrice,
                             value: Double,
                             profit: Double,
                             datetime: ZonedDateTime
                             ) extends MongoEntity[StockPositionValue] {
  override def toDocument = Document(
    "position" -> position.toDocument,
    "price" -> price.toBson,
    "value" -> value,
    "profit" -> profit,
    "datetime" -> MongoUtils.toBsonDateTime(datetime)
  )
}

object StockPositionValue extends TimedMongoOperation[StockPositionValue] {

  override val collectionName = "StockPositionValue"

  override val dateTimeField = "datetime"

  override def fromDocument(document: Document) = {
    val position = document.get("position").map { bson =>
      StockPosition.fromDocument(Document(bson.asDocument()))
    }.getOrElse(throw new IllegalArgumentException(s"Cannot extract StockPosition from $document"))
    val price = document.get("price").map(StockPrice.fromBson(_)).
      getOrElse(throw new IllegalArgumentException(s"Cannot extract StockPosition from $document"))
    StockPositionValue(
      position = position,
      price = price,
      value = MongoUtils.getDouble(document, "value"),
      profit = MongoUtils.getDouble(document, "profit"),
      datetime = MongoUtils.getZonedDateTime(document, "datetime")
    )
  }

}
