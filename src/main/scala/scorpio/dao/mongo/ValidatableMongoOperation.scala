package scorpio.dao.mongo

import scala.concurrent.ExecutionContext.Implicits.global
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._

import scala.util.{Failure, Success, Try}

/**
  * Created by HONGBIN on 2017/1/30.
  */
trait ValidatableMongoOperation[T <: MongoEntity[T]] extends MongoOperation[T] {

  def validField: String

  override def findAll = Mongo.find(equal(validField, true), collectionName).map(fromDocument)
  override def findAllAsync = Mongo.findAsync(equal(validField, true), collectionName).map { seq => seq.map(fromDocument) }
  override def find(query: Bson) = Mongo.find(and(equal(validField, true), query), collectionName).map(fromDocument)
  override def findAsync(query: Bson) = Mongo.findAsync(and(equal(validField, true), query), collectionName).map { seq => seq.map(fromDocument) }
  override def findTopN(query: Bson, collectionName: String, orderBy: Bson, limit: Int) =
    Mongo.findTopN(and(equal(validField, true), query), collectionName, orderBy, limit).map(fromDocument)
  override def findTopNAsync(query: Bson, collectionName: String, orderBy: Bson, limit: Int) =
    Mongo.findTopNAsync(and(equal(validField, true), query), collectionName, orderBy, limit).map { seq => seq.map(fromDocument)}
  override def findTopN(collectionName: String, orderBy: Bson, limit: Int) =
    Mongo.findTopN(equal(validField, true), collectionName, orderBy, limit).map(fromDocument)
  override def findTopNAsync(collectionName: String, orderBy: Bson, limit: Int) =
    Mongo.findTopNAsync(equal(validField, true), collectionName, orderBy, limit).map { seq => seq.map(fromDocument)}
  override def findOne(query: Bson): Option[T] = Try[T](fromDocument(Mongo.findOne(and(equal(validField, true), query), collectionName))) match {
    case Success(entity) => Some(entity)
    case Failure(_: IllegalStateException) => None
    case Failure(exception) => throw exception
  }
  override def findOneAsync(query: Bson) = Mongo.findOneAsync(and(equal(validField, true), query), collectionName).map(fromDocument)

  def invalidate(filter: Bson) = Mongo.update(filter, set(validField, false), collectionName)
  def invalidateAsync(filter: Bson) = Mongo.updateAsync(filter, set(validField, false), collectionName)

}
