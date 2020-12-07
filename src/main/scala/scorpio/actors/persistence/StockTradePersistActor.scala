package scorpio.actors.persistence

import akka.actor.Actor
import scorpio.common.Log
import scorpio.trade.model.StockTrade

/**
  * Created by HONGBIN on 2017/1/21.
  */

object StockTradePersistActor {

  case class StockTradePersistSuccess(trade: StockTrade)

}

class StockTradePersistActor extends Actor with Log {
  import StockTradePersistActor._

  override def receive = {
    case trade: StockTrade =>
      StockTrade.insert(trade)
      logger.info(s"$trade persisted to Mongo")
      sender() ! StockTradePersistSuccess(trade)
  }

}
