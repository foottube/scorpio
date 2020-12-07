package scorpio.actor.http

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}

import scala.concurrent.duration._
import scorpio.actors.http.StockObserveActor
import scorpio.actors.http.StockObserveActor.{GetStockObserve, GetStockObserves}
import scorpio.common.{ActorTestBase, UnitSpec}
import scorpio.marketdata.model.StockPriceObserve

/**
  * Created by HONGBIN on 2017/1/6.
  */
class StockObserveActorTest extends TestKit(ActorSystem("StockObserveActorTest")) with UnitSpec with ActorTestBase {

  val actor = system.actorOf(Props[StockObserveActor], "stockObserveActor")

  val probe = TestProbe()

  "StockObserveActor" can "retrieve one StockPriceObserve" in {
    probe.send(actor, GetStockObserve("sh601398"))
    val observe = probe.receiveOne(10 seconds).asInstanceOf[StockPriceObserve]
    assert(observe.name == "工商银行")
  }

  it can "retrieve multiple StockPriceObserve objects" in {
    probe.send(actor, GetStockObserves(Seq("sz000002", "sh601398", "sz300017")))
    val observes = probe.receiveOne(10 seconds).asInstanceOf[Seq[StockPriceObserve]]
    assert(observes.size == 3)
    assert(observes.head.name == "万 科Ａ")
  }

}
