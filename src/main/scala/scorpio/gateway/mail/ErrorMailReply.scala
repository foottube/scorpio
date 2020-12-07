package scorpio.gateway.mail

import scorpio.common.Log

/**
  * Created by HONGBIN on 2017/2/1.
  */
trait ErrorMailReply {
  this: Log =>

  def handleError(message: String, mail: MailMessage): Unit = {
    logger.warn(message)
    MailGateway.replyMessage(success = false, message, mail)
  }

}
