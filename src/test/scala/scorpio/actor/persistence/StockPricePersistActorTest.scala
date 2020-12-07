package scorpio.actor.persistence

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestKit
import scorpio.actors.persistence.{PersistStockPriceObserve, StockPricePersistActor}
import scorpio.common.{ActorTestBase, UnitSpec}
import scorpio.marketdata.model.StockPriceObserve

/**
  * Created by HONGBIN on 2017/1/8.
  */
class StockPricePersistActorTest extends TestKit(ActorSystem("StockPricePersistActorTest")) with UnitSpec with ActorTestBase {

  val data = "var hq_str_sh600485=\"信威集团,16.870,16.900,16.890,17.030,16.820,16.890,16.900,24563063,415484916.000,2900,16.890,32000,16.880,57500,16.870,61800,16.860,81100,16.850,66205,16.900,58200,16.910,53400,16.920,41700,16.930,49200,16.940,2016-12-09,14:00:00,00\";"
  val data2 = "var hq_str_sh600485=\"信威集团,16.870,16.900,16.890,17.530,16.820,16.890,16.900,24563063,415484916.000,2900,16.890,32000,16.880,57500,16.870,61800,16.860,81100,16.850,66205,16.900,58200,16.910,53400,16.920,41700,16.930,49200,16.940,2016-12-09,14:05:00,00\";"
  val data3 = "var hq_str_sh600485=\"信威集团,16.870,16.900,16.890,18.030,16.820,16.890,16.900,24563063,415484916.000,2900,16.890,32000,16.880,57500,16.870,61800,16.860,81100,16.850,66205,16.900,58200,16.910,53400,16.920,41700,16.930,49200,16.940,2016-12-09,14:10:00,00\";"
  val data4 = "var hq_str_sh601857=\"中国石油,8.170,8.160,8.360,8.420,8.150,8.350,8.360,116201023,968663810.000,242032,8.350,97001,8.340,105600,8.330,238300,8.320,157700,8.310,143600,8.360,384831,8.370,266400,8.380,370800,8.390,1134980,8.400,2017-01-06,15:00:00,00\";"

  val observes = Seq(
    StockPriceObserve(data),
    StockPriceObserve(data2),
    StockPriceObserve(data3),
    StockPriceObserve(data4)
  )

  "StockPricePersistActor" can "persist StockPriceObserves" in {
    StockPriceObserve.drop()
    val actor = system.actorOf(Props[StockPricePersistActor])
    actor ! PersistStockPriceObserve(observes)
    Thread.sleep(10000)
    val all = StockPriceObserve.findAll
    assert(all.size == 4)
    assert(all.contains(StockPriceObserve(data3)))
  }

  override protected def cleanup(): Unit = {
    StockPriceObserve.drop()
  }

}
