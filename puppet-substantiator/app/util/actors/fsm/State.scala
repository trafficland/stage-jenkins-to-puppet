package util.actors.fsm

import akka.actor.ActorRef

object BasicState {

  trait IState

  trait IStateData {
    val target: ActorRef
  }

}