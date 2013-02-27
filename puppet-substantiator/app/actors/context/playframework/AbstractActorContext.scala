package actors.context.playframework

import play.api.Logger._
import util.OptionHelper._
import play.api.libs.concurrent.Akka
import akka.actor.{ActorRef, ActorSystem, Props}
import actors.fsm.CancellableMapFSM
import actors.http.SystemStateHttpActor
import concurrent.ExecutionContext.Implicits._
import spray.can.server.{HttpServer, SprayCanHttpServerApp}
import play.api.mvc._
import actors.{ValidatorActor, ScriptExecutorActor}
import actors.context.IActorContext

abstract class AbstractActorContext extends IActorContext with SprayCanHttpServerApp with Controller {

  implicit lazy val playApp = play.api.Play.current
  implicit lazy val configuration = play.api.Play.configuration
  lazy val ourlogger = logger

  lazy val delayMilli = getOptionOrDefault(configuration.getInt("actor.schedule.delaySeconds"), 1000)

  lazy val listeningInterface = getOptionOrDefault(configuration.getString("actor.httpServer.listeningInterface"), "127.0.0.1")
  lazy val listeningPort = getOptionOrDefault(configuration.getInt("actor.httpServer.listeningPort"), 64800)

  def createActors(): Unit = {
    val dispatcherName = "akka.actor.default-dispatcher"
    val scheduler = system.actorOf(Props(() => new CancellableMapFSM(delayMilli)).withDispatcher(dispatcherName), name = ActorContextProvider.scheduleName)
    system.actorOf(Props(() => new ValidatorActor(global, ActorContextProvider)).withDispatcher(dispatcherName), name = ActorContextProvider.validatorName)
    system.actorOf(Props(() => new ScriptExecutorActor(logger = Some(logger))).withDispatcher(dispatcherName), name = ActorContextProvider.scriptorName)

    val absUrl = getOptionOrDefault(configuration.getString("playBaseUrl"),"localhost:9000")
    val httpActor = system.actorOf(Props(() =>
      new SystemStateHttpActor(ActorContextProvider, absUrl))
      .withDispatcher(dispatcherName), name = ActorContextProvider.httpStateHandlerName)

    val server = newHttpServer(httpActor)
    server ! HttpServer.Unbind
    server ! Bind(interface = listeningInterface, port = listeningPort)

    import actors.fsm.CancellableMapFSMDomainProvider.domain._
    scheduler ! SetTarget(Some(httpActor)) // initialize scheduler
  }

  override lazy val system: ActorSystem = Akka.system

  protected def getActorPath(actorName: String): String = system.toString + "/user/%s".format(actorName)

  def getActor(actorName: String): ActorRef = system.actorFor(getActorPath(actorName))

}
