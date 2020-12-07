package scorpio.actor.persistence

import java.time.ZonedDateTime
import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}

import scala.concurrent.duration._
import scorpio.actors.persistence.PortfolioUpdateActor
import scorpio.actors.persistence.PortfolioUpdateActor._
import scorpio.common.{ActorTestBase, UnitSpec}
import scorpio.trade.model.{StockPortfolio, StockPosition, StockTrade}

/**
  * Created by HONGBIN on 2017/1/30.
  */
class PortfolioUpdateActorTest extends TestKit(ActorSystem("PortfolioUpdateActorTest")) with UnitSpec with ActorTestBase {

  val probe = TestProbe()

  val actor = system.actorOf(Props[PortfolioUpdateActor])

  "PortfolioUpdateActor" can "create new portfolio from position" in {
    val position = StockPosition(
      code = "sh600787",
      name = "中储股份",
      quantity = 5000,
      cost = 9.36,
      payment = 46800
    )
    val uuid = UUID.randomUUID
    probe.send(actor, AddPosition(uuid, position, true))
    val reply = probe.receiveOne(5 seconds)
    assert(reply == PortfolioUpdateSuccess(uuid, StockPortfolio(Vector(position))))
    val portfolio = StockPortfolio.findAll.head
    assert(portfolio.positions.size == 1)
    assert(portfolio.positions.contains(position))
  }

  it can "add position to existing portfolio" in {
    val position = StockPosition(
      code = "sh600362",
      name = "江西铜业",
      quantity = 10000,
      cost = 27.5,
      payment = 275000
    )
    val uuid = UUID.randomUUID
    probe.send(actor, AddPosition(uuid, position, true))
    val reply = probe.receiveOne(5 seconds)
    assert(reply.asInstanceOf[PortfolioUpdateSuccess].uuid == uuid)
    assert(reply.asInstanceOf[PortfolioUpdateSuccess].portfolio.positions.size == 2)
    assert(reply.asInstanceOf[PortfolioUpdateSuccess].portfolio.positions.contains(position))
    val portfolio = StockPortfolio.findAll.head
    assert(portfolio.positions.size == 2)
    assert(portfolio.positions.contains(position))
  }

  it can "adjust positions according to trades" in {
    val trade1 = StockTrade(
      code = "sz002415",
      name = "海康威视",
      buy = true,
      quantity = 1000,
      price = 25,
      value = 25000,
      payment = 25025,
      dateTime = ZonedDateTime.now.minusMinutes(30)
    )
    val position = StockPosition(
      code = "sz002415",
      name = "海康威视",
      quantity = 1000,
      cost = 25.025,
      payment = 25025
    )
    val uuid = UUID.randomUUID
    probe.send(actor, UpdateByTrades(uuid, Seq(trade1), persist = true))
    val reply = probe.receiveOne(5 seconds)
    assert(reply.asInstanceOf[PortfolioUpdateSuccess].uuid == uuid)
    assert(reply.asInstanceOf[PortfolioUpdateSuccess].portfolio.positions.size == 3)
    assert(reply.asInstanceOf[PortfolioUpdateSuccess].portfolio.positions.contains(position))
    val portfolio = StockPortfolio.findAll.head
    assert(portfolio.positions.size == 3)
    assert(portfolio.positions.contains(position))
  }

  it can "handle emtpy trade list" in {
    val uuid = UUID.randomUUID
    probe.send(actor, UpdateByTrades(uuid, Nil, persist = true))
    val reply = probe.receiveOne(5 seconds).asInstanceOf[PortfolioUpdateSuccess]
    assert(reply.uuid == uuid)
    assert(reply.portfolio.positions.size == 3)
    val portfolio = StockPortfolio.findAll.head
    assert(portfolio.positions.size == 3)
  }

  it should "fail when add duplicated position" in {
    val position = StockPosition(
      code = "sh600787",
      name = "中储股份",
      quantity = 5000,
      cost = 9.36,
      payment = 46800
    )
    val uuid = UUID.randomUUID
    probe.send(actor, AddPosition(uuid, position, true))
    val reply = probe.receiveOne(5 seconds)
    assert(reply.isInstanceOf[PortfolioUpdateFail])
    assert(reply.asInstanceOf[PortfolioUpdateFail].uuid.equals(uuid))
    val portfolio = StockPortfolio.findAll.head
    assert(portfolio.positions.size == 3)
  }

  it can "remove position from portfolio" in {
    val uuid = UUID.randomUUID
    probe.send(actor, RemovePosition(uuid, "sz002415", true))
    val reply = probe.receiveOne(5 seconds)
    assert(reply.asInstanceOf[PortfolioUpdateSuccess].uuid == uuid)
    val portfolio = StockPortfolio.findAll.head
    assert(portfolio.positions.size == 2)
    assert(!portfolio.positions.exists(_.code == "sz002415"))
  }

  it should "swallow error when asked to remove non-existing position" in {
    val uuid = UUID.randomUUID
    probe.send(actor, RemovePosition(uuid, "sz00002", true))
    val reply = probe.receiveOne(5 seconds)
    assert(reply.asInstanceOf[PortfolioUpdateSuccess].uuid == uuid)
  }

  it can "clean positions according to trades" in {
    val trade1 = StockTrade(
      code = "sh600362",
      name = "江西铜业",
      buy = false,
      quantity = 10000,
      price = 30,
      value = 300000,
      payment = 299400,
      dateTime = ZonedDateTime.now.minusMinutes(25)
    )
    val trade2 = StockTrade(
      code = "sh600787",
      name = "中储股份",
      buy = false,
      quantity = 5000,
      price = 10,
      value = 50000,
      payment = 49900,
      dateTime = ZonedDateTime.now.minusMinutes(20)
    )
    val uuid = UUID.randomUUID
    probe.send(actor, UpdateByTrades(uuid, Seq(trade1, trade2), true))
    val reply = probe.receiveOne(5 seconds)
    assert(reply.asInstanceOf[PortfolioUpdateSuccess].uuid == uuid)
    assert(reply.asInstanceOf[PortfolioUpdateSuccess].portfolio.positions.isEmpty)
    val portfolio = StockPortfolio.findAll.head
    assert(portfolio.positions.isEmpty)
  }

  it should "swallow error when asked to remove position from empty portfolio" in {
    val uuid = UUID.randomUUID
    probe.send(actor, RemovePosition(uuid, "sz00002", true))
    val reply = probe.receiveOne(5 seconds)
    assert(reply.asInstanceOf[PortfolioUpdateSuccess].uuid == uuid)
    assert(reply.asInstanceOf[PortfolioUpdateSuccess].portfolio.positions.isEmpty)
    val portfolio = StockPortfolio.findAll.head
    assert(portfolio.positions.isEmpty)
  }

  it can "add position to portfolio with empty position" in {
    val position = StockPosition(
      code = "sh600362",
      name = "江西铜业",
      quantity = 10000,
      cost = 27.5,
      payment = 275000
    )
    val uuid = UUID.randomUUID
    probe.send(actor, AddPosition(uuid, position, true))
    val reply = probe.receiveOne(5 seconds)
    assert(reply == PortfolioUpdateSuccess(uuid, StockPortfolio(Vector(position))))
    val portfolio = StockPortfolio.findAll.head
    assert(portfolio.positions.head == position)
  }

  override protected def cleanup(): Unit = {
    StockPortfolio.drop()
  }

}
