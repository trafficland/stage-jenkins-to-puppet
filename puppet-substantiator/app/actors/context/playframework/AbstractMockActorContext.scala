package actors.context.playframework

import akka.testkit.TestProbe
import play.api.libs.concurrent.Akka
import org.scalatest.mock.MockitoSugar._
import actors.context.IActorContext

abstract class AbstractMockActorContext extends IActorContext {
  implicit val app = play.api.Play.current
  final val probe = TestProbe()(Akka.system)

  def ourlogger = mock[org.slf4j.Logger]

  def delayMilli = 1000

  def createActors() {}

  override def system = probe.system

  protected def getActorPath(actorName: String): String = ""

  def getActor(actorName: String) = probe.ref
}
