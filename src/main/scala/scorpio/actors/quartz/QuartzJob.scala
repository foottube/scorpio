package scorpio.actors.quartz

import akka.actor.ActorSystem
import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.JobBuilder.newJob
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.{JobDataMap, JobDetail, Scheduler, Trigger}
import scorpio.common.Log

/**
  * Created by HONGBIN on 2017/1/13.
  */
case class QuartzJob(
                    groupName: String,
                    crons: Map[String, String],
                    sched: Scheduler,
                    actorSystem: ActorSystem,
                    eventBuilder: ActorJobEventBuilder
                  ) extends Log {

  def schedule() = {
    val jobData = new JobDataMap()
    jobData.put(ActorJob.ACTOR_SYSTEM_KEY, actorSystem)
    jobData.put(ActorJob.EVENT_BUILDER_KEY, eventBuilder)
    crons.foreach { cron =>
      val job: JobDetail = newJob(classOf[ActorJob]).withIdentity(cron._1, groupName).usingJobData(jobData).build()
      val trigger: Trigger = newTrigger().withIdentity(cron._1, groupName).
        withSchedule(cronSchedule(cron._2)).forJob(cron._1, groupName).build()
      sched.scheduleJob(job, trigger)
      logger.info(s"Job [$job] with trigger [$trigger] scheduled.")
    }
  }

}
