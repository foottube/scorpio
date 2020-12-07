package scorpio.jobs

import java.time.Instant

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.quartz.Scheduler
import scorpio.actors.quartz.{ActorJobEventBuilder, QuartzJob}
import scorpio.gateway.mail.MailMessage
import scorpio.messaging.{ReportAction, ReportCommand}

/**
  * Created by HONGBIN on 2017/2/2.
  */
case class EodReportJob(override val scheduler: Scheduler, override val system: ActorSystem) extends CronJob {

  val jobName = "EodReport"

  val groupName = "EodReport"

  val config = ConfigFactory.load()

  val sendTo = config.getString("mail.defaultTo")

  val cron = config.getString("report.cron.eod")

  val eventBuilder = new ActorJobEventBuilder {
    override def buildEvent() = ReportCommand(ReportAction.EOD, None, Some(MailMessage("SCORPIO", sendTo, Instant.now, "SCORPIO EOD REPORT", "")))
  }

  override def schedule() = {
    QuartzJob(groupName, Map(
      "EodReport" -> cron
    ), scheduler, system, eventBuilder).schedule()
  }
}
