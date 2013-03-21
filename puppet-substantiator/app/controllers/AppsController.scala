package controllers

import util.LogAndConsole
import play.api._
import play.api.libs.json._
import libs.json.JsBoolean
import play.api.mvc._
import models._
import mongo.reactive.App
import services.repository.mongo.reactive.impls.{AppRepositoryProvider, IAppsRepositoryProvider}
import services.repository.mongo.reactive.{MongoSearchCriteria, MongoUniqueCheck}
import concurrent.ExecutionContext.Implicits.global
import concurrent._
import reactivemongo.bson.{BSONString, BSONDocument}
import services.repository.Paging
import actors.ValidatorActor
import ValidatorActor._
import services.evaluations.{QueryMachinesUpdateAppEvaluate, AppEvaluate}
import actors.context.playframework.ActorContextProvider
import ActorContextProvider._


abstract class AppsController extends RestController[App]
with IAppsRepositoryProvider {

  implicit val log = play.api.Logger.logger

  def uniqueCheck = Action(parse.json) {
    request =>
      Async {
        repository.uniqueCheck(request.body.as[MongoUniqueCheck]) map {
          result =>
            Ok(JsBoolean(result))
        }
      }
  }

  def validate(name: String, delayQuerySeconds: Int, delayValidateSeconds: Int) = Action {
    Async {
      val httpResult = for {
        result <- repository.search(MongoSearchCriteria(BSONDocument("name" -> BSONString(name)), None, Some(Paging(0, 1))))
        apps <-
        if (result.count > 0) {
          result.results.run[List[App]](repository.modelIteratee).map(m => m)
        }
        else
          future(List.empty[App])
      } yield {
        apps.headOption match {
          case Some(app) =>
            val validatorActorRef = actors().getActor(validatorName)
            //Offset timing so that Query and Updating have a shot to finish first before AppEvaluate Evaluates an Applications State
            validatorActorRef ! StartValidation(delayQuerySeconds, QueryMachinesUpdateAppEvaluate(app, repository), actors().system)
            validatorActorRef ! StartValidation(delayValidateSeconds, AppEvaluate(app, repository), actors().system)
            val msg = "Application found! Validation beginning for app: " + Json.toJson(app).toString() + "\n" +
              "This is the applications current state not the evaluation state!"
            LogAndConsole.debug(msg)
            Ok(msg)
          case None =>
            NotFound
        }
      }
      httpResult
    }
  }

  def cancelValidation(name: String) = Action {
    import _root_.actors.fsm.CancellableMapFSMDomainProvider.domain._
    actors().getActor(scheduleName) ! Remove(name)
    val msg = "Cancellation sent for name: " + name
    LogAndConsole.debug(msg)
    Ok(msg)
  }

}

object AppsController
  extends AppsController
  with AppRepositoryProvider
