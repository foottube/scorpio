package scorpio.trade.calc

import scorpio.trade.model.StockTrade

/**
  * Created by HONGBIN on 2017/1/27.
  */
object CashCalculate {

  def calcCash(initial: Double, trades: Seq[StockTrade]): Double = Calc.round(trades.foldLeft(initial) {
    (cash, trade) => cash + (if(trade.buy) -1 * trade.payment else trade.payment)
  })

}
