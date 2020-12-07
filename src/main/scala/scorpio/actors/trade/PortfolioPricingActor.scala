package scorpio.actors.trade

import java.time.ZonedDateTime
import java.util.UUID

import akka.actor._
import akka.pattern._
import scorpio.actors.RequestTimeout
import scorpio.actors.feed.StockPriceObserveActor.GetStockPrice
import scorpio.actors.persistence.PortfolioValuePersistActor
import scorpio.actors.persistence.PortfolioValuePersistActor.{PersistPortfolioValue, PersistPortfolioValueSuccess}
import scorpio.common.Log
import scorpio.marketdata.model.StockPriceObserve
import scorpio.trade.calc.PortfolioPricingBase
import scorpio.trade.model.{CashValue, PortfolioValue, StockPortfolio}

import scala.collection.mutable.Map
import scala.util.{Failure, Success}

/**
  * Created by HONGBIN on 2017/1/31.
  */
object PortfolioPricingActor {
  case class PricePortfolio(uuid: UUID, portfolio: StockPortfolio, cash: CashValue, dateTime: Option[ZonedDateTime] = None, persist: Boolean = false)
  case class PricePortfolioSuccess(uuid: UUID, value: PortfolioValue)
  case class PricePortfolioFailure( uuid: UUID, message: String)

  def props(priceObserver: ActorRef) = Props(new PortfolioPricingActor(priceObserver))
}

class PortfolioPricingActor(priceFeedActor: ActorRef) extends Actor with PortfolioPricingBase with RequestTimeout with Log {
  import PortfolioPricingActor._
  implicit val ec = context.system.dispatcher

  val persistingPortfolios: Map[UUID, PortfolioValue] = Map.empty
  var persistActor = createPersistActor()

  override def receive = {
    case PricePortfolio(uuid, portfolio, cash, dateTime, persist) =>
      val client = sender
      pricePortfolio(portfolio, cash).onComplete {
        case Success(either) =>
          either match {
            case Left(message) =>
              logger.warn(s"Failed to price portfolio. $message")
              client ! PricePortfolioFailure(uuid, message)
            case Right(portfolioValue) =>
              val result = dateTime match {
                case Some(time) => portfolioValue.copy(datetime = time)
                case None => portfolioValue
              }
              logger.info(s"Portfolio price: $result")
              if (persist) {
                val uuid = UUID.randomUUID()
                persistingPortfolios += (uuid -> result)
                persistActor ! PersistPortfolioValue(uuid, result)
              }
              client ! PricePortfolioSuccess(uuid, result)
          }
        case Failure(throwable) =>
          logger.warn(s"Failed to price portfolio. ${throwable.getMessage}")
          client ! PricePortfolioFailure(uuid, throwable.getMessage)
      }
    case PersistPortfolioValueSuccess(uuid) =>
      persistingPortfolios -= uuid
    case Terminated(_) =>
      logger.warn("PortfolioValuePersistActor terminated. Restart it.")
      persistActor = createPersistActor()
      persistingPortfolios.foreach(keyValue => persistActor ! PersistPortfolioValue(keyValue._1, keyValue._2))
  }

  override def getStockPriceObserve(code: String) =
    priceFeedActor.ask(GetStockPrice(Seq(code))).mapTo[Seq[StockPriceObserve]].map {
      case head +: _ => Some(head)
      case Nil => None
    }

  private def createPersistActor() = {
    val actor = context.actorOf(Props[PortfolioValuePersistActor], "portfolioValuePersistActor")
    context.watch(actor)
    actor
  }

}
