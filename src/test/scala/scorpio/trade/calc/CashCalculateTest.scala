package scorpio.trade.calc

import java.time.ZonedDateTime

import scorpio.common.UnitSpec
import scorpio.trade.model.StockTrade

/**
  * Created by HONGBIN on 2017/1/27.
  */
class CashCalculateTest extends UnitSpec {

  val  now = ZonedDateTime.now

  val trade1 = StockTrade(
    code = "sz002415",
    name = "海康威视",
    buy = true,
    quantity = 1000,
    price = 25,
    value = 25000,
    payment = 25025,
    dateTime = now.minusMinutes(30)
  )

  val trade2 = StockTrade(
    code = "sz000338",
    name = "潍柴动力",
    quantity = 10000,
    buy = true,
    price = 11.11,
    value = 111100,
    payment = 111211.1,
    dateTime = now.minusMinutes(15)
  )

  val trade3 = StockTrade(
    code = "sz002415",
    name = "海康威视",
    quantity = 6000,
    buy = false,
    price = 30,
    value = 180000,
    payment = 179640,
    dateTime = now.minusMinutes(10)
  )

  "CashCalculate" can "calculate cash value after trades happen" in {
    val cash = CashCalculate.calcCash(200000, Seq(trade1, trade2, trade3))
    assert(cash == 243403.9)
  }

  it can "handle empty trade list" in {
    val cash = CashCalculate.calcCash(200000, Nil)
    assert(cash == 200000)
  }

}
