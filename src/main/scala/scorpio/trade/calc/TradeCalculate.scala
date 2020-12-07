package scorpio.trade.calc

/**
  * Created by HONGBIN on 2017/1/21.
  */
trait TradeCalculateBase {

  def commissionRate: Double

  def taxRate: Double

  def price: Double

  def quantity: Int

  def isBuy: Boolean

  lazy val marketValue: Double = Calc.round(price * quantity)

  lazy val payment: Double = Calc.round(
    if (isBuy) marketValue * (1 + commissionRate) else marketValue * (1 - commissionRate - taxRate))

}

case class FixedCommissionTradeCalculate(
                                     override val price: Double,
                                     override val quantity: Int,
                                     override val isBuy: Boolean) extends TradeCalculateBase with FixedCommission
