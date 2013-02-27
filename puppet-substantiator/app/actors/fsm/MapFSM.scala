package actors.fsm

import akka.actor._
import akka.actor.FSM
import actors.fsm.BasicState.{IStateData, IState}
import concurrent.duration._

trait IMapFSMDomainProvider[T] {
  val domain: MapFSMDomain[T]
}

class MapFSMDomain[V] {

  // received events
  case class SetTarget(ref: Option[ActorRef])

  case class Add(key: String, value: V)

  case class Remove(key: String)

  case object Flush

  case object Status

  // sent events
  case class Batch(multiple: Map[String, V])

  // states
  case object Idle extends IState

  case object Active extends IState

  //state data
  case object Uninitialized extends IStateData

  case class Todo(target: Option[ActorRef], map: Map[String, V]) extends IStateData

}

abstract class MapFSM[T](val domain: MapFSMDomain[T]) extends Actor with FSM[IState, IStateData] {

  import this.domain._

  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(SetTarget(ref), Uninitialized) =>
      stay using Todo(ref, Map.empty[String, T])
  }


  // transition elided ...
  when(Active, stateTimeout = 1 second) {
    case Event(Flush, t: Todo) =>
      goto(Idle) using t.copy(map = Map.empty[String, T])
  }

  def handelExtraAdd(key: String, obj: T): Unit = {}

  def handelExtraRemove(key: String): Unit = {}

  final lazy val _partialUnhandled = partialUnHandled

  protected def partialUnHandled: StateFunction = {
    {
      case Event(SetTarget(ref), Todo(_, map)) =>
        stay using Todo(ref, map)
      case Event(Status, Todo(ref, map)) =>
        ref.map(_ ! Batch(map))
        stay using Todo(ref, map)
      case Event(Add(key, obj), Todo(ref, currentMap)) =>
        handelExtraAdd(key, obj)
        goto(Active) using Todo(ref, currentMap + (key -> obj))
      case Event(Remove(key), Todo(ref, currentMap)) =>
        val newMap =
          if (currentMap.contains(key))
            currentMap - key
          else
            currentMap
        if (newMap.isEmpty)
          goto(Idle)
        else {
          handelExtraRemove(key)
          goto(Active) using Todo(ref, newMap)
        }
      case Event(_, Todo(_, _)) =>
        //Could be in idle state, not putting logic in idle as it will never go active
        stay
      case Event(e, s) =>
        log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
        stay
    }
  }

  // unhandled elided ...
  // takes care of all state action events
  whenUnhandled(_partialUnhandled)

  initialize
}

object AnyMapFSMDomainProvider extends IMapFSMDomainProvider[Any] {
  val domain: MapFSMDomain[Any] = new MapFSMDomain[Any]
}

class AnyMapFSM extends MapFSM[Any](AnyMapFSMDomainProvider.domain)
