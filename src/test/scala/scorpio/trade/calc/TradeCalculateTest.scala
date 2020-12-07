package scorpio.trade.calc

import com.typesafe.config.ConfigFactory
import scorpio.common.UnitSpec

/**
  * Created by HONGBIN on 2017/1/21.
  */
class TradeCalculateTest extends UnitSpec {

  val config = ConfigFactory.load()

  val commissionRate = config.getDouble("trade.calc.commissionRate")

  val taxRate = config.getDouble("trade.calc.taxRate")

  "FixCommissionRateTradeCalculate" can "calculate market value etc." in {
    val buyCalc = FixedCommissionTradeCalculate(12.5, 10000, true)
    assert(buyCalc.marketValue == 125000)
    assert(buyCalc.payment == Calc.round(125000 * (1 + commissionRate)))

    val sellCalc = FixedCommissionTradeCalculate(12.5, 10000, false)
    assert(sellCalc.marketValue == 125000)
    assert(sellCalc.payment == Calc.round(125000 * (1 - commissionRate - taxRate)))

  }

}
