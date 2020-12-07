package scorpio.actors.mail

import java.time.Instant

import akka.actor._
import com.typesafe.config.ConfigFactory
import scorpio.actors.RepeatActor.RepeatActor
import scorpio.common.Log
import scorpio.gateway.mail.{MailGateway, MailMessage}

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
  * Created by HONGBIN on 2017/1/2.
  */
object MailCheckingActor {

  def props(mailAnalyzer: ActorRef) = Props(new MailCheckingActor(mailAnalyzer))

}

class MailCheckingActor(mailAnalyzer: ActorRef) extends RepeatActor with Log {

  val config = ConfigFactory.load()

  override val timerName = "MailCheckingActorTimer"

  override val duration = config.getInt("mail.pollInterval") seconds

  override def poll(): Unit = {
    val lastSentTime = MailMessage.findAll.headOption match {
      case Some(last) => last.sentInstant
      case None => Instant.MIN
    }
    logger.info(s"Poll message sent after $lastSentTime")
    Try { MailGateway.pollMessageReceivedAfter(lastSentTime) } match {
      case Success(messages) =>
        messages match {
          case _ +: _ => messages.foreach { message =>
            logger.info(s"Received new MailMessage $message")
            mailAnalyzer ! message
            MailMessage.upsert(message)
          }
          case Nil =>
        }
      case Failure(throwable) =>
        logger.error(s"Failed to receive mail", throwable)
        logger.info("send PoisonBill to self")
        self ! PoisonPill
    }
  }
}
