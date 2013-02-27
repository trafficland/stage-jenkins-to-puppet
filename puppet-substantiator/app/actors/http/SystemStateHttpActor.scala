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
import concurrent.ExecutionContext.Implicits.global
import actors.context.{IActorNames, IActorContextProvider}
import actors.fsm.ICancellableDelay

class SystemStateHttpActor(provider: IActorContextProvider, serviceUrl: String)
  extends Actor
  with SprayActorLogging with IActorNames with BasicFormats {

  import actors.fsm.CancellableMapFSMDomainProvider.domain._

  val host = serviceUrl.split(':')(0)
  val port = serviceUrl.split(':')(1).toInt

  val httpClient = DefaultHttpClient(provider.actors().system)

  def receive = {
    case HttpRequest(GET, "/actorHook/routes", _, _, _) =>
      sender ! HttpResponse.apply(status = StatusCodes.Accepted,
        entity = HttpBody(MediaTypes.`text/html`,
          <html>
            <body>
              <h1>Actor Hook hit
                <i>spray-can</i>
                !</h1>
              <p>Defined resources:</p>
              <ul>
                <li>
                  <a href="/actorHook/routes">/actorHook/routes</a>
                </li>
                <li>
                  <a href="/actorHook/scheduled">/actorHook/scheduled</a>
                </li>
              </ul>
            </body>
          </html>.toString
        ))
    case HttpRequest(GET, "/actorHook/scheduled", _, _, _) =>
      provider.actors().getActor(scheduleName) ! Status
      sender ! HttpResponse.apply(status = StatusCodes.Accepted,
        entity = HttpBody(MediaTypes.`text/html`,
          <html>
            <body>
              <h1>Actor Hook hit from
                <i>spray-can</i>
                !</h1>
            </body>
          </html>.toString
        ))
    case Batch(map) => handleScheduled(map)
    case _ =>
      sender ! HttpResponse.apply(status = StatusCodes.Accepted,
        entity = HttpBody(MediaTypes.`text/html`,
          <html>
            <body>
              <h1>Actor Hook hit
                <i>spray-can</i>
                !</h1>
              <p>Resource Not FOUND!!</p>
            </body>
          </html>.toString
        ))
  }

  def handleScheduled(map: Map[String, ICancellableDelay]) =
    map.size > 0 match {
      case true => saveScheduled(map)
      case false => deleteScheduled(map)

    }

  def saveScheduled(map: Map[String, ICancellableDelay]) = {
    val state = map.map(m => (("{app:" + m._1 + ',' + "isCancelled:" + m._2.isCancelled + '}'))).toList.reduce(_ + "," + _)
    val ent = HttpEntity {
      Some {
        HttpBody(ContentType.`application/json`,
          jsonSprayFormat.write(
            ActorState("scheduler",
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
    HttpDialog(httpClient, host, port).send(HttpRequest(method = HttpMethods.DELETE, uri = "/actors/scheduled")).end
}