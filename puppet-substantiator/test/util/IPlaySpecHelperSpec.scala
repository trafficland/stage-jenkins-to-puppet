package util

import org.specs2.mutable._
import play.api.mvc._

class IPlaySpecHelperSpec extends Specification with IPlaySpecHelper with Controller{

  "IPlaySpecHelper" should {

    "Be able to match result content of string type" in {
      val result = Ok("someResult")
      val tuple = resultToStatusContentTuple[String](result)
      (tuple._1, tuple._2.get) must equalTo(OK, "someResult")
    }
    "Be able to match result content of Array[Byte] type" in {
      val result = Ok(Array[Byte](1))
      val tuple = resultToStatusContentTuple[Array[Byte]](result)
      (tuple._1,tuple._2.get.toList) must equalTo(OK, Array[Byte](1).toList)
    }
  }
}
