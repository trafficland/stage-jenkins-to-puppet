package controllers

import play.api._
import play.api.libs.json._
import libs.concurrent.Akka
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
import akka.actor._
import services.actors.ValidatorActor
import services.actors.ValidatorActorMessages.StartValidation
import services.evaluations.AppEvaluations.AppEvaluate

abstract class AppsController extends RestController[App]
with IAppsRepositoryProvider {

  implicit val jsonReader = App.AppJSONReader
  implicit val jsonWriter = App.AppJSONWriter
  implicit val criteriaReader = App.AppCriteriaReader
  implicit val uniqueCheckReader = App.AppUniqueCheckReader

  implicit val playApp = play.api.Play.current
  val maybeActorName = play.api.Play.configuration.getString("validationActorName")
  val actorName = maybeActorName match {
    case Some(s) => s
    case None => "validator"
  }

  val system = Akka.system
  val validatorActorRef = system.actorOf(Props(() => new ValidatorActor(global), actorName))

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
            validatorActorRef ! StartValidation(delayMilliSeconds, AppEvaluate(app), system)
            Ok("Application found! Validation beinging for app: " + Json.toJson(app).toString() + "\n" +
              "This is the applications current state not the evaluation state!")
          case None =>
            NotFound
        }
      }
      httpResult
    }
  }

}

object AppsController
  extends AppsController
  with AppRepositoryProvider
