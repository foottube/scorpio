package scorpio.trade.calc

/**
  * Created by HONGBIN on 2017/1/27.
  */
object Calc {

  def round(value: Double, precision: Double = 0.01): Double = {
    val factor: Int = (1 / precision).toInt
    Math.round(value * factor) / factor.toDouble
  }

}
