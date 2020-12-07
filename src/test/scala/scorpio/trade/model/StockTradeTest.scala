package scorpio.trade.model

import java.time.ZonedDateTime

import org.mongodb.scala.model.Filters._
import scorpio.common.UnitSpec
import scorpio.dao.mongo.MongoUtils

/**
  * Created by HONGBIN on 2017/1/2.
  */
class StockTradeTest extends UnitSpec {

  val trade = StockTrade(
    code = "002601",
    name = "佰利联",
    buy = true,
    quantity = 1000,
    price = 12.5,
    value = 12500,
    payment = 12800,
    dateTime = ZonedDateTime.now.minusHours(1)
  )

  "StockTrade" can "be saved in Mongo" in {
    StockTrade.insert(trade)
  }

  it can "be found using dateTime" in {
    val dateTime = ZonedDateTime.now
    val retrieved = StockTrade.find(lt("datetime", MongoUtils.toBsonDateTime(dateTime)))
    assert(retrieved.size == 1)
    assert(retrieved.head == trade)
  }

  it can "be dropped" in {
    StockTrade.drop
  }

}
