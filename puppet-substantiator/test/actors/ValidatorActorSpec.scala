package actors

import akka.actor._
import akka.testkit._
import context.{IActorNames, IActorContextProvider, IActorContext}
import org.scalatest._
import org.scalatest.matchers.{ShouldMatchers, MustMatchers}
import scala.concurrent.ExecutionContext.Implicits.global


class ValidatorActorSpec(_system: ActorSystem)
  extends TestKit(_system) with ImplicitSender
  with WordSpec with MustMatchers with BeforeAndAfterAll
  with BeforeAndAfter with ShouldMatchers with IActorNames {

  def this() = this(ActorSystem("ValidatorActorSpec"))

  lazy val kitToPass: TestKit = this

  object TestProvider extends IActorContextProvider with IActorNames {
    lazy val validatorRef = TestActorRef(new ValidatorActor(global, TestProvider), name = validatorName)

    def actors() = new TestActors(kitToPass) with IActorContext {
      override def getActor(actorName: String): ActorRef = {
        if (validatorName == actorName)
          validatorRef
        else
          super.getActor(actorName)
      }
    }
  }

  override def afterAll {
    system.shutdown()
  }

  import ValidatorActor._
  import actors.fsm.CancellableMapFSMDomainProvider.domain._

  "StartValidation " should {
    "send Add" in {
      lazy val actorRef = TestProvider.actors().getActor(validatorName)
      val test = TestEvaluate(false)
      actorRef ! StartValidation(1000, test, system)
      expectMsgType[Add]
      test.evaluate()
      test.state should be("pass")

    }
  }
  "TickValidation " should {
    "send remove" in {
      lazy val actorRef = TestProvider.actors().getActor(validatorName)
      val test = TestEvaluate(true)
      actorRef ! TickValidation(test)
      expectMsgType[Remove]
      test.evaluate()
      test.state should be("fail")

    }
  }
}