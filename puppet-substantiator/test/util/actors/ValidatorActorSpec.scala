package util.actors

import _root_.util.evaluations.IEvaluate
import akka.actor._
import akka.testkit._
import org.scalatest._
import org.scalatest.matchers.{ShouldMatchers, MustMatchers}
import scala.concurrent.ExecutionContext.Implicits.global


class ValidatorActorSpec(_system: ActorSystem)
  extends TestKit(_system) with ImplicitSender
  with WordSpec with MustMatchers with BeforeAndAfterAll
  with BeforeAndAfter with ShouldMatchers {

  def this() = this(ActorSystem("ValidatorActorSpec"))

  override def afterAll {
    system.shutdown()
  }

  import ValidatorActor._
  import _root_.util.actors.fsm.CancellableMapFSMDomainProvider.domain._

  def initialize(): TestActorRef[ValidatorActor] = {
    TestActorRef(new ValidatorActor(global, this.testActor))
  }

  "StartValidation " should {
    "send Add" in {
      lazy val actorRef = initialize()
      val test = TestEvaluate(false)
      actorRef ! StartValidation(1000, test, system)
      expectMsgType[Add]
      test.run()
      test.state should be("pass")

    }
  }
  "TickValidation " should {
    "send remove" in {
      lazy val actorRef = initialize()
      val test = TestEvaluate(true)
      actorRef ! TickValidation(test)
      expectMsgType[Remove]
      test.run()
      test.state should be("fail")

    }
  }
}