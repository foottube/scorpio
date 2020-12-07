package scorpio.messaging

import java.time.ZonedDateTime

import scorpio.gateway.mail.MailMessage
import scorpio.messaging.CashAction.CashAction
import scorpio.messaging.PositionAction.PositionAction
import scorpio.messaging.PriceFeedAction.PriceFeedAction
import scorpio.messaging.ReportAction.ReportAction
import scorpio.messaging.StockAction.StockAction
import scorpio.messaging.TradeAction.TradeAction

/**
  * Created by HONGBIN on 2017/1/2.
  */
trait MailCommand {
  def message: Option[MailMessage]
}

object StockAction extends Enumeration {
  type StockAction = Value
  val ADD, REMOVE, LIST = Value
}

case class StockCommand(
                         action: StockAction,
                         target: Option[String],
                         override val message: Option[MailMessage]) extends MailCommand

object StockCommand {
  // See docs/mail-message-protocol for MailMessage format details
  // SCORPIO STOCK ADD sh600476
  // SCORPIO STOCK REM sh600476
  // SCORPIO STOCK LST
  val stockCmdRegex = """^(?:\w+)\s+STOCK\s+(\w+)(?:\s+([a-z]{2}\d+))?$""".r
}

object TradeAction extends Enumeration {
  type TradeAction = Value
  val ADD, REMOVE = Value
}

case class TradeCommand(
                         action: TradeAction,
                         code: String,
                         isBuy: Boolean,
                         price: Double,
                         quantity: Int,
                         time: ZonedDateTime,
                         override val message: Option[MailMessage]) extends MailCommand

object TradeCommand {
  // SCORPIO TRADE ADD sh600476 BUY 28.90 1000 14:23
  val tradeCmdRegex = """^(?:\w+)\s+TRADE\s+(\w+)\s+([a-z]{2}\d+)\s+(\w+)\s+([\d.]+)\s+(\d+)\s+([\d:]+)$""".r
}

object PositionAction extends Enumeration {
  type PositionAction = Value
  val LIST, ADD, REMOVE = Value
}

case class PositionCommand(
                            action: PositionAction,
                            code: Option[String],
                            quantity: Option[Int],
                            cost: Option[Double],
                            override val message: Option[MailMessage]) extends MailCommand

object PositionCommand {
  // SCORPIO POSITION LST
  // SCORPIO POSITION ADD sh600362 5000 20.15
  // SCORPIO POSITION REM sh600362
  val positionCmdRegex = """^(?:\w+)\s+POSITION\s+(\w+)(?:\s+([a-z]{2}\d+)(?:\s+(\d+)\s+([\d.]+))?)?$""".r
}

object PriceFeedAction extends Enumeration {
  type PriceFeedAction = Value
  val START, STOP, RELOAD, STATUS = Value
}

case class PriceFeedCommand(
                             action: PriceFeedAction,
                             override val message: Option[MailMessage]) extends MailCommand

object PriceFeedCommand {
  // SCORPIO PRICEFEED START
  // SCORPIO PRICEFEED STOP
  // SCORPIO PRICEFEED STATUS
  // SCORPIO PRICEFEED RELOAD
  val priceFeedCmdRegex = """^(?:\w+)\s+PRICEFEED\s+(\w+)$""".r
}

object CashAction extends Enumeration {
  type CashAction = Value
  val ADD, REMOVE, LIST = Value
}

case class CashCommand(
                        action: CashAction,
                        amount: Option[Double],
                        dateTime: Option[ZonedDateTime],
                        override val message: Option[MailMessage]) extends MailCommand

object CashCommand {
  // SCORPIO CASH ADD 100000 13:50
  // SCORPIO CASH REM 50000 14:35
  // SCORPIO CASH LST
  val cashCmdRegex = """^(?:\w+)\s+CASH\s+(\w+)(?:\s+([\d.]+)(?:\s+([\d:]+))?)?$""".r
}

object ReportAction extends Enumeration {
  type ReportAction = Value
  val EOD, PNL = Value
}

case class ReportCommand(
                        action: ReportAction,
                        dateTime: Option[ZonedDateTime],
                        override val message: Option[MailMessage]) extends MailCommand