package globals

import concurrent.ExecutionContext.Implicits._
import play.api.Logger._
import util.actors.{ScriptExecutorActor, ValidatorActor, fsm}
import util.actors.fsm.CancellableMapFSM
import util.OptionHelper._
import akka.actor.{ActorRef, ActorSystem, ActorPath, Props}
import play.api.libs.concurrent.Akka
import org.scalatest.mock.MockitoSugar.mock

abstract class AbstractActors extends IActors {

  implicit lazy val playApp = play.api.Play.current
  implicit lazy val configuration = play.api.Play.configuration
  lazy val ourlogger = logger

  lazy val delayMilli = getOptionOrDefault(configuration.getInt("actor.schedule.delayMilli"), 1000)

  def createActors(): Unit = {
    Akka.system.actorOf(Props(() => new CancellableMapFSM(delayMilli)).withDispatcher("akka.actor.default-dispatcher"), name = ActorsProvider.scheduleName)
    Akka.system.actorOf(Props(() => new ValidatorActor(global)).withDispatcher("akka.actor.default-dispatcher"), name = ActorsProvider.validatorName)
    Akka.system.actorOf(Props(() => new ScriptExecutorActor()).withDispatcher("akka.actor.default-dispatcher"), name = ActorsProvider.scriptorName)
  }

  def system: ActorSystem = Akka.system

  protected def getActorPath(actorName: String): String = "akka://%s127.0.0.1:2552/user/%s".format(system.name, actorName)

  def getActor(actorName: String): ActorRef = system.actorFor(getActorPath(actorName))

}


trait IActors {
  def ourlogger: org.slf4j.Logger

  def delayMilli: Int

  def createActors(): Unit

  def system: ActorSystem

  protected def getActorPath(actorName: String): String

  def getActor(actorName: String): ActorRef
}

abstract class AbstractMockActors extends IActors {
  def ourlogger = mock[org.slf4j.Logger]

  def delayMilli = 1000

  def createActors() {}

  override def system = mock[ActorSystem]

  protected def getActorPath(actorName: String): String = ""

  def getActor(actorName: String) = mock[ActorRef]
}

trait IActorsProvider {
  def actors(isMock: Boolean = false): IActors
}

trait IActorsProviderImpl extends IActorsProvider {

  protected object Actors extends AbstractActors

  protected object MockActors extends AbstractMockActors

  def actors(isMock: Boolean = false): IActors =
    if (isMock)
      MockActors
    else
      Actors
}

object ActorsProvider extends IActorsProviderImpl {
  val scriptorName = "scriptor"
  val scheduleName = "scheduler"
  val validatorName = "validator"
}