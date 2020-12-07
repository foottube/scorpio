package scorpio.common

import org.slf4j.LoggerFactory

/**
  * Created by HONGBIN on 2016/11/2.
  */
trait Log {

  val logger = LoggerFactory.getLogger(getClass)

}
