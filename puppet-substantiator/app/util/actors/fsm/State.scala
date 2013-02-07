package util.actors.fsm

import akka.actor.ActorRef

object BasicState {

  trait IState

  trait ITargetStateData {
    val target: ActorRef
  }

  trait IStateData

}