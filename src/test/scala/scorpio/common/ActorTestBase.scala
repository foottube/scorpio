package scorpio.common

import akka.actor.Props
import akka.routing.FromConfig
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, Suite}
import scorpio.actors.http.StockObserveActor

/**
  * Created by HONGBIN on 2017/1/2.
  */
trait ActorTestBase extends BeforeAndAfterAll {
  this: TestKit with Suite =>

  val stockObserverPool = system.actorOf(FromConfig.props(Props(new StockObserveActor)), "stockObserverPool")

  protected def cleanup(): Unit = {}

  protected def init(): Unit = {}

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    init()
  }

  override protected def afterAll(): Unit = {
    cleanup()
    super.afterAll()
    system.terminate()
  }

}
