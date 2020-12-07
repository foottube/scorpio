package scorpio.actor.mail

import java.time.{Instant, ZoneId, ZonedDateTime}

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import scorpio.actors.mail.MailAnalyzerActor
import scorpio.common.{ActorTestBase, UnitSpec}
import scorpio.gateway.mail.MailMessage
import scorpio.messaging._

/**
  * Created by HONGBIN on 2017/1/2.
  */
class MailAnalyzerActorTest extends TestKit(ActorSystem("MailAnalyzerActorTest")) with UnitSpec with ActorTestBase {

  "Mail command regexp" should "behave as expected" in {
    "SCORPIO TRADE ADD sh600476 BUY 28.90 1000 14:23" match {
      case TradeCommand.tradeCmdRegex(action, code, buyOrSell, price, quantity, time) =>
        assert(action == "ADD")
        assert(code == "sh600476")
        assert(buyOrSell == "BUY")
        assert(price == "28.90")
        assert(quantity == "1000")
        assert(time == "14:23")
      case _ => fail("Not match")
    }
    "SCORPIO STOCK REM sh600476" match {
      case StockCommand.stockCmdRegex(action, code) =>
        assert(action == "REM")
        assert(code == "sh600476")
      case _ => fail("Not match")
    }
    "SCORPIO STOCK LST" match {
      case StockCommand.stockCmdRegex(action, code) =>
        assert(action == "LST")
        assert(code == null)
      case _ => fail("Not match")
    }
    "SCORPIO PRICEFEED START" match {
      case PriceFeedCommand.priceFeedCmdRegex(action) => assert(action == "START")
      case _ => fail("Not match")
    }
    "SCORPIO POSITION LST" match {
      case PositionCommand.positionCmdRegex(action, _, _, _) => assert(action == "LST")
      case _ => fail("Not match")
    }
    "SCORPIO POSITION ADD sh600362 5000 20.15" match {
      case PositionCommand.positionCmdRegex(action, code, quantity, cost) =>
        assert(action == "ADD")
        assert(code == "sh600362")
        assert(quantity == "5000")
        assert(cost == "20.15")
      case _ => fail("Not match")
    }
    "SCORPIO POSITION REM sh600362" match {
      case PositionCommand.positionCmdRegex(action, code, _, _) =>
        assert(action == "REM")
        assert(code == "sh600362")
      case _ => fail("Not match")
    }
    "SCORPIO CASH LST" match {
      case CashCommand.cashCmdRegex(action, _, _) =>
        assert(action == "LST")
      case _ => fail("Not match")
    }
    "SCORPIO CASH ADD 1234.56" match {
      case CashCommand.cashCmdRegex(action, amount, _) =>
        assert(action == "ADD")
        assert(amount == "1234.56")
      case _ => fail("Not match")
    }
    "SCORPIO CASH REM 101.11 12:56" match {
      case CashCommand.cashCmdRegex(action, amount, time) =>
        assert(action == "REM")
        assert(amount == "101.11")
        assert(time == "12:56")
      case _ => fail("Not match")
    }
  }

  val probe = TestProbe()
  val analyzer = system.actorOf(Props[MailAnalyzerActor])

  "MailAnalyzerActor" can "analyze stock command" in {
    system.eventStream.subscribe(probe.ref, classOf[StockCommand])
    val msg = MailMessage("TEST", "foottube@126.com", Instant.now, "SCORPIO STOCK ADD sh600476", "This is a test")
    analyzer ! msg
    probe.expectMsg(StockCommand(StockAction.ADD, Some("sh600476"), Some(msg)))
  }

  it can "analyze trade command" in {
    system.eventStream.subscribe(probe.ref, classOf[TradeCommand])
    val msg = MailMessage("TEST", "foottube@126.com", Instant.now, "SCORPIO TRADE ADD sh600476 BUY 128.05 5000 14:00", "This is a test")
    analyzer ! msg
    val now = ZonedDateTime.now()
    probe.expectMsg(TradeCommand(
      action = TradeAction.ADD,
      code = "sh600476",
      isBuy = true,
      price = 128.05,
      quantity = 5000,
      time = ZonedDateTime.of(now.getYear, now.getMonthValue, now.getDayOfMonth, 14, 0, 0, 0, ZoneId.of("Asia/Shanghai")),
      Some(msg)
    ))
  }

  it can "analyze position command" in {
    system.eventStream.subscribe(probe.ref, classOf[PositionCommand])
    var msg = MailMessage("", "", Instant.now, "SCORPIO POSITION LST", "")
    analyzer ! msg
    probe.expectMsg(PositionCommand(PositionAction.LIST, None, None, None, Some(msg)))
    msg = MailMessage("", "", Instant.now, "SCORPIO POSITION ADD sz000002 12500 24.35", "")
    analyzer ! msg
    probe.expectMsg(PositionCommand(PositionAction.ADD, Some("sz000002"), Some(12500), Some(24.35), Some(msg)))
    msg = MailMessage("", "", Instant.now, "SCORPIO POSITION REM sh600031", "")
    analyzer ! msg
    probe.expectMsg(PositionCommand(PositionAction.REMOVE, Some("sh600031"), None, None, Some(msg)))
  }

  it can "analyze pricefeed command" in {
    system.eventStream.subscribe(probe.ref, classOf[PriceFeedCommand])
    val msg = MailMessage("", "", Instant.now, "SCORPIO PRICEFEED STOP", "")
    analyzer ! msg
    probe.expectMsg(PriceFeedCommand(PriceFeedAction.STOP, Some(msg)))
  }

  it can "analyze cash command" in {
    system.eventStream.subscribe(probe.ref, classOf[CashCommand])
    var msg = MailMessage("", "", Instant.now, "SCORPIO CASH LST", "")
    analyzer ! msg
    probe.expectMsg(CashCommand(CashAction.LIST, None, None, Some(msg)))
    msg = MailMessage("", "", Instant.now, "SCORPIO CASH ADD 100000 13:50", "")
    val now = ZonedDateTime.now
    analyzer ! msg
    probe.expectMsg(CashCommand(CashAction.ADD, Some(100000),
      Some(ZonedDateTime.of(now.getYear, now.getMonthValue, now.getDayOfMonth, 13, 50, 0, 0, ZoneId.of("Asia/Shanghai"))), Some(msg)))
    msg = MailMessage("", "", Instant.now, "SCORPIO CASH REM 50000 14:35", "")
    analyzer ! msg
    probe.expectMsg(CashCommand(CashAction.REMOVE, Some(50000),
      Some(ZonedDateTime.of(now.getYear, now.getMonthValue, now.getDayOfMonth, 14, 35, 0, 0, ZoneId.of("Asia/Shanghai"))), Some(msg)))
  }

}
