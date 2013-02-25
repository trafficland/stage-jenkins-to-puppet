package util

import java.io.File
import play.api.Configuration
import com.typesafe.config.ConfigFactory
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee._
import concurrent._
import concurrent.duration._
import play.api.test._
import play.api.test.Helpers._


trait IPlaySpecHelper {
  implicit val localGlobal = concurrent.ExecutionContext.Implicits.global

  trait ITestConfig {
    def configuration: Configuration
  }

  lazy val testName = "test"

  def timeoutSeconds = 10 seconds

  def createRunningApp[T](configLocation: String = "")(run: => T): T = {
    configLocation match {
      case "" => running(FakeApplication())(run)
      case "test" => running(new FakeApplication() with ITestConfig {
        override def configuration: Configuration =
          super.configuration ++ Configuration(ConfigFactory.parseFile(new File("conf/test.conf")))

      })(run)
      case "test-akka-mock" => running(new FakeApplication() with ITestConfig {
        override def configuration: Configuration =
          super.configuration ++ Configuration(ConfigFactory.parseFile(new File("conf/test-akka-mock.conf")))

      })(run)
      case s: String => running(FakeApplication(new File(s)))(run)
      case _ => running(FakeApplication())(run)
    }
  }

  def resultToStatusContentTupleJsonErrors(anyResult: Result): (Int, Boolean) = {
    val result = checkForAsyncResult(anyResult)
    val content = contentAsString(result)
    val jsonContent = Json.parse(content)
    (status(result), (jsonContent \ "errors").asOpt[Boolean] == None)
  }

  def resultToFieldComparison(anyResult: Result, fieldToCheck: String, compareTo: String): Boolean = {
    resultToOptField(anyResult, fieldToCheck).get == compareTo
  }

  def resultToOptField(anyResult: Result, fieldToCheck: String): Option[String] = {
    val result = checkForAsyncResult(anyResult)
    val content = contentAsString(result)
    val jsonContent = Json.parse(content)
    (jsonContent \ fieldToCheck).asOpt[String]
  }

  def checkForAsyncResult(anyResult: Result): Result = {
    anyResult match {
      case AsyncResult(fut) =>
        Await.result[Result](fut, timeoutSeconds)
      case _ =>
        anyResult
    }
  }

  def handleChunkedResult(chunkedResult: ChunkedResult[String]): String = {
    var str = ""
    val buildList = Iteratee.fold[String, Unit](0) {
      (_, a) => str = str + a
    }
    val promisedIteratee = chunkedResult.chunks(buildList).asInstanceOf[Promise[Iteratee[String, Unit]]]
    val fut = for {
      now <- promisedIteratee.future
      unit <- now.run
    }
    yield (unit)
    Await.result(fut, timeoutSeconds * 5)
    new String(str)
  }

  def jsonStringToModelList[A](str: String)(implicit jsonReader: Reads[A]): List[A] = {
    Json.parse(str).asOpt[JsArray].get
      .value.flatMap(jsonReader.reads(_).asOpt).toList
  }

  def chunksToModelList[A](chunkedResult: ChunkedResult[String])(implicit jsonReader: Reads[A]): List[A] = {
    val string = handleChunkedResult(chunkedResult)
    jsonStringToModelList(string)
  }

  def await[T](fut: Future[T]): T = {
    Await.result[T](fut, timeoutSeconds * 2)
  }


}