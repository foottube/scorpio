package scorpio.marketdata.model

import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import scorpio.common.UnitSpec

/**
  * Created by HONGBIN on 2017/1/5.
  */
class StockCodeNameTest extends UnitSpec {

  val pair1 = StockCodeName("sz002601", "佰利联", true)
  val pair2 = StockCodeName("sh600485", "信威集团", true)

  "StockCodeName" can "be saved into Mongo" in {
    StockCodeName.insertMany(List(pair1, pair2))
  }

  it can "be retrieved by code" in {
    val pair = StockCodeName.findOne(and(equal("code", "sz002601"), equal("valid", true))).get
    assert(pair == pair1)
  }

  it can "be retrieved by name" in {
    val pair = StockCodeName.findOne(and(equal("name", "信威集团"), equal("valid", true))).get
    assert(pair == pair2)
  }

  it can "be made invalid" in {
    StockCodeName.update(equal("code", "sh600485"), set("valid", false))
    val stocks = StockCodeName.find(equal("valid", true))
    assert(stocks.size == 1)
    assert(stocks.contains(pair1))
    assert(!stocks.contains(pair2))
  }

  it can "be dropped" in {
    StockCodeName.drop()
  }

}
