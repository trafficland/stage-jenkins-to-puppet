package controllers

import play.api._
import play.api.libs.json._
import libs.json.JsBoolean
import play.api.mvc._
import models._
import mongo.reactive.{IAppReadersWriters, App}
import services.repository.mongo.reactive.impls.{AppRepositoryProvider, IAppsRepositoryProvider}
import services.repository.mongo.reactive.{MongoSearchCriteria, MongoUniqueCheck}
import concurrent.ExecutionContext.Implicits.global
import concurrent._
import reactivemongo.bson.{BSONString, BSONDocument}
import services.repository.Paging
import _root_.util.actors.ValidatorActor._
import services.evaluations.{QueryMachinesUpdateAppEvaluate, AppEvaluate}
import globals.playframework.ActorsProvider
import ActorsProvider._

abstract class AppsController extends RestController[App]
with IAppsRepositoryProvider {

  def uniqueCheck = Action(parse.json) {
    request =>
      Async {
        repository.uniqueCheck(request.body.as[MongoUniqueCheck]) map {
          result =>
            Ok(JsBoolean(result))
        }
      }
  }

  def validate(name: String, delayMilliSeconds: Int) = Action {
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
            validatorActorRef ! StartValidation(delayMilliSeconds, QueryMachinesUpdateAppEvaluate(app, repository), actors().system)
            validatorActorRef ! StartValidation(delayMilliSeconds + 60000, AppEvaluate(app, repository), actors().system)
            Ok("Application found! Validation beginning for app: " + Json.toJson(app).toString() + "\n" +
              "This is the applications current state not the evaluation state!")
          case None =>
            NotFound
        }
      }
      httpResult
    }
  }

  def cancelValidation(name: String) = Action {
    import _root_.util.actors.fsm.CancellableMapFSMDomainProvider.domain._
    actors().getActor(scheduleName) ! Remove(name)
    Ok("Cancellation sent for name: " + name)
  }

}

object AppsController
  extends AppsController
  with AppRepositoryProvider
