package scorpio.actor.trade

import java.time.{ZoneId, ZonedDateTime}

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestKit
import scorpio.actors.feed.PriceFeedSuperviseActor
import scorpio.actors.trade.PortfolioManageActor
import scorpio.common.{ActorTestBase, UnitSpec}
import scorpio.marketdata.model.{SelectedStock, StockCodeName, StockPrice, StockPriceObserve}
import scorpio.messaging.{ReportAction, ReportCommand}
import scorpio.service.StockNameQueryService
import scorpio.trade.model._

/**
  * Created by HONGBIN on 2017/2/2.
  */
class PortfolioManageActorEodReportTest extends TestKit(ActorSystem("PortfolioManageActorEodReportTest")) with UnitSpec with ActorTestBase {

  val observe1 = StockPriceObserve("var hq_str_sz000970=\"中科三环,12.820,12.780,13.230,13.280,12.780,13.230,13.240,17666905,232053959.280,15992,13.230,2000,13.220,29400,13.210,72700,13.200,41800,13.190,10800,13.240,95942,13.250,139600,13.260,141100,13.270,109600,13.280,2017-01-26,16:37:03,00\";")
  val observe2 = StockPriceObserve("var hq_str_sh600031=\"三一重工,6.780,6.770,6.880,6.890,6.730,6.860,6.870,26097709,177813418.000,18500,6.860,69100,6.850,68200,6.840,102558,6.830,125100,6.820,600,6.870,66700,6.880,1019701,6.890,1104060,6.900,213500,6.910,2017-01-26,15:00:00,00\";")
  val observe3 = StockPriceObserve("var hq_str_sh601398=\"工商银行,4.560,4.560,4.600,4.600,4.560,4.590,4.600,84502233,386971697.000,3699761,4.590,1096700,4.580,2041441,4.570,2162600,4.560,626900,4.550,8088720,4.600,4245697,4.610,2765964,4.620,1816547,4.630,1227395,4.640,2017-01-26,15:00:00,00\";")

  lazy val priceObserver = system.actorOf(Props[PriceFeedSuperviseActor], "priceFeedSuperviseActor")

  val maxTradeTime = ZonedDateTime.now.minusHours(1)

  override protected def init(): Unit = {
    val yesterday = ZonedDateTime.now.minusDays(1)
    StockPriceObserve.insertMany(Seq(observe1, observe2, observe3))
    SelectedStock.insert(SelectedStock("sz000970", "中科三环", true, yesterday))
    SelectedStock.insert(SelectedStock("sh600031", "三一重工", true, yesterday))
    SelectedStock.insert(SelectedStock("sh601398", "工商银行", true, yesterday))
    CashValue.insert(CashValue(120000.0, yesterday))
    StockPortfolio.insert(StockPortfolio(Vector(
      StockPosition("sz000970", "中科三环", 10000, 10.0, 100000),
      StockPosition("sh600031", "三一重工", 15000, 6.5, 97500)
    )))
    PortfolioValue.insert(PortfolioValue(
      stocks = Vector(
        StockPositionValue(
          StockPosition("sz000970", "中科三环", 10000, 10.0, 100000),
          StockPrice("sz000970", 12.78), 127800, 27800, yesterday),
        StockPositionValue(
          StockPosition("sh600031", "三一重工", 15000, 6.5, 97500),
          StockPrice("sh600031", 6.77), 101550, 4050, yesterday
        )
      ),
      cash = CashValue(120000.0, yesterday),
      datetime = yesterday
    ))
    StockTrade.insert(StockTrade("sh601398", "工商银行", true, 20000, 4.57, 91400, 91491.4, ZonedDateTime.now.minusHours(2)))
    StockTrade.insert(StockTrade("sh600031", "三一重工", false, 15000, 6.8, 102000, 101796, maxTradeTime))
  }

  "PortfolioManageActor" can "generate EOD report" in {
    system.actorOf(PortfolioManageActor.props(priceObserver))
    Thread.sleep(1000)
    system.eventStream.publish(ReportCommand(ReportAction.EOD, None, None))
    Thread.sleep(3000)
    assert(PortfolioValue.findAll.size == 2)
    assert(PortfolioValue.findAllAfter(ZonedDateTime.now.minusHours(6)).size == 1)
    val portfolioValue = PortfolioValue.findLatestBefore(ZonedDateTime.now).get
    assert(portfolioValue.datetime.isAfter(ZonedDateTime.now.minusSeconds(5)))
    assert(portfolioValue.stocks.size == 2)
    assert(portfolioValue.stocks.contains(StockPositionValue(
      StockPosition("sz000970", "中科三环", 10000, 10.0, 100000),
      StockPrice("sz000970", 13.23), 132300, 32300,
      ZonedDateTime.of(2017, 1, 26, 16, 37, 3, 0, ZoneId.of("Asia/Shanghai")))))
    assert(portfolioValue.stocks.contains(StockPositionValue(
      StockPosition("sh601398", "工商银行", 20000, 4.575, 91491.4),
      StockPrice("sh601398", 4.60), 92000, 508.6,
      ZonedDateTime.of(2017, 1, 26, 15, 0, 0, 0, ZoneId.of("Asia/Shanghai")))))
    assert(portfolioValue.cash.amount == 130304.6)

    assert(CashValue.findAll.size == 2)
    assert(CashValue.findLatestBefore(ZonedDateTime.now).get == CashValue(130304.6, maxTradeTime))

    val stockPortfolio = StockPortfolio.findAll.head
    assert(stockPortfolio.positions.size == 2)
    assert(stockPortfolio.positions.contains(StockPosition("sz000970", "中科三环", 10000, 10.0, 100000)))
    assert(stockPortfolio.positions.contains(StockPosition("sh601398", "工商银行", 20000, 4.575, 91491.4)))
  }

  override protected def cleanup(): Unit = {
    StockPriceObserve.drop()
    StockPortfolio.drop()
    SelectedStock.drop()
    PortfolioValue.drop()
    CashValue.drop()
    StockTrade.drop()
    StockNameQueryService.cache.clean()
    StockCodeName.drop()
  }


}
