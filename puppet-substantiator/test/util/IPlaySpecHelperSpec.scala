package util

import org.specs2.mutable._
import play.api.mvc._
import play.api.test.Helpers._

class IPlaySpecHelperSpec extends Specification with IPlaySpecHelper with Controller {

  "IPlaySpecHelper" should {

    "Be able to match result content of string type" in {
      val result = Ok("someResult")

      status(result) must equalTo(OK)
      contentAsString(result) must equalTo("someResult")
    }
    "Be able to match result content of Array[Byte] type" in {
      val result = Ok(Array[Byte](1))
      status(result) must equalTo(OK)
      contentAsBytes(result).toList must equalTo(Array[Byte](1).toList)
    }
  }
}
