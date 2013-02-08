package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import controllers.AppInfo
import _root_.util.IPlaySpecHelper

class ApplicationSpec extends Specification with IPlaySpecHelper{

  "Application" should {

    "send 404 on a bad request" in {
      createRunningApp() {
        route(FakeRequest(GET, "/")) must beSome
      }
    }

    "render the index page" in {
      createRunningApp() {
        val home = route(FakeRequest(GET, "/version")).get

        status(home) must equalTo(OK)
        contentAsString(home) must contain (AppInfo.version)
      }
    }
  }
}