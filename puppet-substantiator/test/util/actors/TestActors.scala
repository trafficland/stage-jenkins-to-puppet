package util.actors

import akka.testkit.TestKit
import globals.IActors
import akka.actor.ActorRef
import org.scalatest.mock.MockitoSugar._

case class TestActors(kit:TestKit) extends IActors {
  override def system = kit.system

  override protected def getActorPath(actorName: String): String = system.toString + "/user/%s".format(actorName)

  override def getActor(actorName: String): ActorRef = kit.testActor

  override def createActors() = {}

  def ourlogger = mock[org.slf4j.Logger]

  def delayMilli = 5000
}
