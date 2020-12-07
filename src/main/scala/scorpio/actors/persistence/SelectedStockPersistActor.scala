package scorpio.actors.persistence

import java.time.ZonedDateTime

import akka.actor._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import scorpio.common.Log
import scorpio.dao.mongo.MongoUtils
import scorpio.gateway.mail.MailGateway
import scorpio.marketdata.model.SelectedStock
import scorpio.messaging.{PriceFeedAction, PriceFeedCommand, StockAction, StockCommand}
import scorpio.service.StockNameQueryService

/**
  * Created by HONGBIN on 2017/1/4.
  */
class SelectedStockPersistActor extends Actor with Log {

  context.system.eventStream.subscribe(self, classOf[StockCommand])
  logger.info("Subscribe to StockCommand")

  override def receive = {
    case StockCommand(StockAction.ADD, codeOption, msg) =>
      logger.info(s"Received StockAction.ADD, $codeOption, $msg")
      codeOption match {
        case Some(code) =>
          StockNameQueryService.get(code) match {
            case Some(codeName) =>
              val found = SelectedStock.find(and(equal("code", code), equal("name", codeName.name), equal("selected", true)))
              if (found.isEmpty) {
                val selectedStock = SelectedStock(
                  code = code,
                  name = codeName.name,
                  selected = true,
                  whenSelected = ZonedDateTime.now
                )
                SelectedStock.insert(selectedStock)
                context.system.eventStream.publish(PriceFeedCommand(PriceFeedAction.RELOAD, msg))
                msg.foreach(MailGateway.replyMessage(true, s"Inserted $code ${codeName.name}", _))
              } else {
                msg.foreach(MailGateway.replyMessage(true, s"$code ${codeName.name} already exists", _))
              }
            case None => msg.foreach(MailGateway.replyMessage(false, s"Cannot find name for $code", _))
          }
        case None => msg.foreach(MailGateway.replyMessage(false, "Stock code is empty", _))
      }
    case StockCommand(StockAction.REMOVE, codeOption, msg) =>
      logger.info(s"Received StockAction.REMOVE, $codeOption, $msg")
      codeOption match {
        case Some(code) =>
          SelectedStock.update(and(equal("code", code), equal("selected", true)),
            combine(set("selected", false), set("whenInvalidated", MongoUtils.toBsonDateTime(ZonedDateTime.now))))
          context.system.eventStream.publish(PriceFeedCommand(PriceFeedAction.RELOAD, msg))
          msg.foreach(MailGateway.replyMessage(true, "SelectedStock updated", _))
        case None => msg.foreach(MailGateway.replyMessage(false, "Stock code is empty", _))
      }
    case StockCommand(StockAction.LIST, _, msg) =>
      logger.info(s"Received StockAction.LIST, $msg")
      val report = SelectedStock.findAll.filter(_.selected == true).map { stock =>
        s"${stock.code} ${stock.name}"
      }.mkString("\n")
      msg.foreach(MailGateway.replyMessage(true, report, _))
  }

}
