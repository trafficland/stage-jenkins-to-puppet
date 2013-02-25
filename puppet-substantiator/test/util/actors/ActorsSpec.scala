package util.actors

import akka.actor._
import akka.testkit._
import org.scalatest._
import org.scalatest.matchers.{ShouldMatchers, MustMatchers}
import util.IPlaySpecHelper
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
import globals.playframework.ActorsProvider
import ActorsProvider._


class ActorsSpec(_system: ActorSystem)
  extends TestKit(_system) with ImplicitSender
  with WordSpec with MustMatchers with BeforeAndAfterAll
  with BeforeAndAfter with ShouldMatchers with IPlaySpecHelper {

  def this() = this(ActorSystem("ActorsSpec"))

  override def afterAll {
    system.shutdown()
  }

  import ValidatorActor._


  "SetTarget of schedule actor should return batch " should {
    "batch could should match" in {
      10.milliseconds.dilated
      TestActorRef
      import fsm.CancellableMapFSMDomainProvider.domain._
      createRunningApp("test") {
        actors().createActors()
        val schedule = actors().getActor("scheduler")
        val validator = actors().getActor("validator")
        schedule ! SetTarget(Some(testActor)) // initialize scheduler
        validator ! StartValidation(60000, TestEvaluate(false, "test1"), Akka.system)
        validator ! StartValidation(60000, TestEvaluate(true, "test2"), Akka.system)
        validator ! StartValidation(60000, TestEvaluate(true, "test3"), Akka.system)
        val spin = (receiveWhile(5 seconds, 5 seconds) {
          case a: Any => a
        })
        //need to wait for data to get inserted
        schedule ! Status
        val rec = (receiveWhile(30 seconds, 5 seconds) {
          case b: Batch =>
            Some(b)
          case _ => None
        }).flatMap(b => b)
        rec.size should be(1)
        rec.head.multiple.size should be(3)
      }
    }
  }
}