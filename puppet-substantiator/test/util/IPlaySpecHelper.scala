package util

import play.api.test._
import play.api.test.Helpers._
import java.io.File
import play.api.Configuration
import com.typesafe.config.ConfigFactory
import play.api.mvc._
import scala.reflect.runtime.universe._
import play.api.libs.json.{JsValue, Json, Reads}
import concurrent._
import concurrent.ExecutionContext.global
import concurrent.duration._


trait IPlaySpecHelper {
  implicit lazy val app = play.api.Play.current
  implicit lazy val localGlobal = global

  trait ITestConfig {
    def configuration: Configuration
  }

  def timeoutSeconds = 2 seconds

  def createRunningApp[T](configLocation: String = "")(run: => T): T = {
    configLocation match {
      case "" => running(FakeApplication())(run)
      case "test" => running(new FakeApplication() with ITestConfig {
        override def configuration: Configuration =
          super.configuration ++ Configuration(ConfigFactory.parseFile(new File("conf/test.conf")))

      })(run)
      case s: String => running(FakeApplication(new File(s)))(run)
      case _ => running(FakeApplication())(run)
    }
  }

  def resultToStatusContentTuple[T: TypeTag](anyResult: Result, optJsonReader: Option[Reads[T]] = None): (Int, Option[T]) = {
    val result = checkForAsyncResult(anyResult)
    optJsonReader match {
      case Some(jsonReader) =>
        (status(result), Some(jsonReader.reads(Json.parse(contentAsString(result))).asInstanceOf[T]))
      case None =>
        typeOf[T] match {
          case t if t =:= typeOf[String] =>
            (status(result), Some(contentAsString(result).asInstanceOf[T]))
          case t if t =:= typeOf[Array[Byte]] =>
            (status(result), Some(contentAsBytes(result).asInstanceOf[T]))
          case _ =>
            (-1, None)
        }
    }
  }

  def resultToStatusContentTupleJsonErrors(anyResult: Result): (Int, Boolean) = {
    val result = checkForAsyncResult(anyResult)
    val content = contentAsString(result)
    val jsonContent = Json.parse(content)
    (status(result), (jsonContent \ "errors").asOpt[JsValue].isEmpty)
  }

  def checkForAsyncResult(anyResult: Result): Result = {
    anyResult match {
      case async: AsyncResult =>
        Await.result[Result](async.unflatten, timeoutSeconds)
      case _ =>
        anyResult
    }
  }


}