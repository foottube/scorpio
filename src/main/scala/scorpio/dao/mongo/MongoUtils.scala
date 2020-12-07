package scorpio.dao.mongo

import java.time.{Instant, ZoneId, ZonedDateTime}

import org.bson.BsonArray
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.{BsonDateTime, BsonValue}

/**
  * Created by HONGBIN on 2016/12/10.
  */
object MongoUtils {

  def extractString(bson: Option[BsonValue]) = bson.map(_.asString()).map(_.getValue)

  def extractDouble(bson: Option[BsonValue]) = bson.map(_.asDouble()).map(_.getValue)

  def extractInt(bson: Option[BsonValue]) = bson.map(_.asInt32()).map(_.getValue)

  def extractLong(bson: Option[BsonValue]) = bson.map(_.asInt64()).map(_.getValue)

  def extractBoolean(bson: Option[BsonValue]) = bson.map(_.asBoolean()).map(_.getValue)

  def extractArray(bson: Option[BsonValue]): Option[BsonArray] = bson.map(_.asArray()).map(_.asArray())

  def extractDateTime(bson: Option[BsonValue]) = bson.map(_.asDateTime()).map(_.getValue)

  def getString(doc: Document, key: String) = extractString(doc.get(key)).
    getOrElse(throw new IllegalArgumentException(s"Cannot get ${key} from ${doc}"))

  def getInt(doc: Document, key: String) = extractInt(doc.get(key)).
    getOrElse(throw new IllegalArgumentException(s"Cannot get ${key} from ${doc}"))

  def getLong(doc: Document, key: String) = extractLong(doc.get(key)).
    getOrElse(throw new IllegalArgumentException(s"Cannot get ${key} from ${doc}"))

  def getDouble(doc: Document, key: String) = extractDouble(doc.get(key)).
    getOrElse(throw new IllegalArgumentException(s"Cannot get ${key} from ${doc}"))

  def getBoolean(doc: Document, key: String) = extractBoolean(doc.get(key)).
    getOrElse(throw new IllegalArgumentException(s"Cannot get ${key} from ${doc}"))

  def getEpochInMilliseconds(doc: Document, key: String) = extractDateTime(doc.get(key)).
    getOrElse(throw new IllegalArgumentException(s"Cannot get ${key} from ${doc}"))

  def getEpochInMillisecondsOption(doc: Document, key: String) = extractDateTime(doc.get(key))

  def getVectorOfInt(doc: Document, key: String) =
    extractArray(doc.get(key)).map(_.toArray(new Array[BsonValue](0)).map(_.asInt32().getValue).toVector)
      .getOrElse(throw new IllegalArgumentException(s"Cannot get ${key} from ${doc}"))

  def getVectorOfDouble(doc: Document, key: String) =
    extractArray(doc.get(key)).map(_.toArray(new Array[BsonValue](0)).map(_.asDouble().getValue).toVector)
      .getOrElse(throw new IllegalArgumentException(s"Cannot get ${key} from ${doc}"))

  def getZonedDateTime(doc: Document, key: String) = ZonedDateTime.ofInstant(Instant.ofEpochMilli(
    MongoUtils.getEpochInMilliseconds(doc, key)), ZoneId.of("Asia/Shanghai"))

  def toBsonDateTime(zonedDateTime: ZonedDateTime) = BsonDateTime(zonedDateTime.toInstant.toEpochMilli)

}
