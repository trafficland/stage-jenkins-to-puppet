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
import scala.concurrent.duration._
import play.api.libs.json.Json


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
                            val passStr = "Version Check for %s application PASSED for %s version! Actual value is %s !".format(app.name, app.expected, actualState)
                            Console.println(passStr)
                            logger.debug(passStr)
                          case false =>
                            val failStr = "Version Check for %s application FAILED for %s version! Actual value is %s !".format(app.name, app.expected, actualState)
                            logger.debug(failStr)
                            Console.println(failStr)
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
  def evaluate()(implicit context: ExecutionContext): Future[IEvaluated[App]] =
    app.id match {
      case Some(id) =>
        val listOfFutOptAppMachines = query
        val listOfAppMach = listOfFutOptAppMachines.flatMap {
          f => {
            val appMach = Await.result(f, 3 seconds)
            appMach
          }
        }
        for {
          latestSomeApp <- futureEitherOfOptionExceptionToOption(repo.get(id))
          optApp <- latestSomeApp match {
            case Some(latestApp) =>
              futureEitherOfOptionExceptionToOption(repo.update(latestApp.copy(actualCluster = listOfAppMach)))
            case None =>
              future(None)
          }
        } yield {
          optApp match {
            case Some(updated) =>
              Pass(updated)
            case None =>
              Fail(app)
          }
        }
      case None =>
        future(Fail(app))

    }

  protected def query = app.actualCluster.map {
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

  def filterOutImmediateForwardSlash(testUrl: String): String = {
    testUrl.startsWith("/") match {
      case true =>
        testUrl.replaceFirst("/", "")
      case false =>
        testUrl
    }
  }

  def futOneBoolToPassFail(oneFutBool: Future[Boolean]): Future[IEvaluated[App]] = {
    for {
      bool <- oneFutBool
    } yield {
      if (bool)
        Pass(app)
      else
        Fail(app)
    }
  }

  def failAction(result: App) = {
  }


  def passAction(result: App) {
  }

  def name = app.name + "Query"


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
