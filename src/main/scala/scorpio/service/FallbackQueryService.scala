package scorpio.service

import scala.concurrent.ExecutionContext.Implicits.global
import org.mongodb.scala.model.Filters._
import scorpio.common.Log
import scorpio.dao.mongo.MongoOperation
import scorpio.dao.mongo.{MongoEntity, MongoOperation}

import scala.util.{Failure, Success}

/**
  * Created by HONGBIN on 2017/1/6.
  */
trait FallbackQueryService[K, V] extends Log {

  def query(key: K): Option[V]

  def add(key: K, value: V): Unit

  def fallback: Option[FallbackQueryService[K, V]]

  def get(key: K): Option[V] = {
    query(key) match {
      case Some(value) =>
        logger.debug(s"Found $key -> $value")
        Some(value)
      case None =>
        logger.debug(s"Cannot find result with $key, fallback to $fallback")
        fallback match {
          case Some(service) =>
            service.get(key) match {
              case Some(value) =>
                add(key, value)
                Some(value)
              case None => None
            }
          case None => None
        }

    }
  }
}

class LocalMapQueryService[K, V](override val fallback: Option[FallbackQueryService[K, V]] = None) extends FallbackQueryService[K, V] with Log {

  private var map: Map[K, V] = Map.empty

  override def query(key: K): Option[V] = map.get(key)

  override def add(key: K, value: V): Unit = {
    logger.debug(s"Add $key -> $value to local map")
    map = map + (key -> value)
  }

  def size = map.size

  def clean(): Unit = map = Map.empty

}

class MongoQueryService[K, V <: MongoEntity[V]](
                                                 keyAttrName: String,
                                                 op: MongoOperation[V],
                                                 override val fallback: Option[FallbackQueryService[K, V]] = None
                                               ) extends FallbackQueryService[K, V] with Log {

  override def query(key: K): Option[V] = op.findOne(equal(keyAttrName, key))

  override def add(key: K, value: V): Unit = op.insertAsync(value).onComplete {
    case Success(_) => logger.debug(s"Inserted $value into Mongo database")
    case Failure(throwable) => logger.warn(s"Failed to insert $value into Mongo database due to $throwable")
  }

}
