package scorpio.trade.calc

import scorpio.marketdata.model.{StockPrice, StockPriceObserve}
import scorpio.trade.model.{StockPosition, StockPositionValue}

/**
  * Created by HONGBIN on 2017/1/29.
  */
object StockPositionCalculate {

  def calcPositionValue(position: StockPosition, observe: StockPriceObserve): StockPositionValue = {
    val price = StockPrice(
      code = observe.code,
      price = if (observe.currentPrice > 0) observe.currentPrice else observe.yesterdayClose)
    val value = Calc.round(position.quantity * price.price)
    val profit = Calc.round(value - position.payment)
    StockPositionValue(position, price, value, profit, observe.datetime)
  }

}
