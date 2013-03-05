package controllers

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import org.scalatest.mock.MockitoSugar.mock
import org.slf4j.Logger
import util.playframework.{IPlaySpecHelper, LiveTestServer}

trait IScriptControllerTest {
  def ourlogger: Logger

  lazy val scriptPathAndName: String = ""
}

object ScriptControllerFactory {
  val defaultScript = "./resource/test.sh"

  def create(scriptPath: String = defaultScript, useOriginalLogger: Boolean = false): ScriptController = {
    if (!useOriginalLogger)
      new ScriptController() with IScriptControllerTest {
        override lazy val ourlogger: Logger = mock[Logger]
        override lazy val scriptPathAndName: String = scriptPath
      }
    else
      new ScriptController() with IScriptControllerTest {
        override lazy val ourlogger: Logger = play.api.Logger.logger
        override lazy val scriptPathAndName: String = scriptPath
      }
  }
}

class ScriptControllerIntegrationSpec extends Specification with IPlaySpecHelper with LiveTestServer {
  override protected lazy val fakeApp = Some(createFakeApp(testName))

  import ScriptControllerFactory._

  override lazy val testName = "test-akka-mock"
//  actors.context.playframework.ActorContextProvider.actors().createActors()

  "ScriptController" should {
    "send 200 config script hosted controller" in {
      val optResult = route(FakeRequest(GET, "/rollback/appName/8080"))
      optResult match {
        case Some(result) =>
          status(result) shouldEqual OK
          contentAsString(result) must contain("Executed Rollback script!")
        case None =>
          "have result" shouldEqual ("no result")
      }

      "send 500 missing wrong script" in {
        val mockController = create("/assets/junk/file.sh")
        val action = mockController.rollBack("appName", 8080)
        val result = checkForAsyncResult(action.apply(FakeRequest(GET, "/rollback/appName/8080")))
        status(result) shouldEqual (INTERNAL_SERVER_ERROR)
        contentAsString(result) shouldEqual ("Script not Found!")
      }
      "send 200 config script correct" in {
        val mockController = create(defaultScript, true)
        val action = mockController.rollBack("appName", 8080)
        val result = checkForAsyncResult(action.apply(FakeRequest(GET, "/rollback/appName/8080")))
        status(result) shouldEqual (OK)
        contentAsString(result) shouldEqual ("Executed Rollback script!")
      }
    }
  }
  step(stopServer())
}