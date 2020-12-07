package scorpio.trade.model

import java.time.ZonedDateTime

import scorpio.common.UnitSpec

/**
  * Created by HONGBIN on 2017/1/15.
  */
class CashValueTest extends UnitSpec {

  val cash1 = CashValue(300000.0, ZonedDateTime.now.minusDays(1).minusHours(6))

  val cash2 = CashValue(500000.0, ZonedDateTime.now.minusHours(5))

  "Cash" can "be saved into Mongo" in {
    CashValue.insertMany(List(cash1, cash2))
    assert(CashValue.findAll.size == 2)
  }

  "Latest Cash value" can "be retrieved" in {
    val latest = CashValue.findLatestBefore(ZonedDateTime.now).get
    assert(latest == cash2)
    val latest2 = CashValue.findLatestBefore(ZonedDateTime.now.minusDays(1)).get
    assert(latest2 == cash1)
  }

  "Cash" can "be dropped from Mongo" in {
    CashValue.drop()
  }

}
