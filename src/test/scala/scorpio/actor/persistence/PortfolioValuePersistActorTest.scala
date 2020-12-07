package scorpio.actor.persistence

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import scorpio.actors.persistence.PortfolioValuePersistActor
import scorpio.actors.persistence.PortfolioValuePersistActor.{PersistPortfolioValue, PersistPortfolioValueSuccess}
import scorpio.common.{ActorTestBase, UnitSpec}
import scorpio.marketdata.model.StockPrice
import scorpio.trade.model._

/**
  * Created by HONGBIN on 2017/2/1.
  */
class PortfolioValuePersistActorTest extends TestKit(ActorSystem("PortfolioValuePersistActorTest")) with UnitSpec with ActorTestBase {

  val probe = TestProbe()

  val actor = system.actorOf(Props[PortfolioValuePersistActor])

  "PortfolioValuePersistActor" can "persist PortFolioValue into Mongo" in {
    val portfolioValue = PortfolioValue(
      stocks = Vector(
        StockPositionValue(
          position = StockPosition(code = "sz000970", name = "中科三环", quantity = 10000, cost = 10.5, payment = 105000),
          price = StockPrice(code = "sz000970", price = 13.23),
          value = 132300,
          profit = 27300,
          datetime = ZonedDateTime.now.minusHours(2)
        ),
        StockPositionValue(
          position = StockPosition(code = "sh600031", name = "三一重工", quantity = 15000, cost = 7.025, payment = 105375),
          price = StockPrice(code = "sh600031", price = 6.88),
          value = 103200,
          profit = -2175,
          datetime = ZonedDateTime.now.minusHours(2)
        )
      ),
      cash = CashValue(amount = 0.0, datetime = ZonedDateTime.now.minusHours(3)),
      datetime = ZonedDateTime.now
    )
    val uuid = UUID.randomUUID()
    probe.send(actor, PersistPortfolioValue(uuid, portfolioValue))
    val response = probe.receiveOne(5 seconds)
    assert(response == PersistPortfolioValueSuccess(uuid))
    val saved = PortfolioValue.findAll.head
    assert(saved == portfolioValue)
  }

  override def cleanup(): Unit = {
    PortfolioValue.drop()
    StockPortfolio.drop()
  }

}
