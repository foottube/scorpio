package scorpio.common

import java.time.ZonedDateTime

/**
  * Created by HONGBIN on 2017/1/29.
  */
object ZonedDateTimeOrdering {

  implicit val zonedDateTimeOrdering = new Ordering[ZonedDateTime] {
    override def compare(x: ZonedDateTime, y: ZonedDateTime): Int = x.compareTo(y)
  }

}
