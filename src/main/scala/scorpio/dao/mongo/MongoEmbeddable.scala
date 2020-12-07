package scorpio.dao.mongo

import org.bson.BsonValue

/**
  * Created by HONGBIN on 2017/1/14.
  */
trait MongoEmbeddable[T] {
  def toBson: BsonValue
}

trait MongoEmbeddableFactory[T <: MongoEmbeddable[T]] {
  def fromBson(bson: BsonValue): T
}
