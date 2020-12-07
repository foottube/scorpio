package scorpio.actors.persistence

import java.time.ZonedDateTime
import java.util.UUID

import akka.actor.Actor
import scorpio.common.Log
import scorpio.trade.calc.CashCalculate
import scorpio.trade.model.{CashValue, StockTrade}

/**
  * Created by HONGBIN on 2017/1/31.
  */
object CashUpdateActor {
  case class UpdateCash(uuid: UUID, delta: Double, datetime: ZonedDateTime, persist: Boolean)
  case class UpdateCashByTrades(uuid: UUID, trades: Seq[StockTrade], persist: Boolean)
  case class CashUpdateSuccess(uuid: UUID, cash: CashValue)
  case class CashUpdateFail(uuid: UUID, message: String)
}

class CashUpdateActor extends Actor with Log {
  import CashUpdateActor._

  override def receive = {
    case UpdateCash(uuid, delta, datetime, persist) =>
      val updated = CashValue.findLatestBefore(ZonedDateTime.now) match {
        case Some(cashValue) => cashValue.copy(amount = cashValue.amount + delta, datetime = datetime)
        case None => CashValue(delta, datetime)
      }
      if (persist) {
        CashValue.insert(updated)
        logger.info(s"Persisted $updated")
      }
      sender() ! CashUpdateSuccess(uuid, updated)
    case UpdateCashByTrades(uuid, trades, persist) =>
      import scorpio.common.ZonedDateTimeOrdering._
      val datetime = if (trades.isEmpty) ZonedDateTime.now else trades.map(_.dateTime).max
      val amount = CashValue.findLatestBefore(ZonedDateTime.now) match {
        case Some(cashValue) => cashValue.amount
        case None => 0
      }
      val updated = CashValue(CashCalculate.calcCash(amount, trades), datetime)
      if (persist) {
        CashValue.insert(updated)
        logger.info(s"Persisted $updated")
      }
      sender() ! CashUpdateSuccess(uuid, updated)
  }

}
