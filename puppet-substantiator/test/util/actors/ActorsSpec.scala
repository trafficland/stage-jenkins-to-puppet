package util.actors

import _root_.util.evaluations.IEvaluate
import akka.actor._
import akka.testkit._
import org.scalatest._
import org.scalatest.matchers.{ShouldMatchers, MustMatchers}
import globals.Actors._
import util.IPlaySpecHelper
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
import scala.concurrent._
import scala.PartialFunction


class ActorsSpec(_system: ActorSystem)
  extends TestKit(_system) with ImplicitSender
  with WordSpec with MustMatchers with BeforeAndAfterAll
  with BeforeAndAfter with ShouldMatchers with IPlaySpecHelper {

  def this() = this(ActorSystem("ActorsSpec"))

  override def afterAll {
    system.shutdown()
  }

  import ValidatorActor._
  import _root_.util.actors.fsm.CancellableMapFSMDomainProvider.domain._

  case class TestEvaluate(name:String,doFail: Boolean) extends IEvaluate[String] {
    def evaluate = {
      if (doFail)
        Fail("fail")
      else
        Pass("pass")
    }

    var state: String = ""

    def failAction(result: String) {
      state = result
    }

    def passAction(result: String) {
      state = result
    }
  }

  "SetTarget of schedule actor should return batch " should {
    "send Add" in {
      10.milliseconds.dilated
             TestActorRef
      import fsm.CancellableMapFSMDomainProvider.domain._
      createRunningApp("test") {
        schedule ! SetTarget(Some(testActor)) // initialize scheduler
        validatorActorRef ! StartValidation(60000, TestEvaluate("test1",false), Akka.system)
        validatorActorRef ! StartValidation(60000, TestEvaluate("test2",true), Akka.system)
        validatorActorRef ! StartValidation(60000, TestEvaluate("test3",true), Akka.system)
        Await.result(future (Thread.sleep(3000)),5 seconds)
        //need to wait for data to get inserted
        schedule ! Status
        val rec = (receiveWhile(30 seconds, 0 seconds) {
          case b: Batch => Some(b)
          case _ => None
        }).flatMap(b => b)
        rec.size  should be(1)
        rec.head.multiple.size should be(3)
      }
    }
  }
}