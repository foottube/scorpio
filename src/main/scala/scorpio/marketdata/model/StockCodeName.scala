package scorpio.marketdata.model

import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.conversions.Bson
import scorpio.dao.mongo.{MongoEntity, MongoOperation, MongoUtils, ValidatableMongoOperation}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by HONGBIN on 2017/1/5.
  */
case class StockCodeName(code: String, name: String, valid: Boolean) extends MongoEntity[StockCodeName] {

  override def toDocument = Document(
    "code" -> code,
    "name" -> name,
    "valid" -> valid
  )

}

object StockCodeName extends ValidatableMongoOperation[StockCodeName] {

  override val collectionName = "StockCodeName"

  override val validField = "valid"

  override def fromDocument(document: Document) = StockCodeName(
    code = MongoUtils.getString(document, "code"),
    name = MongoUtils.getString(document, "name"),
    valid = MongoUtils.getBoolean(document, "valid")
  )

}
