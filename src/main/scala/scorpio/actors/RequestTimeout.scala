package scorpio.actors

import akka.util.Timeout
import com.typesafe.config.ConfigFactory

/**
  * Created by HONGBIN on 2017/1/9.
  */
trait RequestTimeout {
  import scala.concurrent.duration._
  implicit val requestTimeout: Timeout = {
    val timeout = ConfigFactory.load().getString("akka.requestTimeout")
    val duration = Duration(timeout)
    FiniteDuration(duration.length, duration.unit)
  }
}
