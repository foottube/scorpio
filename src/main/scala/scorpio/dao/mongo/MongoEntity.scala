package scorpio.dao.mongo

import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.conversions.Bson

import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{Failure, Success, Try}

/**
  * Created by HONGBIN on 2016/12/10.
  */
trait MongoEntity[T] {
  def toDocument: Document
}

trait MongoOperation[T <: MongoEntity[T]] {

  def collectionName: String
  def fromDocument(document: Document): T

  def insert(entity: T) = Mongo.insert(entity.toDocument, collectionName)
  def insertAsync(entity: T) = Mongo.insertAsync(entity.toDocument, collectionName)
  def update(filter: Bson, update: Bson) = Mongo.update(filter, update, collectionName)
  def updateAsync(filter: Bson, update: Bson) = Mongo.updateAsync(filter, update, collectionName)
  def upsert(entity: T) = Mongo.upsert(entity.toDocument, collectionName)
  def upsertAsync(entity: T) = Mongo.upsertAsync(entity.toDocument, collectionName)
  def upsert(filter: Bson, entity: T) = Mongo.upsert(filter, entity.toDocument, collectionName)
  def upsertAsync(filter: Bson, entity: T) = Mongo.upsertAsync(filter, entity.toDocument, collectionName)
  def insertMany(entities: Seq[T]) = Mongo.insertMany(entities.map(_.toDocument), collectionName)
  def insertManyAsync(entities: Seq[T]) = Mongo.insertManyAsync(entities.map(_.toDocument), collectionName)
  def findAll = Mongo.findAll(collectionName).map(fromDocument(_))
  def findAllAsync = Mongo.findAllAsync(collectionName).map { seq => seq.map(fromDocument(_)) }
  def find(query: Bson) = Mongo.find(query, collectionName).map(fromDocument(_))
  def findAsync(query: Bson) = Mongo.findAsync(query, collectionName).map { seq => seq.map(fromDocument(_)) }
  def findTopN(query: Bson, collectionName: String, orderBy: Bson, limit: Int) =
    Mongo.findTopN(query, collectionName, orderBy, limit).map(fromDocument(_))
  def findTopNAsync(query: Bson, collectionName: String, orderBy: Bson, limit: Int) =
    Mongo.findTopNAsync(query, collectionName, orderBy, limit).map { seq => seq.map(fromDocument(_))}
  def findTopN(collectionName: String, orderBy: Bson, limit: Int) =
    Mongo.findTopN(collectionName, orderBy, limit).map(fromDocument(_))
  def findTopNAsync(collectionName: String, orderBy: Bson, limit: Int) =
    Mongo.findTopNAsync(collectionName, orderBy, limit).map { seq => seq.map(fromDocument(_))}
  def findOne(query: Bson): Option[T] = Try[T](fromDocument(Mongo.findOne(query, collectionName))) match {
    case Success(entity) => Some(entity)
    case Failure(_: IllegalStateException) => None
    case Failure(exception) => throw exception
  }
  def findOneAsync(query: Bson) = Mongo.findOneAsync(query, collectionName).map(fromDocument(_))
  def findById(id: String) = Try[T](fromDocument(Mongo.findById(BsonObjectId(id), collectionName))) match {
    case Success(entity) => Some(entity)
    case Failure(_: IllegalStateException) => None
    case Failure(exception) => throw exception
  }
  def findByIdAsync(id: String) = Mongo.findByIdAsync(BsonObjectId(id), collectionName).map(fromDocument(_))
  def drop() = Mongo.drop(collectionName)
  def dropAsync() = Mongo.dropAsync(collectionName)
}
