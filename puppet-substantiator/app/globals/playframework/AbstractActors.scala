package globals.playframework

import play.api.Logger._
import util.OptionHelper._
import play.api.libs.concurrent.Akka
import akka.actor.{ActorRef, ActorSystem, Props}
import util.actors.fsm.CancellableMapFSM
import util.actors.http.SystemStateHttpActor
import util.actors.{ScriptExecutorActor, ValidatorActor}
import concurrent.ExecutionContext.Implicits._
import scala.Some
import globals._
import spray.can.server.SprayCanHttpServerApp


abstract class AbstractActors extends IActors with SprayCanHttpServerApp {

  implicit lazy val playApp = play.api.Play.current
  implicit lazy val configuration = play.api.Play.configuration
  lazy val ourlogger = logger

  lazy val delayMilli = getOptionOrDefault(configuration.getInt("actor.schedule.delayMilli"), 1000)

  lazy val listeningInterface = getOptionOrDefault(configuration.getString("actor.httpServer.listeningInterface"),"127.0.0.1")
  lazy val listeningPort = getOptionOrDefault(configuration.getInt("actor.httpServer.listeningPort"),64800)

  def createActors(): Unit = {
    val scheduler= system.actorOf(Props(() => new CancellableMapFSM(delayMilli)).withDispatcher("akka.actor.default-dispatcher"), name = ActorsProvider.scheduleName)
    system.actorOf(Props(() => new ValidatorActor(global, ActorsProvider)).withDispatcher("akka.actor.default-dispatcher"), name = ActorsProvider.validatorName)
    system.actorOf(Props(() => new ScriptExecutorActor(logger = Some(logger))).withDispatcher("akka.actor.default-dispatcher"), name = ActorsProvider.scriptorName)

    val httpActor = system.actorOf(Props[SystemStateHttpActor],name=ActorsProvider.httpServerName)
    newHttpServer(httpActor) ! Bind(interface = listeningInterface, port = listeningPort)

    import util.actors.fsm.CancellableMapFSMDomainProvider.domain._
    scheduler ! SetTarget(Some(httpActor)) // initialize scheduler
  }

  override lazy val system: ActorSystem = Akka.system

  protected def getActorPath(actorName: String): String = system.toString + "/user/%s".format(actorName)

  def getActor(actorName: String): ActorRef = system.actorFor(getActorPath(actorName))

}
