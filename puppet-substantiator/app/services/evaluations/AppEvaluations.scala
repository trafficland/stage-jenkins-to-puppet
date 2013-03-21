package services.evaluations

import _root_.util.PlaySettings
import util.evaluations._
import models.mongo.reactive._
import play.api.libs.ws.WS
import services.repository.mongo.reactive.impls.IAppsRepository
import play.api.Logger._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
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
  def evaluate()(implicit context: ExecutionContext): Future[IEvaluated[App]] = {
    val futQueries = app.actualCluster.map {
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

    val oneFutBool = futQueriesResultsToListOfFutureBools(futQueries).reduce((futBool1, futBool2) =>
      for {
        nowBool1 <- futBool1
        nowBool2 <- futBool2
      } yield (nowBool1 && nowBool2)
    )
    futOneBoolToPassFail(oneFutBool)
  }

  def filterOutImmediateForwardSlash(testUrl: String): String = {
    testUrl.startsWith("/") match {
      case true =>
        testUrl.replaceFirst("/", "")
      case false =>
        testUrl
    }
  }

  def futQueriesResultsToListOfFutureBools(futQueries: List[Future[Either[Option[App], Exception]]]): List[Future[Boolean]] = {
    futQueries.map {
      futQuery =>
        for {
          query <- futQuery
          result <- query match {
            case Left(optUpdatedApp) =>
              optUpdatedApp match {
                case Some(upApp) =>
                  future(true)
                case None =>
                  future(false)
              }
            case Right(ex) =>
              future(false)
          }
        } yield (result)
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

  def failAction(result: App) = {}


  def passAction(result: App) {}

  def name = app.name + "Query"


  def testMachine(appToUpdate: App, machineName: String, request: WS.WSRequestHolder): Future[Either[Option[App], Exception]] = {
    appToUpdate.id match {
      case Some(id) =>
        val optFutResponse = request.get()
          .map(Some(_))
          .recover {
          case _ => None
        }
        for {
          optResponse <- optFutResponse
          latestAppState <- repo.get(id)
          update <- {
            optResponse match {
              case Some(result) =>
                latestAppState match {
                  case Left(latestOptApp) =>
                    latestOptApp match {
                      case Some(latestApp) =>
                        val logStr = "------------- Machine: %s got the following response: %s -------------".format(machineName, result.body)
                        val updated = repo.update(latestApp.copy(actualCluster =
                          AppMachineState(machineName, Some(result.body)) :: latestApp.actualCluster.filter(m => m.machineName != machineName)
                        ))
                        Console.print(logStr)
                        logger.info(logStr)
                        updated
                      case None =>
                        val logStr = "No latest app to update from ap: %s for machine: %s".format(app.name, machineName)
                        Console.println(logStr)
                        logger.debug(logStr)
                        future(Right(new Exception(logStr)))
                    }
                  case Right(ex) =>
                    logger.debug(ex.getMessage)
                    future(Right(ex))
                    future(Right(ex))
                }
              case None =>
                val logStr = "No Response from machine %s".format(machineName)
                Console.println(logStr)
                logger.debug(logStr)
                future(Right(new Exception(logStr)))
            }
          }
        } yield (update)
      case None =>
        logger.info("No id for application, therefore no application can be be updated frin testMachine. " +
          "This would cause the state to be out of sync.")
        future(Left(Some(appToUpdate)))
    }
  }

}
