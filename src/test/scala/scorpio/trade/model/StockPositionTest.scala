package scorpio.trade.model

import java.time.ZonedDateTime

import org.mongodb.scala.model.Filters._
import scorpio.common.UnitSpec
import scorpio.marketdata.model.StockPrice

/**
  * Created by HONGBIN on 2017/1/14.
  */
class StockPositionTest extends UnitSpec {

  val position = StockPosition(
    code = "sh600787",
    name = "中储股份",
    quantity = 5000,
    cost = 9.36,
    payment = 46800
  )

  val stockPrice = StockPrice(code = "sh600787", price = 9.27)

  val stockPositionValue = StockPositionValue(
    position = position,
    price = stockPrice,
    value = 46350,
    profit = -450,
    datetime = ZonedDateTime.now
  )

  "StockPosition" can "be saved into Mongo" in {
    StockPosition.insert(position)
    val saved = StockPosition.find(equal("code", "sh600787"))
    assert(saved.size == 1)
    assert(saved.head == position)
  }

  it can "be dropped from Mongo" in {
    StockPosition.drop()
  }

  "StockPositionValue" can "be saved into Mongo" in {
    StockPositionValue.insert(stockPositionValue)
    val saved = StockPositionValue.findAll
    assert(saved.size == 1)
    assert(saved.head == stockPositionValue)
  }

  it can "be searched by stock code" in {
    val saved = StockPositionValue.find(equal("position.code", "sh600787"))
    assert(saved.size == 1)
    assert(saved.head == stockPositionValue)
  }

  it can "ben dropped from Mongo" in {
    StockPositionValue.drop()
  }

}
