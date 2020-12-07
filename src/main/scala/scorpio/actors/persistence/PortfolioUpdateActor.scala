package scorpio.actors.persistence

import java.util.UUID

import akka.actor.Actor
import scorpio.common.Log
import scorpio.trade.calc.FixedCommissionPortfolioCalculate
import scorpio.trade.model.{StockPortfolio, StockPosition, StockTrade}

/**
  * Created by HONGBIN on 2017/1/30.
  */

object PortfolioUpdateActor {
  case class AddPosition(uuid: UUID, position: StockPosition, persist: Boolean)
  case class RemovePosition(uuid: UUID, code: String, persist: Boolean)
  case class UpdateByTrades(uuid: UUID, trades: Seq[StockTrade], persist: Boolean)
  case class PortfolioUpdateSuccess(uuid: UUID, portfolio: StockPortfolio)
  case class PortfolioUpdateFail(uuid: UUID, message: String)
}

class PortfolioUpdateActor extends Actor with Log {
  import PortfolioUpdateActor._

  override def receive = {
    case AddPosition(uuid, position, persist) =>
      StockPortfolio.findAll.headOption match {
        case Some(exist) =>
          if (exist.positions.exists(_.code == position.code)) {
            val message = s"Position with ${position.code} already exists. Please use trade update instead of position update."
            logger.warn(s"AddPosition failed. $message")
            sender() ! PortfolioUpdateFail(uuid, message)
          } else {
            val updated = exist.copy(positions = exist.positions :+ position)
            if (persist) {
              StockPortfolio.upsert(updated)
              logger.info(s"Persisted $updated")
            }
            sender() ! PortfolioUpdateSuccess(uuid, updated)
          }
        case None =>
          val portfolio = StockPortfolio(Vector(position))
          if (persist) {
            StockPortfolio.upsert(portfolio)
            logger.info(s"Persisted $portfolio")
          }
          sender() ! PortfolioUpdateSuccess(uuid, portfolio)
      }
    case RemovePosition(uuid, code, persist) =>
      StockPortfolio.findAll.headOption match {
        case Some(exist) =>
          val updated = exist.copy(positions = exist.positions.filter(_.code != code))
          if (persist) {
            StockPortfolio.upsert(updated)
            logger.info(s"Persisted $updated")
          }
          sender() ! PortfolioUpdateSuccess(uuid, updated)
        case None =>
          val message = "StockPortfolio doesn't exist in Mongo."
          logger.warn(s"RemovePosition failed. $message")
          sender() ! PortfolioUpdateFail(uuid, message)
      }
    case UpdateByTrades(uuid, trades, persist) =>
      val portfolio = FixedCommissionPortfolioCalculate.calcPortfolio(
        previousPortfolio = StockPortfolio.findAll.headOption,
        trades = trades
      )
      if (persist) {
        StockPortfolio.upsert(portfolio)
        logger.info(s"Persisted $portfolio")
      }
      sender() ! PortfolioUpdateSuccess(uuid, portfolio)
  }

}
