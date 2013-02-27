package globals.playframework

import play.api.Logger._
import util.OptionHelper._
import play.api.libs.concurrent.Akka
import akka.actor.{ActorRef, ActorSystem, Props}
import util.actors.fsm.CancellableMapFSM
import util.actors.http.SystemStateHttpActor
import util.actors.{ScriptExecutorActor, ValidatorActor}
import concurrent.ExecutionContext.Implicits._
import globals._
import spray.can.server.{HttpServer, ServerSettings, SprayCanHttpServerApp}
import controllers.routes
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import concurrent.Await
import concurrent.duration._

abstract class AbstractActors extends IActors with SprayCanHttpServerApp with Controller {

  implicit lazy val playApp = play.api.Play.current
  implicit lazy val configuration = play.api.Play.configuration
  lazy val ourlogger = logger

  lazy val delayMilli = getOptionOrDefault(configuration.getInt("actor.schedule.delayMilli"), 1000)

  lazy val listeningInterface = getOptionOrDefault(configuration.getString("actor.httpServer.listeningInterface"), "127.0.0.1")
  lazy val listeningPort = getOptionOrDefault(configuration.getInt("actor.httpServer.listeningPort"), 64800)

  def createActors(): Unit = {
    val dispatcherName = "akka.actor.default-dispatcher"
    val scheduler = system.actorOf(Props(() => new CancellableMapFSM(delayMilli)).withDispatcher(dispatcherName), name = ActorsProvider.scheduleName)
    system.actorOf(Props(() => new ValidatorActor(global, ActorsProvider)).withDispatcher(dispatcherName), name = ActorsProvider.validatorName)
    system.actorOf(Props(() => new ScriptExecutorActor(logger = Some(logger))).withDispatcher(dispatcherName), name = ActorsProvider.scriptorName)

    val absUrl = getOptionOrDefault(configuration.getString("playBaseUrl"),"localhost:9000")
    val httpActor = system.actorOf(Props(() =>
      new SystemStateHttpActor(ActorsProvider, absUrl))
      .withDispatcher(dispatcherName), name = ActorsProvider.httpStateHandlerName)

    val server = newHttpServer(httpActor)
    server ! HttpServer.Unbind
    server ! Bind(interface = listeningInterface, port = listeningPort)

    import util.actors.fsm.CancellableMapFSMDomainProvider.domain._
    scheduler ! SetTarget(Some(httpActor)) // initialize scheduler
  }

  override lazy val system: ActorSystem = Akka.system

  protected def getActorPath(actorName: String): String = system.toString + "/user/%s".format(actorName)

  def getActor(actorName: String): ActorRef = system.actorFor(getActorPath(actorName))

}
