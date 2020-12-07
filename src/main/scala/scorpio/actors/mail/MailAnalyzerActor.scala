package scorpio.actors.mail

import java.time.{ZoneId, ZonedDateTime}

import akka.actor._
import scorpio.common.Log
import scorpio.gateway.mail._
import scorpio.messaging._

/**
  * Created by HONGBIN on 2017/1/2.
  */
class MailAnalyzerActor extends Actor with Log with ErrorMailReply {

  val timeRegex = """(\d{2}):(\d{2})""".r

  override def receive = {
    case msg: MailMessage =>
      logger.info(s"Process MailMessage $msg")
      msg.subject match {
        case StockCommand.stockCmdRegex(action, code) =>
          action match {
            case "ADD" => context.system.eventStream.publish(StockCommand(StockAction.ADD, Some(code), Some(msg)))
            case "REM" => context.system.eventStream.publish(StockCommand(StockAction.REMOVE, Some(code), Some(msg)))
            case "LST" => context.system.eventStream.publish(StockCommand(StockAction.LIST, None, Some(msg)))
            case _ => handleError(s"Unknown stock command action $action", msg)
          }
        case TradeCommand.tradeCmdRegex(action, code, buyOrSell, price, quantity, time) =>
          val now = ZonedDateTime.now()
          val dateTime = time match {
            case timeRegex(hour, minute) => ZonedDateTime.of(now.getYear, now.getMonthValue, now.getDayOfMonth, hour.toInt, minute.toInt, 0, 0, ZoneId.of("Asia/Shanghai"))
            case _ => now
          }
          action match {
            case "ADD" =>
              context.system.eventStream.publish(TradeCommand(
                action = TradeAction.ADD,
                code = code,
                isBuy = if (buyOrSell == "BUY") true else false,
                price = price.toDouble,
                quantity = quantity.toInt,
                time = dateTime,
                message = Some(msg)
              ))
            case "REM" =>
              context.system.eventStream.publish(TradeCommand(
                action = TradeAction.REMOVE,
                code = code,
                isBuy = if (buyOrSell == "BUY") true else false,
                price = price.toDouble,
                quantity = quantity.toInt,
                time = dateTime,
                message = Some(msg)
              ))
            case _ => handleError(s"Unknown trade command action $action", msg)
          }
        case PositionCommand.positionCmdRegex(action, code, quantity, cost) =>
          action match {
            case "LST" => context.system.eventStream.publish(PositionCommand(PositionAction.LIST, None, None, None, Some(msg)))
            case "ADD" => context.system.eventStream.publish(PositionCommand(PositionAction.ADD, Some(code), Some(quantity.toInt), Some(cost.toDouble), Some(msg)))
            case "REM" => context.system.eventStream.publish(PositionCommand(PositionAction.REMOVE, Some(code), None, None, Some(msg)))
            case _ => handleError(s"Unknown position command action $action", msg)
          }
        case PriceFeedCommand.priceFeedCmdRegex(action) =>
          action match {
            case "START" => context.system.eventStream.publish(PriceFeedCommand(PriceFeedAction.START, Some(msg)))
            case "STOP" => context.system.eventStream.publish(PriceFeedCommand(PriceFeedAction.STOP, Some(msg)))
            case "RELOAD" =>context.system.eventStream.publish(PriceFeedCommand(PriceFeedAction.RELOAD, Some(msg)))
            case "STATUS" => context.system.eventStream.publish(PriceFeedCommand(PriceFeedAction.STATUS, Some(msg)))
            case _ => handleError(s"Unknown pricefeed command action $action", msg)
          }
        case CashCommand.cashCmdRegex(action, amount, time) =>
          action match {
            case "LST" => context.system.eventStream.publish(CashCommand(CashAction.LIST, None, None, Some(msg)))
            case "ADD" | "REM" =>
              val now = ZonedDateTime.now()
              val dateTime = time match {
                case timeRegex(hour, minute) => ZonedDateTime.of(now.getYear, now.getMonthValue, now.getDayOfMonth, hour.toInt, minute.toInt, 0, 0, ZoneId.of("Asia/Shanghai"))
                case _ => now
              }
              val command = if (action == "ADD") CashAction.ADD else CashAction.REMOVE
              context.system.eventStream.publish(CashCommand(command, Some(amount.toDouble), Some(dateTime), Some(msg)))
            case _ => handleError(s"Unknown pricefeed command action $action", msg)
          }
        case _ =>
          handleError(s"${msg.subject} doesn't match with any mail command", msg)
      }
    case unknown =>
      logger.warn(s"Received unknown message $unknown")
  }

}
