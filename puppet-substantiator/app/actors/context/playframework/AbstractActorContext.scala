package actors.context.playframework

import play.api.Logger._
import util.OptionHelper._
import play.api.libs.concurrent.Akka
import akka.actor.{ActorRef, ActorSystem, Props}
import actors.fsm.CancellableMapFSM
import concurrent.ExecutionContext.Implicits._
import play.api.mvc._
import actors._
import actors.context.IActorContext
import util.PlaySettings
import PlaySettings._

abstract class AbstractActorContext extends IActorContext with Controller {

  implicit lazy val playApp = play.api.Play.current
  implicit lazy val configuration = play.api.Play.configuration
  lazy val ourlogger = logger

  lazy val delayMilli = getOptionOrDefault(configuration.getInt("actor.schedule.delaySeconds"), 1000)

  def createActors(): Unit = {
    val dispatcherName = "akka.actor.default-dispatcher"
    val scheduler = system.actorOf(Props(() => new CancellableMapFSM(delayMilli)).withDispatcher(dispatcherName), name = ActorContextProvider.scheduleName)
    system.actorOf(Props(() => new ValidatorActor(global, ActorContextProvider)).withDispatcher(dispatcherName), name = ActorContextProvider.validatorName)
    system.actorOf(Props(() => new ScriptExecutorActor(logger = Some(logger))).withDispatcher(dispatcherName), name = ActorContextProvider.scriptorName)

    val httpActor = system.actorOf(Props(() =>
      new SystemStateActor(ActorContextProvider, absUrl))
      .withDispatcher(dispatcherName), name = ActorContextProvider.actorStateHandlerName)

    import actors.fsm.CancellableMapFSMDomainProvider.domain._
    scheduler ! SetTarget(Some(httpActor)) // initialize scheduler
  }

  override lazy val system: ActorSystem = Akka.system

  protected def getActorPath(actorName: String): String = system.toString + "/user/%s".format(actorName)

  def getActor(actorName: String): ActorRef = system.actorFor(getActorPath(actorName))

}
