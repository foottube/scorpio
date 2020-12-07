package scorpio.actors.trade

import akka.actor.{Actor, ActorRef, Props, Terminated}
import scorpio.common.Log

/**
  * Created by HONGBIN on 2017/1/22.
  */
class TradeCaptureSuperviseActor extends Actor with Log {

  var tradeCaptureActor = createPersistActor()

  override def receive = {
    case Terminated(_) =>
      logger.warn("TradeCaptureActor terminated. Restart it.")
      tradeCaptureActor = createPersistActor()
  }

  private def createPersistActor(): ActorRef = {
    val actor = context.actorOf(Props[TradeCaptureActor], "tradeCaptureActor")
    context.watch(actor)
  }

}
