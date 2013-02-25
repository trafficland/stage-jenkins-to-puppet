package globals.playframework

import play.api.Logger._
import util.OptionHelper._
import play.api.libs.concurrent.Akka
import akka.actor.{ActorRef, ActorSystem, Props}
import util.actors.fsm.CancellableMapFSM
import util.actors.{ScriptExecutorActor, ValidatorActor}
import concurrent.ExecutionContext.Implicits._
import scala.Some
import globals._

abstract class AbstractActors extends IActors {

  implicit lazy val playApp = play.api.Play.current
  implicit lazy val configuration = play.api.Play.configuration
  lazy val ourlogger = logger

  lazy val delayMilli = getOptionOrDefault(configuration.getInt("actor.schedule.delayMilli"), 1000)

  def createActors(): Unit = {
    Akka.system.actorOf(Props(() => new CancellableMapFSM(delayMilli)).withDispatcher("akka.actor.default-dispatcher"), name = ActorsProvider.scheduleName)
    Akka.system.actorOf(Props(() => new ValidatorActor(global, ActorsProvider)).withDispatcher("akka.actor.default-dispatcher"), name = ActorsProvider.validatorName)
    Akka.system.actorOf(Props(() => new ScriptExecutorActor(logger = Some(logger))).withDispatcher("akka.actor.default-dispatcher"), name = ActorsProvider.scriptorName)
  }

  def system: ActorSystem = Akka.system

  protected def getActorPath(actorName: String): String = system.toString + "/user/%s".format(actorName)

  def getActor(actorName: String): ActorRef = system.actorFor(getActorPath(actorName))

}
