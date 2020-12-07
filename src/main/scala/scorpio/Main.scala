package scorpio

import akka.actor.{ActorSystem, Props}
import akka.routing.FromConfig
import scorpio.actors.feed.PriceFeedSuperviseActor
import scorpio.actors.http.StockObserveActor
import scorpio.actors.mail.MailSuperviseActor
import scorpio.actors.persistence.SelectedStockPersistActor
import scorpio.actors.trade.{PortfolioManageSuperviseActor, TradeCaptureSuperviseActor}
import scorpio.jobs.{CronJob, EodReportJob, PriceFeedStartJob, PriceFeedStopJob}

/**
  * Created by HONGBIN on 2017/1/2.
  */
object Main extends App {

  implicit val system = ActorSystem("scorpio")
  implicit val ec = system.dispatcher
  val stockObserverPool = system.actorOf(FromConfig.props(Props(new StockObserveActor)), "stockObserverPool")
  val selectedStockPersistActor = system.actorOf(Props[SelectedStockPersistActor], "selectedStockPersistActor")
  val priceFeedSuperviseActor = system.actorOf(Props[PriceFeedSuperviseActor], "priceFeedSuperviseActor")
  val mailSuperviseActor = system.actorOf(Props[MailSuperviseActor], "mailSuperviseActor")
  val tradeCaptureSuperviseActor = system.actorOf(Props[TradeCaptureSuperviseActor], "tradeCaptureSuperviseActor")
  val portfolioManageSuperviseActor = system.actorOf(PortfolioManageSuperviseActor.props(priceFeedSuperviseActor), "portfolioManageSuperviseActor")
  val scheduler = CronJob.scheduler
  PriceFeedStartJob(scheduler, system).schedule()
  PriceFeedStopJob(scheduler, system).schedule()
  EodReportJob(scheduler, system).schedule()
}

