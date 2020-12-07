package scorpio.trade.model

import scorpio.common.UnitSpec

/**
  * Created by HONGBIN on 2017/1/15.
  */
class StockPortfolioTest extends UnitSpec {

  val position1 = StockPosition(
    code = "sh600787",
    name = "中储股份",
    quantity = 5000,
    cost = 9.36,
    payment = 46800
  )

  val position2 = StockPosition(
    code = "sz000970",
    name = "中科三环",
    quantity = 10000,
    cost = 11.2,
    payment = 112000
  )

  val portfolio = StockPortfolio(Vector(position1, position2))

  "StockPortfolio" can "be saved to Mongo and retrieved" in {
    StockPortfolio.upsert(portfolio)
    val saved = StockPortfolio.findAll
    assert(saved.size == 1)
    assert(saved.head == portfolio)

    val updated = portfolio.copy(positions = Vector(position1))
    StockPortfolio.upsert(updated)
    val saved2 = StockPortfolio.findAll
    assert(saved2.size == 1)
    assert(saved2.head == updated)
  }

  "StockPortfile" can "be dropped from Mongo" in {
    StockPortfolio.drop()
  }

}
