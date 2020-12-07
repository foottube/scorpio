package scorpio.actor.feed

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import scala.concurrent.duration._
import scorpio.actors.RepeatActor.{Start, Stop}
import scorpio.actors.feed.StockPriceObserveActor
import scorpio.actors.feed.StockPriceObserveActor.{GetStockPrice, ReloadStockCodes}
import scorpio.common.{ActorTestBase, UnitSpec}
import scorpio.marketdata.model.StockPriceObserve

/**
  * Created by HONGBIN on 2017/1/8.
  */
class StockPriceObserveActorTest extends TestKit(ActorSystem("StockPriceObserveActorTest")) with UnitSpec with ActorTestBase {

  val codes = Seq("sh601398", "sz000002", "sz000895")
  val codes2 = Seq("sh601398", "sz000002", "sz000895", "sh600030")

  val observeActor = system.actorOf(StockPriceObserveActor.props("stockPriceObserveActorTimer", codes))

  "StockPriceObserveActor" can "observe stock price" in {
    StockPriceObserve.drop()
    observeActor ! Start
    Thread.sleep(15000)
    var all = StockPriceObserve.findAll
    assert(all.size == 3)
    assert(all.exists(_.name == "工商银行"))
    observeActor ! ReloadStockCodes(codes2)
    Thread.sleep(15000)
    all = StockPriceObserve.findAll
    assert(all.size == 4)
    assert(all.exists(_.name == "中信证券"))
    val probe = TestProbe()
    probe.send(observeActor, GetStockPrice(Seq("sz000002", "sh600030")))
    val observes = probe.receiveOne(10 seconds).asInstanceOf[Seq[StockPriceObserve]]
    assert(observes.size == 2)
    assert(observes.head.name == "万 科Ａ")
    observeActor ! Stop

  }

  override protected def cleanup(): Unit = {
    StockPriceObserve.drop()
  }

}
