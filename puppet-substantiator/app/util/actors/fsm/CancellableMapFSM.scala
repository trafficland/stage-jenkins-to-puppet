package util.actors.fsm

import akka.actor.Cancellable
import concurrent.duration._

trait ICancellableMapFSMDomain {

  case class Cancel(name: String)

}

object CancellableMapFSMProvider extends IMapFSMDomainProvider[Cancellable] {
  val domain = new MapFSMDomain[Cancellable] with ICancellableMapFSMDomain
}

class CancellableMapFSM
(override val domain: MapFSMDomain[Cancellable] with ICancellableMapFSMDomain = CancellableMapFSMProvider.domain,
 timeoutMilli: Int)
  extends MapFSM[Cancellable](domain) {

  import domain._

  whenUnhandled {
    case Event(Cancel(key), Todo(ref, currentMap)) =>
      cancelTimer(key) //if called directly and before a timer
      currentMap(key).cancel()
      goto(Active) using Todo(ref, currentMap - key)
  }

  override def handelExtraAdd(key: String) {
    setTimer(key, Cancel(key), timeoutMilli millisecond, false)
  }

  override def handelExtraRemove(key: String) {
    cancelTimer(key)
  }
}



