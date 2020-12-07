package scorpio.actor.trade

import java.time.ZonedDateTime
import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}

import scala.concurrent.duration._
import scorpio.actors.feed.PriceFeedSuperviseActor
import scorpio.actors.trade.PortfolioPricingActor
import scorpio.actors.trade.PortfolioPricingActor.{PricePortfolio, PricePortfolioFailure, PricePortfolioSuccess}
import scorpio.common.{ActorTestBase, UnitSpec}
import scorpio.marketdata.model.{SelectedStock, StockPrice, StockPriceObserve}
import scorpio.trade.model._

/**
  * Created by HONGBIN on 2017/1/31.
  */
class PortfolioPricingActorTest extends TestKit(ActorSystem("PortfolioPricingActorTest")) with UnitSpec with ActorTestBase {

  val observe1 = StockPriceObserve("var hq_str_sz000970=\"中科三环,12.820,12.780,13.230,13.280,12.780,13.230,13.240,17666905,232053959.280,15992,13.230,2000,13.220,29400,13.210,72700,13.200,41800,13.190,10800,13.240,95942,13.250,139600,13.260,141100,13.270,109600,13.280,2017-01-26,16:37:03,00\";")
  val observe2 = StockPriceObserve("var hq_str_sh600031=\"三一重工,6.780,6.770,6.880,6.890,6.730,6.860,6.870,26097709,177813418.000,18500,6.860,69100,6.850,68200,6.840,102558,6.830,125100,6.820,600,6.870,66700,6.880,1019701,6.890,1104060,6.900,213500,6.910,2017-01-26,15:00:00,00\";")

  lazy val probe = TestProbe()

  lazy val priceObserver = system.actorOf(Props[PriceFeedSuperviseActor], "priceFeedSuperviseActor")

  lazy val actor = system.actorOf(PortfolioPricingActor.props(priceObserver))

  override protected def init(): Unit = {
    StockPriceObserve.insertMany(Seq(observe1, observe2))
    SelectedStock.insert(SelectedStock("sz000970", "中科三环", true, ZonedDateTime.now))
    SelectedStock.insert(SelectedStock("sh600031", "三一重工", true, ZonedDateTime.now))
  }

  "PortfolioPricingActorTest" can "price portfolio" in {
    val position1 = StockPosition(
      code = "sz000970",
      name = "中科三环",
      quantity = 10000,
      cost = 10.5,
      payment = 105000
    )
    val position2 = StockPosition(
      code = "sh600031",
      name = "三一重工",
      quantity = 15000,
      cost = 7.025,
      payment = 105375
    )
    val portfolio = StockPortfolio(Vector(position1, position2))
    val cash = CashValue(101234.56, observe2.datetime.minusHours(2))
    val uuid = UUID.randomUUID()
    probe.send(actor, PricePortfolio(uuid, portfolio, cash))
    val reply = probe.receiveOne(5 seconds)
    val result = reply.asInstanceOf[PricePortfolioSuccess]
    assert(result.uuid == uuid)
    assert(result.value.stocks.size == 2)
    assert(result.value.stocks.contains(StockPositionValue(position1, StockPrice("sz000970", 13.23), 132300, 27300, observe1.datetime)))
    assert(result.value.stocks.contains(StockPositionValue(position2, StockPrice("sh600031", 6.88), 103200, -2175, observe2.datetime)))
    assert(result.value.cash == cash)
    assert(result.value.datetime == observe1.datetime)
  }

  it can "handle pricing time correctly" in {
    val position1 = StockPosition(
      code = "sz000970",
      name = "中科三环",
      quantity = 10000,
      cost = 10.5,
      payment = 105000
    )
    val position2 = StockPosition(
      code = "sh600031",
      name = "三一重工",
      quantity = 15000,
      cost = 7.025,
      payment = 105375
    )
    val portfolio = StockPortfolio(Vector(position1, position2))
    val cash = CashValue(101234.56, ZonedDateTime.now)
    val uuid = UUID.randomUUID()
    probe.send(actor, PricePortfolio(uuid, portfolio, cash))
    val reply = probe.receiveOne(5 seconds)
    val result = reply.asInstanceOf[PricePortfolioSuccess]
    assert(result.uuid == uuid)
    assert(result.value.stocks.size == 2)
    assert(result.value.stocks.contains(StockPositionValue(position1, StockPrice("sz000970", 13.23), 132300, 27300, observe1.datetime)))
    assert(result.value.stocks.contains(StockPositionValue(position2, StockPrice("sh600031", 6.88), 103200, -2175, observe2.datetime)))
    assert(result.value.cash == cash)
    assert(result.value.datetime == cash.datetime)
  }

  it can "overwrite pricing time" in {
    val position1 = StockPosition(
      code = "sz000970",
      name = "中科三环",
      quantity = 10000,
      cost = 10.5,
      payment = 105000
    )
    val position2 = StockPosition(
      code = "sh600031",
      name = "三一重工",
      quantity = 15000,
      cost = 7.025,
      payment = 105375
    )
    val portfolio = StockPortfolio(Vector(position1, position2))
    val cash = CashValue(101234.56, ZonedDateTime.now.minusMinutes(60))
    val dateTime = ZonedDateTime.now
    val uuid = UUID.randomUUID()
    probe.send(actor, PricePortfolio(uuid, portfolio, cash, Some(dateTime)))
    val reply = probe.receiveOne(5 seconds)
    val result = reply.asInstanceOf[PricePortfolioSuccess]
    assert(result.uuid == uuid)
    assert(result.value.stocks.size == 2)
    assert(result.value.stocks.contains(StockPositionValue(position1, StockPrice("sz000970", 13.23), 132300, 27300, observe1.datetime)))
    assert(result.value.stocks.contains(StockPositionValue(position2, StockPrice("sh600031", 6.88), 103200, -2175, observe2.datetime)))
    assert(result.value.cash == cash)
    assert(result.value.datetime == dateTime)
  }

  it can "handle empty position list" in {
    val portfolio = StockPortfolio(Vector.empty)
    val cash = CashValue(101234.56, observe2.datetime.minusHours(2))
    val uuid = UUID.randomUUID()
    probe.send(actor, PricePortfolio(uuid, portfolio, cash))
    val reply = probe.receiveOne(5 seconds)
    val result = reply.asInstanceOf[PricePortfolioSuccess]
    assert(result.uuid == uuid)
    assert(result.value.stocks.isEmpty)
    assert(result.value.cash == cash)
    assert(result.value.datetime == cash.datetime)
  }

  it should "reply cause of failure when pricing cannot be done" in {
    val position1 = StockPosition(
      code = "sz000970",
      name = "中科三环",
      quantity = 10000,
      cost = 10.5,
      payment = 105000
    )
    val position2 = StockPosition(
      code = "sh600362",
      name = "江西铜业",
      quantity = 5000,
      cost = 19.04,
      payment = 95200
    )
    val portfolio = StockPortfolio(Vector(position1, position2))
    val cash = CashValue(101234.56, ZonedDateTime.now)
    val uuid = UUID.randomUUID()
    probe.send(actor, PricePortfolio(uuid, portfolio, cash))
    val reply = probe.receiveOne(5 seconds)
    val result = reply.asInstanceOf[PricePortfolioFailure]
    assert(result.uuid == uuid)
    assert(result.message.contains(position2.code))
  }

  it can "persist pricing result into Mongo" in {
    val position1 = StockPosition(
      code = "sz000970",
      name = "中科三环",
      quantity = 10000,
      cost = 10.5,
      payment = 105000
    )
    val position2 = StockPosition(
      code = "sh600031",
      name = "三一重工",
      quantity = 15000,
      cost = 7.025,
      payment = 105375
    )
    val portfolio = StockPortfolio(Vector(position1, position2))
    val cash = CashValue(101234.56, observe2.datetime.minusHours(2))
    val uuid = UUID.randomUUID()
    probe.send(actor, PricePortfolio(uuid, portfolio, cash, None, persist = true))
    val reply = probe.receiveOne(5 seconds)
    val result = reply.asInstanceOf[PricePortfolioSuccess]
    assert(result.uuid == uuid)
    assert(result.value.stocks.size == 2)
    assert(result.value.stocks.contains(StockPositionValue(position1, StockPrice("sz000970", 13.23), 132300, 27300, observe1.datetime)))
    assert(result.value.stocks.contains(StockPositionValue(position2, StockPrice("sh600031", 6.88), 103200, -2175, observe2.datetime)))
    assert(result.value.cash == cash)
    assert(result.value.datetime == observe1.datetime)

    Thread.sleep(3000)

    val saved = PortfolioValue.findAll.head
    assert(saved.stocks.size == 2)
    assert(saved.stocks.contains(StockPositionValue(position1, StockPrice("sz000970", 13.23), 132300, 27300, observe1.datetime)))
    assert(saved.stocks.contains(StockPositionValue(position2, StockPrice("sh600031", 6.88), 103200, -2175, observe2.datetime)))
    assert(saved.cash == cash)
    assert(saved.datetime == observe1.datetime)
  }

  override protected def cleanup(): Unit = {
    StockPriceObserve.drop()
    SelectedStock.drop()
    PortfolioValue.drop()
  }

}
