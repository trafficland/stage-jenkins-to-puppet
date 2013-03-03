package controllers

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import org.scalatest.mock.MockitoSugar.mock
import org.slf4j.Logger
import util.playframework.{IPlaySpecHelper, LiveTestServer}

trait IScriptControllerTest {
  def optScriptFileName: Option[String]

  def ourlogger: Logger
}

object ScriptControllerFactory {
  def create(optFileName: Option[String], useOriginal: Boolean = false): ScriptController = {
    if (!useOriginal)
      new ScriptController() with IScriptControllerTest {
        override lazy val optScriptFileName: Option[String] = optFileName
        override lazy val ourlogger: Logger = mock[Logger]
      }
    else
      new ScriptController() with IScriptControllerTest {
        override lazy val optScriptFileName: Option[String] = ScriptController.optScriptFileName
        override lazy val ourlogger: Logger = play.api.Logger.logger
      }
  }
}

class ScriptControllerIntegrationSpec extends Specification with IPlaySpecHelper with LiveTestServer {
  override protected lazy val fakeApp = Some(createFakeApp(testName))

  import ScriptControllerFactory._

  override lazy val testName = "test-akka-mock"

  "ScriptController" should {

    "send 200 config script hosted controller" in {
      val optResult = route(FakeRequest(GET, "/rollback/appName/8080"))
      optResult match {
        case Some(result) => status(result) must equalTo(OK)
        contentAsString(result) must contain("Execute Rollback script here!")
        case None =>
          "have result" must beEqualTo("no result")
      }
      "send 500 missing script" in {
        val mockController = create(None)
        val action = mockController.rollBack("appName", 8080)
        val result = checkForAsyncResult(action.apply(FakeRequest(GET, "/rollback/appName/8080")))

        status(result) must equalTo(INTERNAL_SERVER_ERROR)
        contentAsString(result) must equalTo("No script defined to look up!")
      }

      "send 500 missing wrong script" in {
        val mockController = create(Some("oops"))
        val action = mockController.rollBack("appName", 8080)
        val result = checkForAsyncResult(action.apply(FakeRequest(GET, "/rollback/appName/8080")))
        status(result) must equalTo(INTERNAL_SERVER_ERROR)
        contentAsString(result) must equalTo("Script not Found!")
      }
      "send 200 config script correct" in {
        val mockController = create(None, true)
        val action = mockController.rollBack("appName", 8080)
        val result = checkForAsyncResult(action.apply(FakeRequest(GET, "/rollback/appName/8080")))
        status(result) must equalTo(OK)
        contentAsString(result) must equalTo("Execute Rollback script here!")
      }
    }
  }
  step(stopServer())
}