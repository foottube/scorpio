package scorpio.actor.service

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import scorpio.actors.http.StockObserveActor.GetStockObserve
import scorpio.common.{ActorTestBase, Log, UnitSpec}
import scorpio.marketdata.model.StockPriceObserve

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by HONGBIN on 2017/1/6.
  */
class ServiceActorTest extends TestKit(ActorSystem("ServiceActorTest")) with UnitSpec with ActorTestBase {

  "ServiceActor" can "provide stockObserverPool" in {
    val probe = TestProbe()
    val pool = Await.result(system.actorSelection("/user/stockObserverPool").resolveOne(3 seconds), Duration(5, TimeUnit.SECONDS))
    probe.send(pool, GetStockObserve("sh601398"))
    val observe = probe.receiveOne(10 seconds).asInstanceOf[StockPriceObserve]
    assert(observe.name == "工商银行")
  }

}
