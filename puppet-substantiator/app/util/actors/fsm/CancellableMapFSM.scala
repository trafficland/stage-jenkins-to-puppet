package util.actors.fsm

import akka.actor.FSM
import akka.actor.Cancellable
import concurrent.duration._

trait ICancellableMapFSMDomain {

  case class Cancel(name: String)

}

object CancellableMapFSMProvider extends IMapFSMDomainProvider[Cancellable] {
  val domain = new MapFSMDomain[Cancellable] with ICancellableMapFSMDomain
}

class CancellableMapFSM
(timeoutMilli: Int,
 override val domain: MapFSMDomain[Cancellable] with ICancellableMapFSMDomain = CancellableMapFSMProvider.domain)
  extends MapFSM[Cancellable](domain) {

  import domain._

  //http://tumblr.teamon.eu/post/2863302230/scala-combine-several-partial-functions-into-one
  override protected def partialUnHandled: StateFunction = {
    val localPartialUnhandled: StateFunction = {
      case Event(Cancel(key), Todo(ref, currentMap)) =>
        cancelTimer(key) //if called directly and before a timer
        currentMap(key).cancel()
        goto(Active) using Todo(ref, currentMap - key)
    }
    List(localPartialUnhandled, super.partialUnHandled) reduceLeft (_ orElse _)
  }

  override def handelExtraAdd(key: String) {
    setTimer(key, Cancel(key), timeoutMilli millisecond, false)
  }

  override def handelExtraRemove(key: String) {
    cancelTimer(key)
  }
}



