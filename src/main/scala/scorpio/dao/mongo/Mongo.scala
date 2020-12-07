package scorpio.dao.mongo

import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory
import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.UpdateOptions
import org.mongodb.scala.{Document, MongoClient, MongoDatabase}
import scorpio.common.Log

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * This is the gateway to Mongo DB
  * Created by HONGBIN on 2016/12/11.
  */
object Mongo extends Log {

  val config = ConfigFactory.load()
  val dbServer = config.getString("mongo.server")
  val dbName = config.getString("mongo.database")
  val timeout = config.getInt("mongo.timeout")

  val client: MongoClient = MongoClient(s"mongodb://${dbServer}")

  val database: MongoDatabase = client.getDatabase(dbName)

  logger.info(s"Mongo client connects to ${dbServer}/${dbName}")

  def insert(document: Document, collectionName: String): Unit =
    Await.result(insertAsync(document, collectionName), Duration(timeout, TimeUnit.SECONDS))

  def insertAsync(document: Document, collectionName: String) =
    database.getCollection(collectionName).insertOne(document).head()

  def insertMany(documents: Seq[Document], collectionName: String): Unit =
    Await.result(insertManyAsync(documents, collectionName), Duration(timeout, TimeUnit.SECONDS))

  def insertManyAsync(documents: Seq[Document], collectionName: String) =
    database.getCollection(collectionName).insertMany(documents).toFuture()

  def update(filter: Bson, update: Bson, collectionName: String) = Await.result(updateAsync(filter, update, collectionName), Duration(timeout, TimeUnit.SECONDS))

  def updateAsync(filter: Bson, update: Bson, collectionName: String) = database.getCollection(collectionName).updateMany(filter, update).toFuture()

  def upsert(document: Document, collectionName: String) = Await.result(upsertAsync(document, collectionName), Duration(timeout, TimeUnit.SECONDS))

  def upsertAsync(document: Document, collectionName: String) = {
    if (document.contains("_id")) {
      val query = equal("_id", document.get("_id").getOrElse(throw new IllegalArgumentException(s"_id cannot be None in $document")))
      val option = new UpdateOptions
      option.upsert(true)
      database.getCollection(collectionName).replaceOne(query, document, option).head
    } else {
      insertAsync(document, collectionName)
    }
  }

  def upsert(filter: Bson, document: Document, collectionName: String) = Await.result(upsertAsync(filter, document, collectionName), Duration(timeout, TimeUnit.SECONDS))

  def upsertAsync(filter: Bson, document: Document, collectionName: String) = {
    val option = new UpdateOptions
    option.upsert(true)
    database.getCollection(collectionName).replaceOne(filter, document, option).head()
  }

  def findAll(collectionName: String): Seq[Document] = Await.result(findAllAsync(collectionName), Duration(timeout, TimeUnit.SECONDS))

  def findAllAsync(collectionName: String) = database.getCollection(collectionName).find().toFuture()

  def find(query: Bson, collectionName: String): Seq[Document] = Await.result(findAsync(query, collectionName), Duration(timeout, TimeUnit.SECONDS))

  def findAsync(query: Bson, collectionName: String) = database.getCollection(collectionName).find(query).toFuture()

  def findTopNAsync(query: Bson, collectionName: String, orderBy: Bson, limit: Int): Future[Seq[Document]] =
    database.getCollection(collectionName).find(query).sort(orderBy).limit(limit).toFuture()

  def findTopN(query: Bson, collectionName: String, orderBy: Bson, limit: Int): Seq[Document] =
    Await.result(findTopNAsync(query, collectionName, orderBy, limit), Duration(timeout, TimeUnit.SECONDS))

  def findTopNAsync(collectionName: String, orderBy: Bson, limit: Int): Future[Seq[Document]] =
    database.getCollection(collectionName).find().sort(orderBy).limit(limit).toFuture()

  def findTopN(collectionName: String, orderBy: Bson, limit: Int): Seq[Document] =
    Await.result(findTopNAsync(collectionName, orderBy, limit), Duration(timeout, TimeUnit.SECONDS))

  def findOne(query: Bson, collectionName: String): Document = Await.result(findOneAsync(query, collectionName), Duration(timeout, TimeUnit.SECONDS))

  def findOneAsync(query: Bson, collectionName: String) = database.getCollection(collectionName).find(query).head()

  def findById(id: BsonObjectId, collectionName: String): Document = Await.result(findByIdAsync(id, collectionName), Duration(timeout, TimeUnit.SECONDS))

  def findByIdAsync(id: BsonObjectId, collectionName: String) = database.getCollection(collectionName).find(equal("_id", id)).head()

  def drop(collectionName: String): Unit = Await.result(dropAsync(collectionName), Duration(timeout, TimeUnit.SECONDS))

  def dropAsync(collectionName: String) = database.getCollection(collectionName).drop().head()

}
