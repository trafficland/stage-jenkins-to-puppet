package actors.http

import akka.actor.Actor
import spray.util.SprayActorLogging
import spray.http._
import HttpMethods._
import spray.http._
import spray.can.client.{DefaultHttpClient, HttpDialog}
import spray.http.HttpRequest
import models.mongo.reactive.ActorState
import models.mongo.reactive.ActorStateDomain._
import spray.json.{BasicFormats}
import actors.context.{IActorNames, IActorContextProvider}
import actors.fsm.{CancellableDelay, ICancellableDelay}
import concurrent.ExecutionContext.Implicits.global
import concurrent.duration._

class SystemStateHttpActor(provider: IActorContextProvider, serviceUrl: String, loopDelaySeconds: Int = 4)
  extends Actor
  with SprayActorLogging
  with IActorNames with BasicFormats
  with view.IScheduledViews
  with view.IBasicViews {

  import actors.fsm.CancellableMapFSMDomainProvider.domain._

  protected case object Ping

  val host = serviceUrl.split(':')(0)
  val port = serviceUrl.split(':')(1).toInt

  val httpClient = DefaultHttpClient(provider.actors().system)

  val pollName = httpStateHandlerName + "poll"

  def receive = {
    case HttpRequest(GET, "/actorHook/routes", _, _, _) =>
      sender ! routesView
    case HttpRequest(GET, "/actorHook/scheduled", _, _, _) =>
      self ! Ping
      sender ! hookView
    case HttpRequest(GET, "/actorHook/pollOn", _, _, _) =>
      val cancel = provider.actors().system.scheduler.scheduleOnce(loopDelaySeconds seconds, self, Ping)
      provider.actors().getActor(scheduleName) ! Add(pollName, CancellableDelay(None, cancel))
      sender ! pollOnOff("on")
    case HttpRequest(GET, "/actorHook/pollOff", _, _, _) =>
      provider.actors().getActor(scheduleName) ! Remove(pollName)
      sender ! pollOnOff("off")
    case Ping =>
      provider.actors().getActor(scheduleName) ! Status
    case Batch(map) =>
      handleScheduled(map)
    case _ =>
      sender ! notFoundView
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
    val state = map.map(m => (("{itemScheduled:" + m._1 + ',' + "isCancelled:" + m._2.isCancelled + '}'))).toList.reduce(_ + "," + _)
    val ent = HttpEntity {
      Some {
        HttpBody(ContentType.`application/json`,
          jsonSprayFormat.write(
            ActorState(scheduleName,
              true,
              state)
          ).compactPrint)
      }
    }
    val responseF = HttpDialog(httpClient, host, port).send {
      HttpRequest(method = HttpMethods.POST,
        uri = "/actors/save",
        headers =
          HttpHeaders.`Content-Type`(ContentType.`application/json`) :: Nil,
        entity = ent)
    }.end

    for {
      response <- responseF
    }
    yield {
      log.info(
        """|Result from host:
          |status : {}
          |headers: {}
          |body   : {}""".stripMargin,
        response.status, response.headers.mkString("\n  ", "\n  ", ""), response.entity.asString
      )
    }
  }

  def deleteScheduled(map: Map[String, ICancellableDelay]) =
    HttpDialog(httpClient, host, port).send(HttpRequest(method = HttpMethods.DELETE, uri = "/actors/name/" + scheduleName)).end
}