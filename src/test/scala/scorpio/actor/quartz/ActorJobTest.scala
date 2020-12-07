package scorpio.actor.quartz

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.quartz.impl.StdSchedulerFactory
import scorpio.actors.quartz.{ActorJobEventBuilder, QuartzJob}
import scorpio.common.{ActorTestBase, Log, UnitSpec}

import scala.concurrent.duration._

/**
  * Created by HONGBIN on 2017/1/14.
  */
class ActorJobTest extends TestKit(ActorSystem("ActorJobTest")) with UnitSpec with ActorTestBase with Log {

  case class TestEvent()

  "ActorJob" should "be triggered at specified time" in {
    val scheduler = StdSchedulerFactory.getDefaultScheduler()
    scheduler.start()
    val probe = TestProbe()
    system.eventStream.subscribe(probe.ref, classOf[TestEvent])
    val eventBuilder = new ActorJobEventBuilder {
      override def buildEvent() = TestEvent()
    }
    val now = ZonedDateTime.now
    val inTenSec = now.plusSeconds(5)
    val cron = s"${inTenSec.getSecond} ${inTenSec.getMinute} ${inTenSec.getHour} ${inTenSec.getDayOfMonth} ${inTenSec.getMonthValue} ? ${inTenSec.getYear}"
    logger.info(s"cron = ${cron}")
    QuartzJob("group", Map("job1" -> cron), scheduler, system, eventBuilder).schedule()
    probe.expectMsg(7 seconds, TestEvent())
    Thread.sleep(10000)
  }

}
