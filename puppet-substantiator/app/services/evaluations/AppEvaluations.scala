package services.evaluations

import _root_.util.PlaySettings
import util.evaluations._
import util.FutureHelper._
import models.mongo.reactive._
import play.api.libs.ws.WS
import services.repository.mongo.reactive.impls.IAppsRepository
import play.api.Logger._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json


object AppEvaluateHelpers extends IEvaluateDomain[App] {
  def filterOutImmediateForwardSlash(testUrl: String): String = {
    testUrl.startsWith("/") match {
      case true =>
        testUrl.replaceFirst("/", "")
      case false =>
        testUrl
    }
  }

  def futOneBoolToPassFail(oneFutBool: Future[Boolean], app: App): Future[IEvaluated[App]] = {
    for {
      bool <- oneFutBool
    } yield {
      if (bool)
        Pass(app)
      else
        Fail(app)
    }
  }

  def testApp(app: App) = app.actualCluster.map {
    machine =>
      app.port match {
        case Some(realPort) =>
          val url = "http://%s:%s/%s".format(machine.machineName, realPort, filterOutImmediateForwardSlash(app.testUrl))
          val logMsg = "Testing Machine at %s".format(url)
          Console.println(logMsg)
          logger.debug(logMsg)
          testMachine(app, machine.machineName, WS.url(url))
        case None =>
          val url = "http://%s/%s".format(machine.machineName, filterOutImmediateForwardSlash(app.testUrl))
          val logMsg = "Testing Machine at %s".format(url)
          Console.println(logMsg)
          logger.debug(logMsg)
          testMachine(app, machine.machineName, WS.url(url))
      }
  }

  def testMachine(appToUpdate: App, machineName: String, request: WS.WSRequestHolder): Future[Option[AppMachineState]] = {
    val optFutResponse = request.get()
      .map(Some(_))
      .recover {
      case _ => None
    }
    for {
      optResponse <- optFutResponse
    } yield {
      optResponse match {
        case Some(result) =>
          val logStr = "------------- Machine: %s got the following response: %s -------------".format(machineName, result.body)
          Console.print(logStr)
          logger.info(logStr)
          Some(AppMachineState(machineName, Some(result.body)))
        case None =>
          val logStr = "No Response from machine %s".format(machineName)
          Console.println(logStr)
          logger.info(logStr)
          None
      }
    }
  }
}

trait AbstractAppEvaluate extends IEvaluate[App] {
  def handleFuturePassFail(futFailPass: Future[PassFail]) = {
    for {
      failPass <- futFailPass
    } yield {
      failPass match {
        case Fail(someApp) =>
          failAction(someApp)
        case Pass(someApp) =>
          passAction(someApp)
        case _ =>
      }
    }
  }
}

case class AppEvaluate(app: App, repo: IAppsRepository) extends AbstractAppEvaluate with IAppReadersWriters {
  def evaluate()(implicit context: ExecutionContext): Future[IEvaluated[App]] = {
    val futFailPass = app.id match {
      case Some(id) =>
        for {
          eitherAppOrException <- repo.get(id)
        } yield {
          eitherAppOrException match {
            case Left(optApp) =>
              optApp match {
                case Some(upApp) =>
                  if (upApp.actualCluster.forall(appMachine =>
                    appMachine.actual match {
                      case Some(actualState) =>
                        val result = actualState.contains(app.expected)
                        result match {
                          case true =>
                            val passStr = "Version Check for machine %s in %s application PASSED for %s version! Actual value is %s !".format(appMachine.machineName, app.name, app.expected, actualState)
                            Console.println()
                            Console.println(passStr)
                            logger.debug(passStr)
                          case false =>
                            val failStr = "Version Check for machine %s in %s application FAILED for %s version! Actual value is %s !".format(appMachine.machineName, app.name, app.expected, actualState)
                            Console.println()
                            Console.println(failStr)
                            logger.debug(failStr)
                        }
                        result
                      case None =>
                        false
                    }
                  ))
                    Pass(upApp)
                  else {
                    Fail(upApp)
                  }
                case None =>
                  Fail(app)
              }
            case Right(ex) =>
              if (logger.isDebugEnabled)
                logger.debug("Getting app failed! Failing at AppEvaluate!")
              Fail(app)
          }
        }
      case None =>
        future(Fail(app))
    }
    handleFuturePassFail(futFailPass)
    futFailPass
  }

  lazy val rollBackUrl = "http://" + PlaySettings.absUrl + "/rollback"

  def failAction(result: App) = WS.url(rollBackUrl).post(Json.toJson(result))


  def passAction(result: App) {
    //send email on pass?
  }

  def name = app.name + "Validate"

}

case class QueryMachinesUpdateAppEvaluate(app: App, repo: IAppsRepository) extends AbstractAppEvaluate {

  import AppEvaluateHelpers._

  def evaluate()(implicit context: ExecutionContext): Future[IEvaluated[App]] =
    app.id match {
      case Some(id) =>
        val listOfFutOptAppMachines = query
        val futureBools = listOfFutOptAppMachines.map {
          futOptAppMachine: Future[Option[AppMachineState]] =>
            for {
              optAppMachine <- futOptAppMachine
              latestSomeApp <- futureEitherOfOptionExceptionToOption(repo.get(id))
              optApp <- latestSomeApp match {
                case Some(latestApp) =>
                  optAppMachine match {
                    case Some(appMachine) =>
                      futureEitherOfOptionExceptionToOption(repo.update(latestApp.copy(actualCluster =
                        appMachine :: latestApp.actualCluster.filter(_.machineName != appMachine.machineName))))
                    case None =>
                      future(None)
                  }
                case None =>
                  future(None)
              }
            } yield {
              optApp match {
                case Some(updated) =>
                  true
                case None =>
                  false
              }
            }
        }
        futOneBoolToPassFail(futureBools.reduce((futBool1, futBool2) =>
          for {
            nowBool1 <- futBool1
            nowBool2 <- futBool2
          } yield (nowBool1 && nowBool2)
        ), app)
      case None =>
        future(Fail(app))
    }

  protected def query = testApp(app)

  def failAction(result: App) = {
  }


  def passAction(result: App) {
  }

  def name = app.name + "Query"


}
