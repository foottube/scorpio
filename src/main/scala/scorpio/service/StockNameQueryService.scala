package scorpio.service

import com.typesafe.config.ConfigFactory
import scorpio.common.Log
import scorpio.gateway.http.HttpGateway
import scorpio.marketdata.model.{StockCodeName, StockPriceObserve}

import scala.util.{Failure, Success, Try}

/**
  * Created by HONGBIN on 2017/1/7.
  */

class HttpStockNameQueryService(baseUrl: String) extends FallbackQueryService[String, StockCodeName] with Log {

  override def query(key: String): Option[StockCodeName] = {
    val url = baseUrl + key
    val data = HttpGateway.get(url)
    Try {
      StockPriceObserve(data)
    } match {
      case Success(observe) =>
        StockPriceObserve(data)
        Some(StockCodeName(
          code = observe.code,
          name = observe.name,
          valid = true
        ))
      case Failure(throwable) =>
        logger.error(s"Failed to query $key", throwable)
        None
    }
  }

  override def add(key: String, value: StockCodeName): Unit = {}

  override val fallback = None

}

object StockNameQueryService {

  val baseUrl: String = ConfigFactory.load().getString("feed.price.observe.url")

  val http = new HttpStockNameQueryService(baseUrl)

  val mongo = new MongoQueryService[String, StockCodeName]("code", StockCodeName, Some(http))

  val cache = new LocalMapQueryService[String, StockCodeName](Some(mongo))

  def get(code: String): Option[StockCodeName] = cache.get(code)

}
