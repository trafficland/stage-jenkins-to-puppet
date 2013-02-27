package actors

import akka.actor._
import akka.testkit._
import org.scalatest._
import org.scalatest.matchers.{ShouldMatchers, MustMatchers}
import org.slf4j.Logger
import org.scalatest.mock.MockitoSugar.mock
import org.mockito.Mockito._
import org.mockito._
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.Some

class ScriptExecutorActorSpec(_system: ActorSystem)
  extends TestKit(_system) with ImplicitSender
  with WordSpec with MustMatchers with BeforeAndAfterAll
  with BeforeAndAfter with ShouldMatchers {

  def this() = this(ActorSystem("ScriptExecutorActorSpec"))

  override def afterAll {
    system.shutdown()
  }

  import ScriptExecutorActor._

  def create = new Cancellable {
    var localCancel = false

    def isCancelled = localCancel

    def cancel() {
      localCancel = true
    }
  }

  def initialize(logger: Logger):TestActorRef[ScriptExecutorActor] = {
    val doThrow = true
    val ref = TestActorRef(new ScriptExecutorActor(true, doThrow,Some(logger)))
    ref
  }

  def logger(isDebug: Boolean = false, doMockError: Boolean = false): Logger = {
    lazy val log = mock[Logger]
    when(log.isDebugEnabled).thenReturn(isDebug)
    log
  }

  "no script exist" should {
    "throw exception" in {
      lazy val log = logger(true, true)
      lazy val actorRef = initialize(log)
      intercept[Exception] {
        actorRef.receive(Script("", Seq("someApp")))
      }
    }
  }

  "script exist" should {
    "expected results from script log Debug" in {
      lazy val log = logger(true)
      lazy val stringCapture = ArgumentCaptor.forClass(classOf[String])

      val actorRef = initialize(log)
      val path = new java.io.File(".").getCanonicalPath
      actorRef.receive(Script(path + "/test/resources/test.sh", Seq("someApp")))

      val test = Await.result(future {
        Thread.sleep(2000)
        verify(log).debug(stringCapture.capture())
        val compare = stringCapture.getValue
        compare == "someApp\n"
      }, 3 seconds)

      test should be(true)
    }
  }
}