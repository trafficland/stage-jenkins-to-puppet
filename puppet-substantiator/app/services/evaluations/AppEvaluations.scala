package services.evaluations

import _root_.util.PlaySettings
import util.evaluations._
import models.mongo.reactive._
import play.api.libs.ws.WS
import services.repository.mongo.reactive.impls.IAppsRepository
import play.api.Logger._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

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

case class AppEvaluate(app: App, repo: IAppsRepository) extends AbstractAppEvaluate {
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
                        val result = app.expected.contains(actualState)
                        result match {
                          case true =>
                            logger.debug("Version Check for %s application PASSED for %s version!".format(app.name, app.expected))
                          case false =>
                            logger.debug("Version Check for %s application FAILED for %s version!".format(app.name, app.expected))
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

  lazy val rollBackUrl = "http://" + PlaySettings.absUrl + "/rollback/%s/%s"

  def failAction(result: App) = WS.url(rollBackUrl.format(result.name, result.port.getOrElse("80"))).get()


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
            testMachine(app, machine.machineName, WS.url("http://%s:%s/%s".format(machine.machineName, realPort, filterOutImmediateForwardSlash(app.testUrl))))
          case None =>
            testMachine(app, machine.machineName, WS.url("http://%s/%s".format(machine.machineName, filterOutImmediateForwardSlash(app.testUrl))))
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
    val optFutResponse = request.get()
      .map(Some(_))
      .recover {
      case _ => None
    }
    for {
      optResponse <- optFutResponse
      update <- {
        optResponse match {
          case Some(result) =>
            repo.update(app.copy(actualCluster =
              AppMachineState(machineName, Some(result.body)) :: app.actualCluster.filter(m => m.machineName != machineName)
            ))
          case None =>
            val logStr = "No Response from machine %s".format(machineName)
            if (logger.isDebugEnabled())
              logger.debug(logStr)
            future(Right(new Exception(logStr)))
        }
      }
    } yield (update)
  }

}
