package scorpio.actors.trade

import akka.actor.{Actor, ActorRef, Props, Terminated}
import scorpio.actors.persistence.StockTradePersistActor
import scorpio.actors.persistence.StockTradePersistActor.StockTradePersistSuccess
import scorpio.common.Log
import scorpio.gateway.mail.{MailGateway, MailMessage}
import scorpio.messaging.{StockAction, StockCommand, TradeAction, TradeCommand}
import scorpio.service.StockNameQueryService
import scorpio.trade.calc.FixedCommissionTradeCalculate
import scorpio.trade.model.StockTrade

/**
  * Created by HONGBIN on 2017/1/21.
  */
class TradeCaptureActor extends Actor with Log {
  import scala.collection.mutable._

  val persistingTrades: Map[StockTrade, Option[MailMessage]] = Map.empty

  var persistActor: ActorRef = createPersistActor()

  private def createPersistActor(): ActorRef = {
    val actor = context.actorOf(Props[StockTradePersistActor], "stockTradePersistActor")
    context.watch(actor)
  }

  override def preStart(): Unit = {
    super.preStart()
    context.system.eventStream.subscribe(self, classOf[TradeCommand])
  }

  override def receive = {
    case Terminated(_) =>
      logger.warn("StockTradePersistActor terminated. Restart it.")
      persistActor = createPersistActor()
      persistingTrades.foreach(persistActor ! _)
    case TradeCommand(TradeAction.ADD, code, isBuy, price, quantity, time, message) =>
      val name = StockNameQueryService.get(code) match {
        case Some(stockCodeName) => stockCodeName.name
        case None =>
          logger.error(s"Cannot find name for $code")
          ""
      }
      // Add code to SelectedStock
      context.system.eventStream.publish(StockCommand(StockAction.ADD, Some(code), None))

      val calc = FixedCommissionTradeCalculate(price, quantity, isBuy)
      val trade = StockTrade(
        code = code,
        name = name,
        buy = isBuy,
        quantity = quantity,
        price = price,
        value = calc.marketValue,
        payment = calc.payment,
        dateTime = time)
      persistingTrades += (trade -> message)
      logger.info(s"StockTrade $trade captured.")
      persistActor ! trade
    case StockTradePersistSuccess(trade) =>
      persistingTrades.get(trade) match {
        case Some(message) =>
          message.foreach(MailGateway.replyMessage(true, "Trade captured successfully", _))
          persistingTrades -= trade
        case None => logger.error(s"Cannot find message for persisted trade $trade")
      }
  }

}
