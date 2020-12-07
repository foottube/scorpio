package scorpio.actor.trade

import java.time.{Instant, ZoneId, ZonedDateTime}

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import scorpio.actors.mail.MailAnalyzerActor
import scorpio.actors.persistence.SelectedStockPersistActor
import scorpio.actors.trade.TradeCaptureActor
import scorpio.common.{ActorTestBase, UnitSpec}
import scorpio.gateway.mail.MailMessage
import scorpio.marketdata.model.{SelectedStock, StockCodeName}
import scorpio.service.StockNameQueryService
import scorpio.trade.calc.Calc
import scorpio.trade.model.StockTrade

/**
  * Created by HONGBIN on 2017/1/21.
  */
class TradeCaptureActorTest extends TestKit(ActorSystem("TradeCaptureActorTest")) with UnitSpec with ActorTestBase{

  implicit val commissionRate = {
    ConfigFactory.load().getDouble("trade.calc.commissionRate")
  }

  system.actorOf(Props[SelectedStockPersistActor])

  val message = "SCORPIO TRADE ADD sh600050 BUY 6.30 10000 14:23"
  val today = ZonedDateTime.now
  val dealTime = ZonedDateTime.of(today.getYear, today.getMonthValue, today.getDayOfMonth, 14, 23, 0, 0, ZoneId.of("Asia/Shanghai"))
  var trade = StockTrade(
    code = "sh600050",
    name = "中国联通",
    buy = true,
    quantity = 10000,
    price = 6.3,
    value = 6.3 * 10000,
    payment = Calc.round(6.3 * 10000 * (1 + commissionRate)),
    dateTime = dealTime
  )

  val analyzer = system.actorOf(Props[MailAnalyzerActor])
  val capture = system.actorOf(Props[TradeCaptureActor])

  "TradeCaptureActor" can "capture trade and persist in Mongo" in {
    Thread.sleep(1000)
    analyzer ! MailMessage("TEST", "foottube@126.com", Instant.now, message, "")
    Thread.sleep(5000)
    val persisted = StockTrade.findAll.head
    assert(persisted == trade)

    val selectedStocks = SelectedStock.findSelected
    assert(selectedStocks.size == 1)
    assert(selectedStocks.exists(_.code == "sh600050"))
  }

  override protected def cleanup(): Unit = {
    StockTrade.drop()
    SelectedStock.drop()
    // Clean up StockNameQueryService cache and mongo collection
    StockNameQueryService.cache.clean()
    StockCodeName.drop()
  }
}
