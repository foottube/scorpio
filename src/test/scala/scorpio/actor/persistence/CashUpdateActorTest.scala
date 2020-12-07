package scorpio.actor.persistence

import java.time.ZonedDateTime
import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import scorpio.actors.persistence.CashUpdateActor
import scorpio.actors.persistence.CashUpdateActor.{CashUpdateSuccess, UpdateCash, UpdateCashByTrades}
import scorpio.common.{ActorTestBase, UnitSpec}
import scorpio.trade.model.{CashValue, StockTrade}

import scala.concurrent.duration._
/**
  * Created by HONGBIN on 2017/1/31.
  */
class CashUpdateActorTest extends TestKit(ActorSystem("CashUpdateActorTest")) with UnitSpec with ActorTestBase {

  val probe = TestProbe()

  val actor = system.actorOf(Props[CashUpdateActor])

  "CashUpdateActor" can "create CashValue object in Mongo" in {
    val uuid = UUID.randomUUID()
    val time = ZonedDateTime.now.minusMinutes(60)
    probe.send(actor, UpdateCash(uuid, 100000.0, time, persist = true))
    val reply = probe.receiveOne(5 seconds)
    assert(reply == CashUpdateSuccess(uuid, CashValue(100000.0, time)))
    assert(CashValue.findAll.size == 1)
    assert(CashValue.findAll.head == CashValue(100000.0, time))
  }

  it can "adjust CashValue by cash" in {
    val uuid = UUID.randomUUID()
    val time = ZonedDateTime.now.minusMinutes(40)
    probe.send(actor, UpdateCash(uuid, -36000.0, time, persist = true))
    val reply = probe.receiveOne(5 seconds)
    assert(reply == CashUpdateSuccess(uuid, CashValue(64000.0, time)))
    assert(CashValue.findAll.size == 2)
    assert(CashValue.findAll.contains(CashValue(64000.0, time)))
    assert(CashValue.findLatestBefore(ZonedDateTime.now).get == CashValue(64000.0, time))
  }

  it can "adjust CashValue by trades" in {
    val trade1 = StockTrade(
      code = "sz002415",
      name = "海康威视",
      quantity = 3000,
      buy = false,
      price = 26,
      value = 78000,
      payment = 77844,
      dateTime = ZonedDateTime.now.minusMinutes(30)
    )

    val trade2 = StockTrade(
      code = "sz000338",
      name = "潍柴动力",
      quantity = 10000,
      buy = true,
      price = 11.11,
      value = 111100,
      payment = 111211.1,
      dateTime = ZonedDateTime.now.minusMinutes(20)
    )

    val uuid = UUID.randomUUID()
    probe.send(actor, UpdateCashByTrades(uuid, Seq(trade1, trade2), persist = true))
    val reply = probe.receiveOne(5 seconds)
    assert(reply == CashUpdateSuccess(uuid, CashValue(30632.9, trade2.dateTime)))
    assert(CashValue.findAll.size == 3)
    assert(CashValue.findAll.contains(CashValue(30632.9, trade2.dateTime)))
    assert(CashValue.findLatestBefore(ZonedDateTime.now).get == CashValue(30632.9, trade2.dateTime))
  }

  it can "handle empty trade list" in {
    val cash = CashValue.findLatestBefore(ZonedDateTime.now).get
    val uuid = UUID.randomUUID()
    probe.send(actor, UpdateCashByTrades(uuid, Nil, persist = true))
    val reply = probe.receiveOne(5 seconds).asInstanceOf[CashUpdateSuccess]
    assert(reply.cash.amount == cash.amount)
    assert(CashValue.findAll.size == 4)
    assert(CashValue.findAll.contains(reply.cash))
    assert(CashValue.findLatestBefore(ZonedDateTime.now).get == reply.cash)
  }

  override protected def cleanup(): Unit = {
    CashValue.drop()
  }

}
