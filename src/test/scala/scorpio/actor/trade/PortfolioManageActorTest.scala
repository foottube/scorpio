package scorpio.actor.trade

import java.time.ZonedDateTime

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import scorpio.actors.feed.PriceFeedSuperviseActor
import scorpio.actors.persistence.SelectedStockPersistActor
import scorpio.actors.trade.PortfolioManageActor
import scorpio.common.{ActorTestBase, UnitSpec}
import scorpio.marketdata.model.{SelectedStock, StockCodeName, StockPriceObserve}
import scorpio.messaging.{CashAction, CashCommand, PositionAction, PositionCommand}
import scorpio.service.StockNameQueryService
import scorpio.trade.model._

/**
  * Created by HONGBIN on 2017/2/1.
  */
class PortfolioManageActorTest extends TestKit(ActorSystem("PortfolioManageActorTest")) with UnitSpec with ActorTestBase {

  val probe = TestProbe()

  lazy val priceObserver = system.actorOf(Props[PriceFeedSuperviseActor], "priceFeedSuperviseActor")

  val actor = system.actorOf(PortfolioManageActor.props(priceObserver))

  system.actorOf(Props[SelectedStockPersistActor])

  "PortfolioManageActor" can "update cash position" in {
    Thread.sleep(1000)
    var time = ZonedDateTime.now.minusSeconds(10)
    system.eventStream.publish(CashCommand(CashAction.ADD, Some(12345.67), Some(time), None))
    Thread.sleep(3000)
    assert(CashValue.findLatestBefore(ZonedDateTime.now).get == CashValue(12345.67, time))
    time = ZonedDateTime.now
    system.eventStream.publish(CashCommand(CashAction.REMOVE, Some(200), Some(time), None))
    Thread.sleep(3000)
    assert(CashValue.findLatestBefore(ZonedDateTime.now).get == CashValue(12145.67, time))
  }

  it can "update stock position" in {
    val position1 = StockPosition(
      code = "sh600787",
      name = "中储股份",
      quantity = 5000,
      cost = 9.36,
      payment = 46800
    )
    val position2 = StockPosition(
      code = "sz000970",
      name = "中科三环",
      quantity = 10000,
      cost = 10.0,
      payment = 100000
    )
    system.eventStream.publish(PositionCommand(PositionAction.ADD, Some("sh600787"), Some(5000), Some(9.36), None))
    system.eventStream.publish(PositionCommand(PositionAction.ADD, Some("sz000970"), Some(10000), Some(10.0), None))
    Thread.sleep(3000)
    var portfolio = StockPortfolio.findAll.head
    assert(portfolio.positions.size == 2)
    assert(portfolio.positions.contains(position1))
    assert(portfolio.positions.contains(position2))

    system.eventStream.publish(PositionCommand(PositionAction.REMOVE, Some("sh600787"), None, None, None))
    Thread.sleep(3000)
    portfolio = StockPortfolio.findAll.head
    assert(portfolio.positions.size == 1)
    assert(portfolio.positions.contains(position2))

    val selectedStocks = SelectedStock.findSelected
    assert(selectedStocks.size == 2)
    assert(selectedStocks.exists(_.code == "sh600787"))
    assert(selectedStocks.exists(_.code == "sz000970"))
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
