package controllers

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.mvc.Result
import _root_.util.IPlaySpecHelper
import play.api.mvc.SimpleResult
import play.api.libs.iteratee.{Enumerator, Iteratee}
import concurrent._
import concurrent.duration.DurationInt
import org.scalatest.mock.MockitoSugar.mock
import org.slf4j.Logger

trait IScriptControllerTest {
  def optScriptFileName: Option[String]
  def ourlogger:Logger
}


class ScriptControllerIntegrationSpec extends Specification with IPlaySpecHelper {

  "ScriptController" should {

    val stringIteratee = Iteratee.fold[String, String]("") {
      (a, b) => b + a
    }

    def matchResult(result: Result): (Int, String) = {
      result match {
        case SimpleResult(header, body) =>
          (header.status,
            Await.result(body.asInstanceOf[Enumerator[String]].run[String](stringIteratee), DurationInt(2).seconds))
        case _ => (-1, "empty")
      }
    }

    "send 500 missing script" in {
      createRunningApp("test") {
        val mockedController = new ScriptController() with IScriptControllerTest {
          override lazy val optScriptFileName: Option[String] = None
          override lazy val ourlogger:Logger = mock[Logger]
        }

        val action = mockedController.rollBack("appName")
        val status = matchResult(action.apply(FakeRequest(GET, """/rollback/"appName"""")))
        status must equalTo(INTERNAL_SERVER_ERROR,"No script defined to look up!")
      }
    }

    "send 500 missing wrong script" in {
      createRunningApp("test") {
        val mockedController = new ScriptController() with IScriptControllerTest {
          override lazy val optScriptFileName: Option[String] = Some("oops")
          override lazy val ourlogger:Logger = mock[Logger]
        }

        val action = mockedController.rollBack("appName")
        val status = matchResult(action.apply(FakeRequest(GET, """/rollback/"appName"""")))
        status must equalTo(INTERNAL_SERVER_ERROR, "Script not Found!")
      }
    }
  }
}