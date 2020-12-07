package scorpio.marketdata.model

import java.time.{Instant, ZoneId, ZonedDateTime}

import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters._
import scorpio.dao.mongo.{MongoEntity, MongoOperation, MongoUtils}

/**
  * Created by HONGBIN on 2016/12/12.
  */

case class SelectedStock(
                          code: String,
                          name: String,
                          selected: Boolean,
                          whenSelected: ZonedDateTime,
                          whenInvalidated: Option[ZonedDateTime] = None) extends MongoEntity[SelectedStock] {

  override def toDocument = whenInvalidated match {
    case Some(datetime) => Document(
      "code" -> code,
      "name" -> name,
      "selected" -> selected,
      "whenSelected" -> MongoUtils.toBsonDateTime(whenSelected),
      "whenInvalidated" -> MongoUtils.toBsonDateTime(datetime)
    )
    case None => Document(
      "code" -> code,
      "name" -> name,
      "selected" -> selected,
      "whenSelected" -> MongoUtils.toBsonDateTime(whenSelected)
    )
  }

}

object SelectedStock extends MongoOperation[SelectedStock] {

  override val collectionName = "SelectedStock"

  override def fromDocument(document: Document) = SelectedStock(
    code = MongoUtils.getString(document, "code"),
    name = MongoUtils.getString(document, "name"),
    selected = MongoUtils.getBoolean(document, "selected"),
    whenSelected = ZonedDateTime.ofInstant(Instant.ofEpochMilli(
      MongoUtils.getEpochInMilliseconds(document, "whenSelected")), ZoneId.of("Asia/Shanghai")),
    whenInvalidated = MongoUtils.getEpochInMillisecondsOption(document, "whenInvalidated") match {
      case Some(epoch) => Some(ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.of("Asia/Shanghai")))
      case None => None
    }
  )

  def findSelected: Seq[SelectedStock] = find(equal("selected", true))
}
