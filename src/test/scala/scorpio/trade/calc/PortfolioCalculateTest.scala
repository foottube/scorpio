package scorpio.trade.calc

import java.time.ZonedDateTime

import scorpio.common.UnitSpec
import scorpio.trade.model.{StockPortfolio, StockPosition, StockTrade}

/**
  * Created by HONGBIN on 2017/1/22.
  */
class PortfolioCalculateTest extends UnitSpec {

  val now = ZonedDateTime.now

  val pos1 = StockPosition(
    code = "sh600362",
    name = "江西铜业",
    quantity = 10000,
    cost = 27.5,
    payment = 275000
  )

  val pos2 = StockPosition(
    code = "sz002415",
    name = "海康威视",
    quantity = 5000,
    cost = 24.6,
    payment = 123000
  )

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
    code = "sz002415",
    name = "海康威视",
    quantity = 1000,
    buy = true,
    price = 23,
    value = 23000,
    payment = 23023,
    dateTime = now.minusMinutes(20)
  )

  val trade3 = StockTrade(
    code = "sz002415",
    name = "海康威视",
    quantity = 3000,
    buy = false,
    price = 26,
    value = 78000,
    payment = 77844,
    dateTime = now.minusMinutes(10)
  )

  val trade4 = StockTrade(
    code = "sz000338",
    name = "潍柴动力",
    quantity = 10000,
    buy = true,
    price = 11.11,
    value = 111100,
    payment = 111211.1,
    dateTime = now.minusMinutes(15)
  )

  val trade5 = StockTrade(
    code = "sz002415",
    name = "海康威视",
    quantity = 6000,
    buy = false,
    price = 30,
    value = 180000,
    payment = 179640,
    dateTime = now.minusMinutes(10)
  )

  val trade6 = StockTrade(
    code = "sz002415",
    name = "海康威视",
    quantity = 4500,
    buy = false,
    price = 69,
    value = 310500,
    payment = 309879,
    dateTime = now.minusMinutes(10)
  )

  "FixedCommissionPortfolioCalculate" can "create new position withs new trade" in {
    val portfolio = FixedCommissionPortfolioCalculate.calcPortfolio(None, Seq(trade1, trade4))
    assert(portfolio.positions.size == 2)
    assert(portfolio.positions.contains(StockPosition(
      code = "sz002415",
      name = "海康威视",
      quantity = 1000,
      cost = 25.025,
      payment = 25025
    )))
    assert(portfolio.positions.contains(StockPosition(
      code = "sz000338",
      name = "潍柴动力",
      quantity = 10000,
      cost = 11.121,
      payment = 111211.1
    )))
  }

  it can "handle empty trade list" in {
    val prev = StockPortfolio(Vector(pos1, pos2))
    val portfolio = FixedCommissionPortfolioCalculate.calcPortfolio(Some(prev), Nil)
    assert(portfolio.positions.size == 2)
    assert(portfolio.positions.contains(pos1))
    assert(portfolio.positions.contains(pos2))
  }

  it can "update existing positions" in {
    val prev = StockPortfolio(Vector(pos1, pos2))
    val portfolio = FixedCommissionPortfolioCalculate.calcPortfolio(Some(prev), Seq(trade1, trade4))
    assert(portfolio.positions.size == 3)
    assert(portfolio.positions.contains(pos1))
    assert(portfolio.positions.contains(StockPosition(
      code = "sz002415",
      name = "海康威视",
      quantity = 6000,
      cost = 24.671,
      payment = 148025
    )))
    assert(portfolio.positions.contains(StockPosition(
      code = "sz000338",
      name = "潍柴动力",
      quantity = 10000,
      cost = 11.121,
      payment = 111211.1
    )))
  }

  it can "handle stock selling" in {
    val prev = StockPortfolio(Vector(pos1, pos2))
    val portfolio = FixedCommissionPortfolioCalculate.calcPortfolio(Some(prev), Seq(trade3))
    assert(portfolio.positions.size == 2)
    assert(portfolio.positions.contains(pos1))
    assert(portfolio.positions.contains(StockPosition(
      code = "sz002415",
      name = "海康威视",
      quantity = 2000,
      cost = 22.578,
      payment = 45156
    )))
  }

  it can "handle stock buying and selling in on batch" in {
    val prev = StockPortfolio(Vector(pos1, pos2))
    val portfolio = FixedCommissionPortfolioCalculate.calcPortfolio(Some(prev), Seq(trade1, trade2, trade3))
    assert(portfolio.positions.size == 2)
    assert(portfolio.positions.contains(pos1))
    assert(portfolio.positions.contains(StockPosition(
      code = "sz002415",
      name = "海康威视",
      quantity = 4000,
      cost = 23.301,
      payment = 93204
    )))
  }

  it can "handle stock selling all" in {
    val prev = StockPortfolio(Vector(pos1, pos2))
    val portfolio = FixedCommissionPortfolioCalculate.calcPortfolio(Some(prev), Seq(trade5, trade4, trade1))
    assert(portfolio.positions.size == 2)
    assert(portfolio.positions.contains(pos1))
    assert(portfolio.positions.exists(_.code == "sz000338"))
    assert(!portfolio.positions.exists(_.code == "sz002415"))
  }

  it can "handle minus cost" in {
    val prev = StockPortfolio(Vector(pos1, pos2))
    val portfolio = FixedCommissionPortfolioCalculate.calcPortfolio(Some(prev), Seq(trade6))
    assert(portfolio.positions.size == 2)
    assert(portfolio.positions.contains(pos1))
    assert(portfolio.positions.find(_.code == "sz002415").get.cost < 0)
  }

}
