package scorpio.jobs

import java.time.Instant

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.quartz.Scheduler
import scorpio.actors.quartz.{ActorJobEventBuilder, QuartzJob}
import scorpio.gateway.mail.MailMessage
import scorpio.messaging.{PriceFeedAction, PriceFeedCommand}

/**
  * Created by HONGBIN on 2017/1/14.
  */
case class PriceFeedStartJob(override val scheduler: Scheduler, override val system: ActorSystem) extends CronJob {

  val jobName = "priceFeedStart"

  val groupName = "priceFeedJob"

  val config = ConfigFactory.load()

  val sendTo = config.getString("mail.defaultTo")

  val morningCron = config.getString("feed.price.job.cron.morningStart")

  val afternoonCron = config.getString("feed.price.job.cron.afternoonStart")

  val eventBuilder = new ActorJobEventBuilder {
    override def buildEvent() = PriceFeedCommand(PriceFeedAction.START, Some(MailMessage("SCORPIO", sendTo, Instant.now, "SCORPIO PRICEFEED START", "")))
  }

  override def schedule() = {
    QuartzJob(groupName, Map(
      "priceFeedMorningStart" -> morningCron,
      "priceFeedAfternoonStart" -> afternoonCron
    ), scheduler, system, eventBuilder).schedule()
  }

}

case class PriceFeedStopJob(override val scheduler: Scheduler, override val system: ActorSystem) extends CronJob {

  val jobName = "priceFeedStop"

  val groupName = "priceFeedJob"

  val config = ConfigFactory.load()

  val sendTo = config.getString("mail.defaultTo")

  val morningCron = config.getString("feed.price.job.cron.morningStop")

  val afternoonCron = config.getString("feed.price.job.cron.afternoonStop")

  val eventBuilder = new ActorJobEventBuilder {
    override def buildEvent() = PriceFeedCommand(PriceFeedAction.STOP, Some(MailMessage("SCORPIO", sendTo, Instant.now, "SCORPIO PRICEFEED STOP", "")))
  }

  override def schedule() = {
    QuartzJob(groupName, Map(
      "priceFeedMorningStop" -> morningCron,
      "priceFeedAfternoonStop" -> afternoonCron
    ), scheduler, system, eventBuilder).schedule()
  }

}

