package scorpio.trade.calc

import java.time.ZonedDateTime

import scorpio.common.ZonedDateTimeOrdering
import scorpio.marketdata.model.StockPriceObserve
import scorpio.trade.model._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by HONGBIN on 2017/1/22.
  */
trait PortfolioCalculateBase {

  def commissionRate: Double

  def buildTradeCalculate(trade: StockTrade): TradeCalculateBase

  def calcPortfolio(previousPortfolio: Option[StockPortfolio], trades: Seq[StockTrade]): StockPortfolio = {
    previousPortfolio match {
      case Some(portfolio) =>
        val positionMap = portfolio.positions.map { position =>
          position.code -> position
        }.toMap
        import scorpio.common.ZonedDateTimeOrdering._
        val positions1 = trades.groupBy(_.code).flatMap { codeTrades =>
          codeTrades._2.sortBy(_.dateTime).foldLeft(positionMap.get(codeTrades._1)) { (pos, trade) =>
            pos match {
              case Some(position) =>
                Some(updatePosition(position, trade))
              case None =>
                Some(createPosition(trade))
            }
          }
        }.filter(_.quantity > 0)
        val tradesCodeSet = trades.map(_.code).toSet
        val positions2 = portfolio.positions.filter(p => !tradesCodeSet.contains(p.code))
        StockPortfolio((positions1 ++ positions2).toVector)
      case None =>
        StockPortfolio(trades.map(createPosition).toVector)
    }
  }

  def updatePosition(pos: StockPosition, trade: StockTrade): StockPosition = {
    val calc = buildTradeCalculate(trade)
    val quantity = if (trade.buy) pos.quantity + trade.quantity else pos.quantity - trade.quantity
    val payment = Calc.round(if (trade.buy) pos.payment + calc.payment else pos.payment - calc.payment)
    val cost = Calc.round(if (quantity > 0) payment / quantity else 0, 0.001)
    pos.copy(
      quantity = quantity,
      payment = payment,
      cost = cost
    )
  }

  def createPosition(trade: StockTrade): StockPosition = {
    val calc = buildTradeCalculate(trade)
    StockPosition(
      code = trade.code,
      name = trade.name,
      quantity = trade.quantity,
      cost = Calc.round(calc.payment / calc.quantity, 0.001),
      payment = calc.payment
    )
  }

}

object FixedCommissionPortfolioCalculate extends PortfolioCalculateBase with FixedCommission {
  override def buildTradeCalculate(trade: StockTrade) = FixedCommissionTradeCalculate(trade.price, trade.quantity, trade.buy)
}

trait PortfolioPricingBase {

  def getStockPriceObserve(code: String): Future[Option[StockPriceObserve]]

  def pricePortfolio(stock: StockPortfolio, cash: CashValue): Future[Either[String, PortfolioValue]] =
    Future.sequence(stock.positions.map { pos =>
      getStockPriceObserve(pos.code) map {
        case Some(observe) => Right(StockPositionCalculate.calcPositionValue(pos, observe))
        case None => Left(s"Cannot get price for ${pos.code}")
      }
    }).map { values =>
      if (values.exists(_.isLeft)) {
        Left(values.collect { case Left(msg) => msg }.mkString(";"))
      } else {
        import scorpio.common.ZonedDateTimeOrdering._
        val positions = values.map(_.right.get)
        val dateTime = if (positions.isEmpty) cash.datetime else positions.map(_.datetime).max
        Right(PortfolioValue(positions, cash, if (dateTime.isBefore(cash.datetime)) cash.datetime else dateTime))
      }
    }
}
