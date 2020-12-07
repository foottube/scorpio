package scorpio.trade.model

import java.time.ZonedDateTime

import scorpio.common.UnitSpec
import scorpio.marketdata.model.StockPrice

/**
  * Created by HONGBIN on 2017/1/15.
  */
class PortfolioValueTest extends UnitSpec {

  val now = ZonedDateTime.now

  val position1 = StockPosition(
    code = "sh600787",
    name = "中储股份",
    quantity = 5000,
    cost = 9.36,
    payment = 46800
  )

  val stockPrice1 = StockPrice(code = "sh600787", price = 9.27)

  val stockPositionValue1 = StockPositionValue(
    position = position1,
    price = stockPrice1,
    value = 46350,
    profit = -450,
    datetime = now
  )

  val position2 = StockPosition(
    code = "sz000970",
    name = "中科三环",
    quantity = 10000,
    cost = 10.0,
    payment = 100000
  )

  val stockPrice2 = StockPrice(code = "sz000970", price = 11.0)

  val stockPositionValue2 = StockPositionValue(
    position = position2,
    price = stockPrice2,
    value = 110000,
    profit = 10000,
    datetime = now
  )

  val cash = CashValue(200000, ZonedDateTime.now)

  val portfolioValue = PortfolioValue(
    stocks = Vector(stockPositionValue1, stockPositionValue2),
    cash = cash,
    datetime = now
  )

  "PortfolioValue" can "be saved into Mongo and retrieved" in {
    PortfolioValue.insert(portfolioValue)
    val saved = PortfolioValue.findAll
    assert(saved.size == 1)
    assert(saved.head == portfolioValue)
  }

  "PortfolioValue" can "be dropped from Mongo" in {
    PortfolioValue.drop()
  }

}
