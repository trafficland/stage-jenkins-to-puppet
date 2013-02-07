package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import controllers.AppInfo

class ApplicationSpec extends Specification {
  
  "Application" should {
    
    "send 404 on a bad request" in {
      running(FakeApplication()) {
        route(FakeRequest(GET, "/")) must beSome
      }
    }
    
    "render the index page" in {
      running(FakeApplication()) {
        val home = route(FakeRequest(GET, "/version")).get
        
        status(home) must equalTo(OK)
        contentAsString(home) must contain (AppInfo.version)
      }
    }
  }
}