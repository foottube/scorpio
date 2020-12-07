package scorpio.dao.mongo

import java.time.ZonedDateTime

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Sorts._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by HONGBIN on 2017/1/15.
  */
trait TimedMongoOperation[T <: MongoEntity[T]] extends MongoOperation[T]{

  def dateTimeField: String

  def findLatestBefore(datetime: ZonedDateTime, limit: Int): Seq[T] =
    findTopN(lte(dateTimeField, MongoUtils.toBsonDateTime(datetime)), collectionName, orderBy(descending(dateTimeField)), limit)

  def findLatestBeforeAsync(datetime: ZonedDateTime, limit: Int): Future[Seq[T]] =
    findTopNAsync(lte(dateTimeField, MongoUtils.toBsonDateTime(datetime)), collectionName, orderBy(descending(dateTimeField)), limit)

  def findLatestBefore(datetime: ZonedDateTime): Option[T] =
    findLatestBefore(datetime: ZonedDateTime, 1) match {
      case head +: _ => Some(head)
      case Nil => None
    }

  def findLatestBeforeAsync(datetime: ZonedDateTime): Future[Option[T]] =
    findLatestBeforeAsync(datetime: ZonedDateTime, 1).map {
      case head +: _ => Some(head)
      case Nil => None
    }

  def findLatestBefore(filter: Bson, datetime: ZonedDateTime, limit: Int): Seq[T] =
    findTopN(and(filter, lte(dateTimeField, MongoUtils.toBsonDateTime(datetime))), collectionName, orderBy(descending(dateTimeField)), limit)

  def findLatestBeforeAsync(filter: Bson, datetime: ZonedDateTime, limit: Int): Future[Seq[T]] =
    findTopNAsync(and(filter, lte(dateTimeField, MongoUtils.toBsonDateTime(datetime))), collectionName, orderBy(descending(dateTimeField)), limit)

  def findAllBefore(datetime: ZonedDateTime): Seq[T] = find(lte(dateTimeField, MongoUtils.toBsonDateTime(datetime)))

  def findAllBeforeAsync(datetime: ZonedDateTime): Future[Seq[T]] = findAsync(lte(dateTimeField, MongoUtils.toBsonDateTime(datetime)))

  def findBefore(filter: Bson, datetime: ZonedDateTime): Seq[T] = find(and(filter, lte(dateTimeField, MongoUtils.toBsonDateTime(datetime))))

  def findBeforeAsync(filter: Bson, datetime: ZonedDateTime): Future[Seq[T]] = findAsync(and(filter, lte(dateTimeField, MongoUtils.toBsonDateTime(datetime))))

  def findAllAfter(datetime: ZonedDateTime): Seq[T] = find(gte(dateTimeField, MongoUtils.toBsonDateTime(datetime)))

  def findAllAfterAsync(datetime: ZonedDateTime): Future[Seq[T]] = findAsync(gte(dateTimeField, MongoUtils.toBsonDateTime(datetime)))

  def findAfter(filter: Bson, datetime: ZonedDateTime): Seq[T] = find(and(filter, gte(dateTimeField, MongoUtils.toBsonDateTime(datetime))))

  def findAfterAsync(filter: Bson, datetime: ZonedDateTime): Future[Seq[T]] = findAsync(and(filter, gte(dateTimeField, MongoUtils.toBsonDateTime(datetime))))

}
