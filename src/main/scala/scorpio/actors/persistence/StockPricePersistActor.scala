package scorpio.actors.persistence

import akka.actor.Actor
import org.mongodb.scala.model.Filters._
import scorpio.common.Log
import scorpio.marketdata.model.StockPriceObserve

import scala.concurrent.Future

/**
  * Created by HONGBIN on 2017/1/8.
  */

case class PersistStockPriceObserve(observes: Seq[StockPriceObserve])

class StockPricePersistActor extends Actor with Log {

  override def receive = {
    case PersistStockPriceObserve(observes) =>
      logger.debug(s"Persist ${observes}")
      StockPriceObserve.upsertMany(observes)
  }

}
