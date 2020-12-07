package scorpio.actors.persistence

import java.util.UUID

import akka.actor.Actor
import scorpio.common.Log
import scorpio.trade.model.PortfolioValue

/**
  * Created by HONGBIN on 2017/2/1.
  */
object PortfolioValuePersistActor {
  case class PersistPortfolioValue(uuid: UUID, portfolioValue: PortfolioValue)
  case class PersistPortfolioValueSuccess(uuid: UUID)
}

class PortfolioValuePersistActor extends Actor with Log {
  import PortfolioValuePersistActor._

  override def receive = {
    case PersistPortfolioValue(uuid, portfolioValue) =>
      logger.info(s"Persist $portfolioValue")
      PortfolioValue.insert(portfolioValue)
      sender() ! PersistPortfolioValueSuccess(uuid)
  }

}
