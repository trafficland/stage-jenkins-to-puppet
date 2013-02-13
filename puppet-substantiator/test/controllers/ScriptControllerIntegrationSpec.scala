package controllers

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import _root_.util.IPlaySpecHelper
import org.scalatest.mock.MockitoSugar.mock
import org.slf4j.Logger

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
        override lazy val ourlogger: Logger = ScriptController.ourlogger
      }
  }
}

class ScriptControllerIntegrationSpec extends Specification with IPlaySpecHelper {

  import ScriptControllerFactory._

  "ScriptController" should {

    "send 200 config script hosted controller" in {
      createRunningApp("test") {
        val result = route(FakeRequest(GET, """/rollback/"appName"""")).get
        status(result) must equalTo(OK)
        contentAsString(result) must contain("Execute Rollback script here!")
      }
      "send 500 missing script" in {
        createRunningApp("test") {
          val mockController = create(None)
          val action = mockController.rollBack("appName")
          val status = resultToStatusContentTuple[String](action.apply(FakeRequest(GET, """/rollback/"appName"""")))
          status must equalTo(INTERNAL_SERVER_ERROR, Some("No script defined to look up!"))
        }
      }

      "send 500 missing wrong script" in {
        createRunningApp("test") {
          val mockController = create(Some("oops"))
          val action = mockController.rollBack("appName")
          val status = resultToStatusContentTuple[String](action.apply(FakeRequest(GET, """/rollback/"appName"""")))
          status must equalTo(INTERNAL_SERVER_ERROR, Some("Script not Found!"))
        }
      }
      "send 200 config script correct" in {
        createRunningApp("test") {
          val mockController = create(None, true)
          val action = mockController.rollBack("appName")
          val status = resultToStatusContentTuple[String](action.apply(FakeRequest(GET, """/rollback/"appName"""")))
          status must equalTo(OK, Some("Execute Rollback script here!"))
        }
      }
    }
  }
}