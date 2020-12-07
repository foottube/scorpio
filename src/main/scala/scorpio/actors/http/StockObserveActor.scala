package scorpio.actors.http

import akka.actor.Actor
import com.typesafe.config.ConfigFactory
import scorpio.common.Log
import scorpio.gateway.http.HttpGateway
import scorpio.marketdata.model.StockPriceObserve

import scala.util.{Success, Try}

/**
  * Created by HONGBIN on 2017/1/6.
  */

object StockObserveActor {

  case class GetStockObserve(code: String)

  case class GetStockObserves(codes: Seq[String])

  val baseUrl: String = ConfigFactory.load().getString("feed.price.observe.url")

}

class StockObserveActor extends Actor with Log {

  import StockObserveActor._

  override def receive = {
    case GetStockObserve(code) =>
      val url = baseUrl + code
      val data = HttpGateway.get(url)
      logger.debug(s"$data received from $url")
      val observe = StockPriceObserve(data)
      sender() ! observe
    case GetStockObserves(codes) =>
      val url = baseUrl + codes.mkString(",")
      val data = HttpGateway.get(url)
      val observes = data.split("\n").map(line => Try(StockPriceObserve(line))).collect {
        case Success(observe) => observe
      }.toSeq
      sender() ! observes
  }
}
