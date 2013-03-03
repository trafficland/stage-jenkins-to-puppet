package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import controllers.AppInfo
import util.playframework.{IPlaySpecHelper, LiveTestServer}

class ApplicationSpec extends Specification with IPlaySpecHelper with LiveTestServer {
  override protected lazy val fakeApp = Some(createFakeApp(testName))

  "Application" should {

    "send 404 on a bad request" in {
      route(FakeRequest(GET, "/")) must beSome
    }

    "render the index page" in {
      val home = route(FakeRequest(GET, "/version")).get

      status(home) must equalTo(OK)
      contentAsString(home) must contain(AppInfo.version)
    }
  }
}