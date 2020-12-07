package scorpio.report

import java.time.format.{DateTimeFormatter, FormatStyle}
import java.util.Locale

import scorpio.trade.model.PortfolioValue

/**
  * Created by HONGBIN on 2017/2/2.
  */
object PortfolioValueFormatter {

  def format(portfolio: PortfolioValue): String = {
    val sb = new StringBuilder
    sb.append("PORTFOLIO VALUE AS OF ")
    sb.append(portfolio.datetime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))).append("\n")
    sb.append("\nSTOCKS:\n").append("CODE\tNAME\tQUANTITY\tPRICE\tCOST\tVALUE\tPAYMENT\tPROFIT\tTIME\n")
    portfolio.stocks.foreach {s =>
      sb.append(s.position.code).append("\t").
        append(s.position.name).append("\t").
        append(s.position.quantity).append("\t").
        append(s.price.price).append("\t").
        append(s.position.cost).append("\t").
        append(s.value).append("\t").
        append(s.position.payment).append("\t").
        append(s.profit).append("\t").
        append(s.datetime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.getDefault))).append("\n")
    }
    val stockValue = portfolio.stocks.map(_.value).sum
    sb.append("STOCK VALUE: ").append(stockValue).append("\n")
    sb.append("\nCASH:\t").append(portfolio.cash.amount).append("\n")
    val total = stockValue + portfolio.cash.amount
    sb.append("\nTOTAL:\t").append(total).append("\n")
    sb.toString()
  }
}
