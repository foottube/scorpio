package scorpio.actors

import akka.actor.FSM
import scorpio.common.Log

import scala.concurrent.duration.FiniteDuration

/**
  * Created by HONGBIN on 2017/1/1.
  */

object RepeatActor {

  sealed trait State
  case object Idle extends State
  case object Busy extends State

  case object Start
  case object Stop
  case object Poll
  case object Status

  case class StateData()

  trait RepeatActor extends FSM[State, StateData] with Log {

    def poll(): Unit
    def timerName: String
    def duration: FiniteDuration

    startWith(Idle, new StateData)
    when(Idle) {
      case Event(Start, _: StateData) =>
        logger.info(s"${self.path} starts.")
        setTimer(timerName, Poll, duration, repeat = true)
        goto(Busy)
      case Event(Status, _: StateData) =>
        sender() ! Idle
        stay
    }
    when(Busy) {
      case Event(Stop, _: StateData) =>
        logger.info(s"${self.path} stops.")
        cancelTimer(timerName)
        goto(Idle)
      case Event(Poll, _: StateData) =>
        poll()
        stay
      case Event(Status, _: StateData) =>
        sender() ! Busy
        stay
    }
    initialize()
  }
}

