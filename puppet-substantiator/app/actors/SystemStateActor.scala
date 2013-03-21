package actors

import akka.actor.Actor
import models.mongo.reactive.ActorState
import actors.context.{ActorNames, IActorContextProvider}
import actors.fsm.{CancellableDelay, ICancellableDelay}
import concurrent.ExecutionContext.Implicits.global
import concurrent.duration._
import play.api.libs.ws.WS

object SystemStateActor {

  case object Ping

  case object PollOn

  case object PollOff

}

class SystemStateActor(provider: IActorContextProvider, serviceUrl: String, loopDelaySeconds: Int = 4)
  extends Actor
  with ActorNames {

  import SystemStateActor._
  import actors.fsm.CancellableMapFSMDomainProvider.domain._

  val host = serviceUrl.split(':')(0)
  val port = serviceUrl.split(':')(1).toInt
  val pollName = actorStateHandlerName + "poll"

  val baseUrl =
    if (serviceUrl.startsWith("http://"))
      serviceUrl
    else "http://" + serviceUrl

  def receive = {
    case PollOn =>
      val cancel = provider.actors().system.scheduler.scheduleOnce(loopDelaySeconds seconds, self, Ping)
      provider.actors().getActor(scheduleName) ! Add(pollName, CancellableDelay(None, cancel))
    case PollOff =>
      provider.actors().getActor(scheduleName) ! Remove(pollName)
    case Ping =>
      provider.actors().getActor(scheduleName) ! Status
    case Batch(map) =>
      handleScheduled(map)
    case _ =>

  }

  //Real looping and Polling is happening between Ping and Batch where the decision to Ping again happens here
  def handleScheduled(map: Map[String, ICancellableDelay]) =
    !map.isEmpty match {
      case true =>
        if (map.contains(pollName))
          provider.actors().system.scheduler.scheduleOnce(loopDelaySeconds seconds, self, Ping)
        saveScheduled(map)
      case false =>
        deleteScheduled(map)
    }

  def saveScheduled(map: Map[String, ICancellableDelay]) = {
    import models.mongo.reactive.ActorStateDomain._
    val state = map.map(m => (("{itemScheduled:" + m._1 + ',' + "isCancelled:" + m._2.isCancelled + '}'))).toList.reduce(_ + "," + _)
    WS.url(baseUrl + "/actors/save").post {
      jsonWriter.writes(ActorState(scheduleName, true, state, None))
    }
  }

  def deleteAll() =
    WS.url(baseUrl + "/actors").delete()

  def deleteScheduled(map: Map[String, ICancellableDelay]) =
    WS.url(baseUrl + "/actors/name/" + scheduleName).delete()
}