package controllers

import play.api._
import libs.json.JsBoolean
import play.api.mvc._
import models._
import mongo.reactive.App
import services.repository.mongo.reactive.impls.{AppRepositoryProvider, IAppsRepositoryProvider}
import services.repository.mongo.reactive.MongoUniqueCheck
import concurrent.ExecutionContext.Implicits.global

abstract class AppsController extends RestController[App]
with IAppsRepositoryProvider {

  implicit val jsonReader = App.AppJSONReader
  implicit val jsonWriter = App.AppJSONWriter
  implicit val criteriaReader = App.AppCriteriaReader
  implicit val uniqueCheckReader = App.AppUniqueCheckReader

  def uniqueCheck = Action(parse.json) {
    request =>
      Async {
        repository.uniqueCheck(request.body.as[MongoUniqueCheck]) map {
          result =>
            Ok(JsBoolean(result))
        }
      }
  }

}

object AppsController
  extends AppsController
  with AppRepositoryProvider
