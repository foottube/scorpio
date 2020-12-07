package scorpio.actors.feed

import java.util.UUID

import akka.actor._
import akka.pattern._
import scorpio.actors.RepeatActor.{Start, Status, Stop}
import scorpio.actors.RequestTimeout
import scorpio.actors.feed.StockPriceObserveActor.{GetStockPrice, ReloadStockCodes}
import scorpio.common.Log
import scorpio.gateway.mail.MailGateway
import scorpio.marketdata.model.SelectedStock
import scorpio.messaging.{PriceFeedAction, PriceFeedCommand}

import scala.util.{Failure, Success}

/**
  * Created by HONGBIN on 2017/1/8.
  */
class PriceFeedSuperviseActor extends Actor with RequestTimeout with Log {

  context.system.eventStream.subscribe(self, classOf[PriceFeedCommand])
  var observeActor = createObserveActor()
  implicit val ec = context.system.dispatcher

  override def receive = {
    case PriceFeedCommand(PriceFeedAction.START, msg) =>
      observeActor ! Start
      msg.foreach(MailGateway.replyMessage(true, "PriceFeed Started.", _))
    case PriceFeedCommand(PriceFeedAction.STOP, msg) =>
      observeActor ! Stop
      msg.foreach(MailGateway.replyMessage(true, "PriceFeed Stopped.", _))
    case PriceFeedCommand(PriceFeedAction.RELOAD, msg) =>
      val codes = SelectedStock.findSelected.map(_.code)
      observeActor ! ReloadStockCodes(codes)
      msg.foreach(MailGateway.replyMessage(true, s"Selected stocks reloaded:\n${codes.mkString(",")}", _))
    case PriceFeedCommand(PriceFeedAction.STATUS, msg) =>
      observeActor.ask(Status) onComplete {
        case Success(state) =>
          msg.foreach(MailGateway.replyMessage(true, state.toString, _))
        case Failure(throwable) =>
          logger.warn("Failed to ask StockPriceObserver for status update.", throwable)
          msg.foreach(MailGateway.replyMessage(false, throwable.getMessage, _))
      }
    case msg: GetStockPrice =>
      observeActor forward msg
    case Terminated(_) =>
      logger.warn("stockPriceObserveActor was terminated. Restart it.")
      observeActor = createObserveActor()
  }

  private def createObserveActor(): ActorRef = {
    import StockPriceObserveActor._
    val codes = SelectedStock.findSelected.map(_.code)
    val timerName = "stockPriceObserveActor" + UUID.randomUUID()
    val actor = context.actorOf(props(timerName, codes))
    context.watch(actor)
  }

}
