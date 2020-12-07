package scorpio.actors.trade

import java.time.ZonedDateTime
import java.util.UUID

import akka.actor.{Actor, ActorRef, Props, Terminated}
import scorpio.actors.persistence.CashUpdateActor.{CashUpdateFail, CashUpdateSuccess, UpdateCash, UpdateCashByTrades}
import scorpio.actors.persistence.PortfolioUpdateActor._
import scorpio.actors.persistence.{CashUpdateActor, PortfolioUpdateActor}
import scorpio.actors.trade.PortfolioPricingActor.{PricePortfolio, PricePortfolioFailure, PricePortfolioSuccess}
import scorpio.common.Log
import scorpio.gateway.mail.{ErrorMailReply, MailGateway}
import scorpio.messaging._
import scorpio.report.PortfolioValueFormatter
import scorpio.service.StockNameQueryService
import scorpio.trade.calc.Calc
import scorpio.trade.model._

import scala.collection.mutable

/**
  * Created by HONGBIN on 2017/2/1.
  */
case class CommandToken(uuid: UUID, multiStep: Boolean, command: MailCommand, persist: Boolean)

object PortfolioManageActor {

  def props(priceFeedActor: ActorRef) = Props(new PortfolioManageActor(priceFeedActor))

}

class PortfolioManageActor(priceFeedActor: ActorRef) extends Actor with Log with ErrorMailReply {

  var cashUpdateActor = createActor(Props[CashUpdateActor], "cashUpdateActor")
  var portfolioUpdateActor = createActor(Props[PortfolioUpdateActor], "portfolioUpdateActor")
  var portfolioPricingActor = createActor(PortfolioPricingActor.props(priceFeedActor), "portfolioPricingActor")
  private val commandMap: mutable.Map[UUID, CommandToken] = mutable.Map.empty
  private val cashUpdateMap: mutable.Map[UUID, Option[CashValue]] = mutable.Map.empty
  private val portfolioUpdateMap: mutable.Map[UUID, Option[StockPortfolio]] = mutable.Map.empty

  override def preStart(): Unit = {
    super.preStart()
    context.system.eventStream.subscribe(self, classOf[CashCommand])
    context.system.eventStream.subscribe(self, classOf[PositionCommand])
    context.system.eventStream.subscribe(self, classOf[ReportCommand])
  }

  override def receive = {

    case command: CashCommand =>
      logger.info(s"Received $command")
      command.action match {
        case CashAction.LIST =>
          CashValue.findLatestBefore(ZonedDateTime.now) match {
            case Some(cash) => command.message.foreach(MailGateway.replyMessage(success = true, cash.toString, _))
            case None => command.message.foreach(MailGateway.replyMessage(success = true, "No cash record found", _))
          }
        case CashAction.ADD | CashAction.REMOVE =>
          val dateTime = command.dateTime match {
            case Some(time) => time
            case None => ZonedDateTime.now
          }
          val amount = command.action match {
            case CashAction.ADD =>
              command.amount match {
                case Some(amount) => amount
                case None => 0.0
              }
            case CashAction.REMOVE =>
              command.amount match {
                case Some(amount) => -1 * amount
                case None => 0.0
              }
          }
          val uuid = UUID.randomUUID()
          commandMap += (uuid -> CommandToken(uuid, multiStep = false, command, persist = true))
          val msg = UpdateCash(uuid, amount, dateTime, persist = true)
          cashUpdateActor ! msg
          logger.debug(s"$msg sent to $cashUpdateActor")
      }

    case command: PositionCommand =>
      logger.info(s"Received $command")
      command.action match {
        case PositionAction.LIST =>
          val now = ZonedDateTime.now
          val uuid = UUID.randomUUID()
          val trades = PortfolioValue.findLatestBefore(now) match {
            case Some(portfolioValue) => StockTrade.findAllAfter(portfolioValue.datetime)
            case None => StockTrade.findAllBefore(now)
          }
          commandMap += (uuid -> CommandToken(uuid, multiStep = true, command, persist = false))
          val cashUpdateMsg = UpdateCashByTrades(uuid, trades, persist = false)
          cashUpdateActor ! cashUpdateMsg
          logger.debug(s"$cashUpdateMsg sent to cashUpdateActor")
          cashUpdateMap += (uuid -> None)
          val portfolioUpdateMsg = UpdateByTrades(uuid, trades, persist = false)
          portfolioUpdateActor ! portfolioUpdateMsg
          logger.debug(s"$portfolioUpdateMsg sent to portfolioUpdateActor")
          portfolioUpdateMap += (uuid -> None)

        case PositionAction.ADD =>
          val uuid = UUID.randomUUID()
          command.code match {
            case Some(code) =>
              // Add code to SelectedStock
              context.system.eventStream.publish(StockCommand(StockAction.ADD, Some(code), None))
              val name = StockNameQueryService.get(code).map(_.name).getOrElse("")
              val quantity = command.quantity.getOrElse(0)
              val cost = command.cost.getOrElse(0.0)
              val payment = Calc.round(quantity * cost)
              val msg = AddPosition(uuid, StockPosition(code, name, quantity, cost, payment), persist = true)
              commandMap += (uuid -> CommandToken(uuid, multiStep = false, command, persist = true))
              portfolioUpdateActor ! msg
              logger.debug(s"$msg sent ot $portfolioUpdateActor")
            case None =>
              command.message.foreach(handleError("Stock code is empty!", _))
          }
        case PositionAction.REMOVE =>
          val uuid = UUID.randomUUID()
          command.code match {
            case Some(code) =>
              val msg = RemovePosition(uuid, code, true)
              commandMap += (uuid -> CommandToken(uuid, multiStep = false, command, persist = true))
              portfolioUpdateActor ! msg
              logger.debug(s"$msg sent ot $portfolioUpdateActor")
            case None =>
              command.message.foreach(handleError("Stock code is empty!", _))
          }
      }

    case command: ReportCommand =>
      logger.info(s"Received $command")
      command.action match {
        case ReportAction.EOD =>
          val now = ZonedDateTime.now
          val uuid = UUID.randomUUID()
          val trades = PortfolioValue.findLatestBefore(now) match {
            case Some(portfolioValue) => StockTrade.findAllAfter(portfolioValue.datetime)
            case None => StockTrade.findAllBefore(now)
          }
          commandMap += (uuid -> CommandToken(uuid, multiStep = true, command, persist = true))
          val cashUpdateMsg = UpdateCashByTrades(uuid, trades, persist = true)
          cashUpdateActor ! cashUpdateMsg
          logger.debug(s"$cashUpdateMsg sent to cashUpdateActor")
          cashUpdateMap += (uuid -> None)
          val portfolioUpdateMsg = UpdateByTrades(uuid, trades, persist = true)
          portfolioUpdateActor ! portfolioUpdateMsg
          logger.debug(s"$portfolioUpdateMsg sent to portfolioUpdateActor")
          portfolioUpdateMap += (uuid -> None)
      }

    case CashUpdateSuccess(uuid, cash) =>
      commandMap.get(uuid) match {
        case Some(token) =>
          if (token.multiStep) {
            portfolioUpdateMap.get(uuid) match {
              case Some(opt) =>
                opt match {
                  case Some(portfolio) =>
                    // TODO datetime in PricePortfolio below should be able to take value besides "now"
                    val pricingMsg = PricePortfolio(uuid, portfolio, cash, Some(ZonedDateTime.now), token.persist)
                    portfolioPricingActor ! pricingMsg
                    logger.debug(s"$pricingMsg sent to portfolioPricingActor")
                    cashUpdateMap -= uuid
                    portfolioUpdateMap -= uuid
                  case None => cashUpdateMap.put(uuid, Some(cash))
                }
              case None =>
                logger.error(s"Cannot find portfolioUpdateMap record for $uuid")
                cashUpdateMap -= uuid
                commandMap -= uuid
            }
          } else {
            token.command.message.foreach(MailGateway.replyMessage(success = true, cash.toString, _))
            commandMap -= uuid
          }

        case None => logger.error(s"Cannot find command token for $uuid")
      }

    case PortfolioUpdateSuccess(uuid, portfolio) =>
      commandMap.get(uuid) match {
        case Some(token) =>
          if (token.multiStep) {
            cashUpdateMap.get(uuid) match {
              case Some(opt) =>
                opt match {
                  case Some(cash) =>
                    // TODO datetime in PricePortfolio below should be able to take value besides "now"
                    val pricingMsg = PricePortfolio(uuid, portfolio, cash, Some(ZonedDateTime.now), token.persist)
                    portfolioPricingActor ! pricingMsg
                    logger.debug(s"$pricingMsg sent to portfolioPricingActor")
                    cashUpdateMap -= uuid
                    portfolioUpdateMap -= uuid
                  case None => portfolioUpdateMap.put(uuid, Some(portfolio))
                }
              case None =>
                logger.error(s"Cannot find cashUpdateMap record for $uuid")
                portfolioUpdateMap -= uuid
                commandMap -= uuid
            }
          } else {
            token.command.message.foreach(MailGateway.replyMessage(success = true, portfolio.toString, _))
            commandMap -= uuid
          }
        case None => logger.error(s"Cannot find command token for $uuid")
      }

    case PricePortfolioSuccess(uuid, portfolioValue) =>
      commandMap.get(uuid) match {
        case Some(token) =>
          token.command.message.foreach(MailGateway.replyMessage(success = true, PortfolioValueFormatter.format(portfolioValue), _))
          commandMap -= uuid
        case None => logger.error(s"Cannot find command token for $uuid")
      }

    case CashUpdateFail(uuid, message) =>
      commandMap.get(uuid) match {
        case Some(token) =>
          token.command.message.foreach(MailGateway.replyMessage(success = false, message, _))
          cashUpdateMap -= uuid
        case None => logger.error(s"Cannot find command token for $uuid")
      }

    case PortfolioUpdateFail(uuid, message) =>
      commandMap.get(uuid) match {
        case Some(token) =>
          token.command.message.foreach(MailGateway.replyMessage(success = false, message, _))
          portfolioUpdateMap -= uuid
        case None => logger.error(s"Cannot find command token for $uuid")
      }

    case PricePortfolioFailure(uuid, message) =>
      commandMap.get(uuid) match {
        case Some(token) =>
          commandMap -= uuid
          cashUpdateMap -= uuid
          portfolioUpdateMap -= uuid
          token.command.message.foreach(MailGateway.replyMessage(success = false, message, _))
        case None => logger.error(s"Cannot find command token for $uuid")
      }

    case Terminated(actor) =>
      if (actor == cashUpdateActor) {
        logger.warn("CashUpdateActor terminated. Restart it.")
        cashUpdateActor = createActor(Props[CashUpdateActor], "cashUpdateActor")
        // TODO redo remaining cash updating command
      } else if (actor == portfolioUpdateActor) {
        logger.warn("PortfolioUpdateActor terminated. Restart it.")
        portfolioUpdateActor = createActor(Props[PortfolioUpdateActor], "portfolioUpdateActor")
        // TODO redo remaining portfolio updating command
      } else if (actor == portfolioPricingActor) {
        logger.warn("PortfolioPricingActor terminated. Restart it.")
        portfolioPricingActor = createActor(Props[PortfolioPricingActor], "portfolioPricingActor")
      }

    case unknown => logger.error(s"Received unknown message: $unknown")
  }

  private def createActor(props: Props, name: String) = {
    val actor = context.actorOf(props, name)
    context.watch(actor)
    actor
  }

}
