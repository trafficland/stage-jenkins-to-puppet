package util

import play.api.test._
import play.api.test.Helpers._
import java.io.File
import play.api.Configuration
import com.typesafe.config.ConfigFactory
import play.api.mvc._
import scala.reflect.runtime.universe._


trait IPlaySpecHelper {
  implicit lazy val app = play.api.Play.current

  trait ITestConfig {
    def configuration: Configuration
  }

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

  def resultToStatusContentTuple[T: TypeTag](result: Result): (Int, Option[T]) = {
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