package util.actors.http

import akka.actor.Actor
import spray.util.SprayActorLogging
import spray.http._
import HttpMethods._
import spray.http._
import globals._
import spray.can.client.{DefaultHttpClient, HttpDialog}
import spray.http.HttpRequest
import models.mongo.reactive.ActorState
import models.mongo.reactive.ActorState._
import spray.json.{BasicFormats}
import concurrent.ExecutionContext.Implicits.global

class SystemStateHttpActor(provider: IActorsProvider, serviceUrl: String)
  extends Actor
  with SprayActorLogging with IActorNames with BasicFormats {

  import util.actors.fsm.CancellableMapFSMDomainProvider.domain._

  val httpClient = DefaultHttpClient(provider.actors().system)

  def receive = {
    case HttpRequest(GET, "/actorHook/scheduled", _, _, _) =>
      provider.actors().getActor(scheduleName) ! Status
      sender ! HttpResponse.apply(status = StatusCodes.Accepted)
    case Batch(map) =>
      //send state to actors repository service
      val state = stringBoolMapFormat.write(map.map(m => (m._1 -> m._2.isCancelled)))
        .asJsObject("unable to convert!").compactPrint
      val responseF =
        HttpDialog(httpClient, serviceUrl)
          .send(
          HttpRequest(method = HttpMethods.POST,
            uri = "/actors/scheduled",
            headers = HttpHeaders.`Content-Type`(ContentType.`application/json`) :: Nil,
            entity = HttpEntity {
              Some {
                HttpBody(ContentType.`application/json`,
                  jsonSprayFormat.write(
                    ActorState("scheduler",
                      true,
                      state)
                  ).compactPrint)
              }
            })
        )
          .end
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
}