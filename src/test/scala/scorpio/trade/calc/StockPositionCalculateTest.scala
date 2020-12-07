package scorpio.trade.calc

import scorpio.common.UnitSpec
import scorpio.marketdata.model.{StockPrice, StockPriceObserve}
import scorpio.trade.model.{StockPosition, StockPositionValue}

/**
  * Created by HONGBIN on 2017/1/29.
  */
class StockPositionCalculateTest extends UnitSpec {

  "StockPositionCalculate" can "calculate positive stock position value" in {
    val position = StockPosition(
      "sz300068",
      "南都电源",
      5000,
      20.02,
      100100
    )
    val price = StockPriceObserve("var hq_str_sz300068=\"南都电源,0.000,20.140,0.000,0.000,0.000,0.000,0.000,0,0.000,0,0.000,0,0.000,0,0.000,0,0.000,0,0.000,0,0.000,0,0.000,0,0.000,0,0.000,0,0.000,2017-01-26,16:37:03,03\";")
    val value = StockPositionCalculate.calcPositionValue(position, price)
    val expected = StockPositionValue(
      position,
      StockPrice(price.code, 20.14),
      100700,
      600,
      price.datetime
    )
    assert(value == expected)
  }

  it can "calculate negative stock position value" in {
    val position = StockPosition(
      "sh600485",
      "信威集团",
      7000,
      16.949,
      118643
    )
    val price = StockPriceObserve("var hq_str_sh600485=\"信威集团,16.870,16.900,16.890,17.030,16.820,16.890,16.900,24563063,415484916.000,2900,16.890,32000,16.880,57500,16.870,61800,16.860,81100,16.850,66205,16.900,58200,16.910,53400,16.920,41700,16.930,49200,16.940,2016-12-09,14:00:00,00\";")
    val value = StockPositionCalculate.calcPositionValue(position, price)
    val expected = StockPositionValue(
      position,
      StockPrice(price.code, 16.89),
      118230,
      -413,
      price.datetime
    )
    assert(value == expected)
  }

}
