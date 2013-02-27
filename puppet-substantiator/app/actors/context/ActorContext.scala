package actors.context

import akka.actor._

trait IActorContext {
  def ourlogger: org.slf4j.Logger

  def delayMilli: Int

  def createActors(): Unit

  def system: ActorSystem

  protected def getActorPath(actorName: String): String

  def getActor(actorName: String): ActorRef
}

trait IActorContextProvider {
  def actors(): IActorContext
}

trait IActorNames{
  val scriptorName = "scriptor"
  val scheduleName = "scheduler"
  val validatorName = "validator"
  val httpStateHandlerName= "httpStateHandler"
  val httpServerName= "http-server"
}