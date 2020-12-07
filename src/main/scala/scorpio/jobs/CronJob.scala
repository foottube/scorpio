package scorpio.jobs

import akka.actor.ActorSystem
import org.quartz.Scheduler
import org.quartz.impl.StdSchedulerFactory

/**
  * Created by HONGBIN on 2017/1/14.
  */
trait CronJob {

  def scheduler: Scheduler

  def system: ActorSystem

  def schedule(): Unit

}

object CronJob {

  lazy val scheduler: Scheduler = {
    val sched = StdSchedulerFactory.getDefaultScheduler()
    sched.start()
    sched
  }

}
