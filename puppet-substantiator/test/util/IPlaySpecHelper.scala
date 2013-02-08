package util

import play.api.test._
import play.api.test.Helpers._
import java.io.File
import play.api.Configuration
import com.typesafe.config.{ConfigFactory, Config}


trait IPlaySpecHelper {

  trait ITestConfig {
    def configuration: Configuration
  }

  def createRunningApp[T](configLocation: String = "")(run: => T): T = {
    configLocation match {
      case "" => running(FakeApplication())(run)
      case "test" => running(new FakeApplication() with ITestConfig {
        override def configuration: Configuration = Configuration(ConfigFactory.parseFile(new File("conf/test.conf")))
      })(run)
      case s: String => running(FakeApplication(new File(s)))(run)
      case _ => running(FakeApplication())(run)
    }
  }


}