package scorpio.trade.calc

import com.typesafe.config.{Config, ConfigFactory}

/**
  * Created by HONGBIN on 2017/1/22.
  */
trait FixedCommission {

  val config: Config = ConfigFactory.load()

  implicit val commissionRate: Double = config.getDouble("trade.calc.commissionRate")

  implicit val taxRate: Double = config.getDouble("trade.calc.taxRate")

}
