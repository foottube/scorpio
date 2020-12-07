package scorpio.marketdata.model

import java.time.{LocalDate, LocalTime, ZoneId, ZonedDateTime}

import org.mongodb.scala.Completed
import org.mongodb.scala.model.Filters._
import scorpio.common.UnitSpec
import scorpio.dao.mongo.MongoUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
  * Created by HONGBIN on 2016/12/10.
  */
class StockPriceObserveTest extends UnitSpec {

  val data = "var hq_str_sh600485=\"信威集团,16.870,16.900,16.890,17.030,16.820,16.890,16.900,24563063,415484916.000,2900,16.890,32000,16.880,57500,16.870,61800,16.860,81100,16.850,66205,16.900,58200,16.910,53400,16.920,41700,16.930,49200,16.940,2016-12-09,14:00:00,00\";"
  val data2 = "var hq_str_sh600485=\"信威集团,16.870,16.900,16.890,17.530,16.820,16.890,16.900,24563063,415484916.000,2900,16.890,32000,16.880,57500,16.870,61800,16.860,81100,16.850,66205,16.900,58200,16.910,53400,16.920,41700,16.930,49200,16.940,2016-12-09,14:05:00,00\";"
  val data3 = "var hq_str_sh600485=\"信威集团,16.870,16.900,16.890,18.030,16.820,16.890,16.900,24563063,415484916.000,2900,16.890,32000,16.880,57500,16.870,61800,16.860,81100,16.850,66205,16.900,58200,16.910,53400,16.920,41700,16.930,49200,16.940,2016-12-09,14:10:00,00\";"
  val data4 = "var hq_str_sh600485=\"信威集团,16.870,16.900,16.890,18.530,16.820,16.890,16.900,24563063,415484916.000,2900,16.890,32000,16.880,57500,16.870,61800,16.860,81100,16.850,66205,16.900,58200,16.910,53400,16.920,41700,16.930,49200,16.940,2016-12-09,14:20:00,00\";"


  "StockPriceObserve" can "parse String in the correct format" in {
    val expected = StockPriceObserve(
      "sh600485", "信威集团", 16.870, 16.900, 16.890, 17.030, 16.820, 16.890, 16.900, 24563063, 415484916.000,
      Vector(2900, 32000, 57500, 61800, 81100), Vector(16.890, 16.880, 16.870, 16.860, 16.850),
      Vector(66205, 58200, 53400, 41700, 49200), Vector(16.900, 16.910, 16.920, 16.930, 16.940),
      ZonedDateTime.of(LocalDate.parse("2016-12-09"), LocalTime.parse("14:00:00"), ZoneId.of("Asia/Shanghai"))
    )
    val actual = StockPriceObserve(data)
    assert(actual == expected)
  }

  it can "be inserted into Mongo database" in {
    val entity = StockPriceObserve(data)
    StockPriceObserve.insert(entity)
  }

  it can "be searched using code" in {
    val expected = StockPriceObserve(data)
    val actual = StockPriceObserve.findOne(equal("code", "sh600485")).get
    assert(actual == expected)
    assert(StockPriceObserve.findOne(equal("code", "xxxxxx")).isEmpty)
  }

  it can "be searched using datetime" in {
    val expected = StockPriceObserve(data)
    val zonedDateTime = ZonedDateTime.now
    val actual = StockPriceObserve.findOne(lt("datetime", MongoUtils.toBsonDateTime(zonedDateTime))).get
    assert(actual == expected)
    assert(StockPriceObserve.findOne(gt("datetime", MongoUtils.toBsonDateTime(zonedDateTime))).isEmpty)
  }

  it can "be inserted in batch" in {
    val observes = Seq(
      StockPriceObserve(data2),
      StockPriceObserve(data3),
      StockPriceObserve(data4)
    )
    StockPriceObserve.insertMany(observes)
  }

  it can "be retrieved by latest observed" in {
    val latest = StockPriceObserve.findLatestBefore(ZonedDateTime.now, 2)
    assert(latest.size == 2)
    assert(latest(0) == StockPriceObserve(data4))
    assert(latest(1) == StockPriceObserve(data3))

    assert(StockPriceObserve.findLatestBefore(equal("code", "XXXX"), ZonedDateTime.now, 1).isEmpty)
  }

  it can "be upsert using filter" in {
    val data5 = "var hq_str_sh600485=\"信威集团,16.870,16.900,17.000,18.530,16.820,16.890,16.900,24563063,415484916.000,2900,16.890,32000,16.880,57500,16.870,61800,16.860,81100,16.850,66205,16.900,58200,16.910,53400,16.920,41700,16.930,49200,16.940,2016-12-09,14:20:00,00\";"
    val observe = StockPriceObserve(data5)
    StockPriceObserve.upsert(and(equal("code", observe.code), equal("datetime", MongoUtils.toBsonDateTime(observe.datetime))), observe)
    val all = StockPriceObserve.findAll
    assert(all.size == 4)
    val saved = StockPriceObserve.findOne(and(equal("code", observe.code), equal("datetime", MongoUtils.toBsonDateTime(observe.datetime)))).get
    assert(saved == observe)
    val data6 = "var hq_str_sh600485=\"信威集团,16.870,16.900,17.000,18.530,16.820,16.890,16.900,24563063,415484916.000,2900,16.890,32000,16.880,57500,16.870,61800,16.860,81100,16.850,66205,16.900,58200,16.910,53400,16.920,41700,16.930,49200,16.940,2016-12-09,14:25:00,00\";"
    val observe2 = StockPriceObserve(data6)
    StockPriceObserve.upsert(and(equal("code", observe2.code), equal("datetime", MongoUtils.toBsonDateTime(observe2.datetime))), observe2)
    assert(StockPriceObserve.findAll.size == 5)
  }

  it can "be dropped" in {
    StockPriceObserve.drop
  }

}
