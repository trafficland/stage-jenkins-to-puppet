package globals

import akka.actor._

trait IActors {
  def ourlogger: org.slf4j.Logger

  def delayMilli: Int

  def createActors(): Unit

  def system: ActorSystem

  protected def getActorPath(actorName: String): String

  def getActor(actorName: String): ActorRef
}

trait IActorsProvider {
  def actors(): IActors
}

trait IActorNames{
  val scriptorName = "scriptor"
  val scheduleName = "scheduler"
  val validatorName = "validator"
}