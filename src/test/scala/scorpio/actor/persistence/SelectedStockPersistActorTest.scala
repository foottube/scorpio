package scorpio.actor.persistence

import java.time.Instant

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestKit
import scorpio.actors.persistence.SelectedStockPersistActor
import scorpio.common.{ActorTestBase, Log, UnitSpec}
import scorpio.gateway.mail.MailMessage
import scorpio.marketdata.model.{SelectedStock, StockCodeName}
import scorpio.messaging.{StockAction, StockCommand}
import scorpio.service.StockNameQueryService

/**
  * Created by HONGBIN on 2017/1/7.
  */
class SelectedStockPersistActorTest extends TestKit(ActorSystem("SelectedStockPersistActorTest")) with UnitSpec with ActorTestBase with Log {

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    system.actorOf(Props[SelectedStockPersistActor], "selectedStockPersistActor")
    Thread.sleep(1000)
  }

  "SelectedStockPersistActor" can "insert selected stock into Mongo" in {
    logger.info("Start test of inserting selected stock into Mongo")
    system.eventStream.publish(StockCommand(StockAction.ADD, Some("sh601398"), None))
    logger.info("Add StockCommand published")
    Thread.sleep(5000)
    logger.info("Check saved SelectedStock")
    val saved = SelectedStock.findAll.head
    assert(saved.code == "sh601398")
    assert(saved.name == "工商银行")
    assert(saved.selected)
    logger.info("End test of inserting selected stock into Mongo")
  }

  it can "invalidate selected stock in Mongo" in {
    logger.info("Start test of invalidating selected stock in Mongo")
    system.eventStream.publish(StockCommand(StockAction.REMOVE, Some("sh601398"), None))
    logger.info("Remove StockCommand published")
    Thread.sleep(5000)
    logger.info("Check saved SelectedStock")
    val saved = SelectedStock.findAll.head
    assert(saved.code == "sh601398")
    assert(saved.name == "工商银行")
    assert(!saved.selected)
    logger.info("End test of invalidating selected stock in Mongo")
  }

  override protected def cleanup(): Unit = {
    logger.info("Start of clean-up")
    SelectedStock.drop()
    // Clean up StockNameQueryService cache and mongo collection
    StockNameQueryService.cache.clean()
    StockCodeName.drop()
    logger.info("End of clean-up")
  }

}
