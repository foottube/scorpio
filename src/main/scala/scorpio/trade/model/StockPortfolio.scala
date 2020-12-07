package scorpio.trade.model

import java.time.ZonedDateTime

import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.bson.collection.immutable.Document
import scorpio.dao.mongo.{MongoEntity, MongoOperation, MongoUtils, TimedMongoOperation}

/**
  * Created by HONGBIN on 2017/1/15.
  */
case class StockPortfolio(positions: Vector[StockPosition]) extends MongoEntity[StockPortfolio] {

  override def toDocument = Document(
    "_id" -> 1,
    "positions" -> positions.map(_.toDocument)
  )

}

object StockPortfolio extends MongoOperation[StockPortfolio] {

  override val collectionName = "StockPortfolio"

  override def fromDocument(document: Document) = {
    val positions = MongoUtils.extractArray(document.get("positions")).
      getOrElse(throw new IllegalArgumentException(s"Cannot extract positions from $document")).
      toArray(new Array[BsonValue](0)).
      map(bson => StockPosition.fromDocument(Document(bson.asDocument()))).toVector
    StockPortfolio(positions)
  }

}

case class PortfolioValue(
                         stocks: Vector[StockPositionValue],
                         cash: CashValue,
                         datetime: ZonedDateTime
                         ) extends MongoEntity[PortfolioValue] {
  override def toDocument = Document(
    "stocks" -> stocks.map(_.toDocument),
    "cash" -> cash.toDocument,
    "datetime" -> MongoUtils.toBsonDateTime(datetime)
  )
}

object PortfolioValue extends TimedMongoOperation[PortfolioValue] {

  override val collectionName = "PortfolioValue"

  override val dateTimeField = "datetime"

  override def fromDocument(document: Document) = {
    val stocks = MongoUtils.extractArray(document.get("stocks")).
      getOrElse(throw new IllegalArgumentException(s"Cannot extract stocks from $document")).
      toArray(new Array[BsonValue](0)).
      map(bson => StockPositionValue.fromDocument(Document(bson.asDocument()))).toVector
    val cash = document.get("cash").map(bson => CashValue.fromDocument(Document(bson.asDocument()))).
      getOrElse(throw new IllegalArgumentException(s"Cannot extract cash from $document"))
    PortfolioValue(
      stocks = stocks,
      cash = cash,
      datetime = MongoUtils.getZonedDateTime(document, "datetime")
    )
  }
}