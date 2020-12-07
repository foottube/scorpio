package scorpio.actors.quartz

import akka.actor.ActorSystem
import org.quartz._
import scorpio.common.Log

/**
  * Created by HONGBIN on 2017/1/12.
  */
class ActorJob extends Job with Log {
  import ActorJob._

  override def execute(jobExecutionContext: JobExecutionContext) = {
    val jobKey = jobExecutionContext.getJobDetail.getKey
    logger.info(s"Job ${jobKey.getGroup}.${jobKey.getName} starts.")
    val system = jobExecutionContext.getJobDetail.getJobDataMap.get(ACTOR_SYSTEM_KEY).asInstanceOf[ActorSystem]
    val builder = jobExecutionContext.getJobDetail.getJobDataMap.get(EVENT_BUILDER_KEY).asInstanceOf[ActorJobEventBuilder]
    system.eventStream.publish(builder.buildEvent())
    logger.info(s"Job ${jobKey.getGroup}.${jobKey.getName} ends.")
  }

}

object ActorJob {

  val ACTOR_SYSTEM_KEY = "actorSystem"

  val EVENT_BUILDER_KEY = "eventBuilder"
}

trait ActorJobEventBuilder {

  def buildEvent(): AnyRef

}
