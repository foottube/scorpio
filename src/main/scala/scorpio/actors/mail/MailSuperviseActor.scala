package scorpio.actors.mail

import akka.actor.{Actor, Props, Terminated}
import scorpio.actors.RepeatActor.Start
import scorpio.common.Log

/**
  * Created by HONGBIN on 2017/1/8.
  */
class MailSuperviseActor extends Actor with Log {

  var analyzer = context.actorOf(Props[MailAnalyzerActor], "mailAnalyzer")

  var checker = context.actorOf(MailCheckingActor.props(analyzer), "mailChecker")

  context.watch(analyzer)

  context.watch(checker)

  checker ! Start

  override def receive = {
    case Terminated(actorRef) =>
      if (actorRef == analyzer) {
        logger.warn("MailAnalyzerActor terminated. Restart it.")
        analyzer = context.actorOf(Props[MailAnalyzerActor], "mailAnalyzer")
        context.watch(analyzer)
      } else if (actorRef == checker) {
        logger.warn("MailCheckingActor terminated. Restart it.")
        checker = context.actorOf(MailCheckingActor.props(analyzer), "mailChecker")
        context.watch(checker)
        checker ! Start
      }
  }
}
