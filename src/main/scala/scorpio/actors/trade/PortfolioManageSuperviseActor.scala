package scorpio.actors.trade

import akka.actor.{Actor, ActorRef, Props, Terminated}
import scorpio.common.Log

/**
  * Created by HONGBIN on 2017/2/1.
  */
object PortfolioManageSuperviseActor {

  def props(priceFeedActor: ActorRef) = Props(new PortfolioManageActor(priceFeedActor))

}

class PortfolioManageSuperviseActor(priceFeedActor: ActorRef) extends Actor with Log {

  var actor = context.actorOf(PortfolioManageActor.props(priceFeedActor), "portfolioManageActor")
  context.watch(actor)

  override def receive = {
    case Terminated(_) =>
      logger.warn("PortfolioManageActor terminated. Restart it.")
      actor = context.actorOf(Props[PortfolioManageActor], "portfolioManageActor")
      context.watch(actor)
  }

}
