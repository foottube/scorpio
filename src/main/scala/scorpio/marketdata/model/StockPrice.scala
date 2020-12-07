package scorpio.marketdata.model

import org.bson.BsonValue
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.collection.immutable.Document
import scorpio.dao.mongo.{MongoEmbeddableFactory, MongoUtils}
import scorpio.dao.mongo.{MongoEmbeddable, MongoEmbeddableFactory, MongoUtils}

/**
  * Created by HONGBIN on 2017/1/14.
  */
case class StockPrice(code: String, price: Double) extends MongoEmbeddable[StockPrice] {

  override def toBson = BsonDocument(
    "code" -> code,
    "price" -> price
  )

}

object StockPrice extends MongoEmbeddableFactory[StockPrice] {
  override def fromBson(bson: BsonValue) = StockPrice(
    code = MongoUtils.getString(Document(bson.asDocument()), "code"),
    price = MongoUtils.getDouble(Document(bson.asDocument()), "price")
  )
}
