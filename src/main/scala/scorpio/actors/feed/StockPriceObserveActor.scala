package scorpio.actors.feed

import java.time.ZonedDateTime

import akka.actor.{Props, Terminated}
import com.typesafe.config.ConfigFactory
import org.mongodb.scala.model.Filters._
import scorpio.actors.RepeatActor.RepeatActor
import scorpio.actors.feed.StockPriceObserveActor.{GetStockPrice, ReloadStockCodes}
import scorpio.actors.http.StockObserveActor.GetStockObserves
import scorpio.actors.persistence.{PersistStockPriceObserve, StockPricePersistActor}
import scorpio.common.Log
import scorpio.marketdata.model.StockPriceObserve

import scala.concurrent.duration._

/**
  * Created by HONGBIN on 2017/1/7.
  */

object StockPriceObserveActor {
  case class UpdateStockPrice(updates: Seq[StockPriceObserve])
  case class ReloadStockCodes(codes: Seq[String])
  case class GetStockPrice(codes: Seq[String])
  def props(timerName: String, targetStockCodes: Seq[String]) = Props(new StockPriceObserveActor(timerName, targetStockCodes))
}

class StockPriceObserveActor(override val timerName: String, var targetStockCodes: Seq[String]) extends RepeatActor with Log {

  val config = ConfigFactory.load()

  override val duration = config.getInt("feed.price.observe.interval") seconds

  val groupSize = config.getInt("feed.price.observe.groupSize")

  var lastObserve: Map[String, StockPriceObserve] = {
    val now = ZonedDateTime.now
    targetStockCodes.map { code =>
      code -> StockPriceObserve.findLatestBefore(equal("code", code), now, 1).headOption
    } collect {
      case (code, Some(observe)) => code -> observe
    } toMap
  }

  var persistActor = context.actorOf(Props[StockPricePersistActor], "stockPricePersistActor")

  context.watch(persistActor)

  override def poll() = {
    logger.info(s"Poll price for stocks: $targetStockCodes")
    targetStockCodes.grouped(groupSize) foreach { codes =>
      logger.info(s"Poll group $codes")
      context.actorSelection("/user/stockObserverPool") ! GetStockObserves(codes)
    }
  }

  def localReceive: Receive = {
    case observes: Seq[StockPriceObserve] =>
      val updates = observes.filter { observe =>
        if (lastObserve.get(observe.code).contains(observe)) {
          false
        } else {
          lastObserve += (observe.code -> observe)
          true
        }
      }
      if (updates.nonEmpty) {
        persistActor ! PersistStockPriceObserve(updates)
      }
    case ReloadStockCodes(codes) =>
      logger.info(s"Set targetStockCodes to $codes")
      targetStockCodes = codes
    case GetStockPrice(codes) =>
      val observes = codes.map(lastObserve.get(_)).collect({
        case Some(observe) => observe
      })
      sender() ! observes
    case Terminated(_) =>
      logger.warn("stockPricePersistActor terminated. Restart it.")
      persistActor = context.actorOf(Props[StockPricePersistActor], "stockPricePersistActor")
      context.watch(persistActor)
  }

  override def receive: Receive = localReceive orElse super.receive

}
