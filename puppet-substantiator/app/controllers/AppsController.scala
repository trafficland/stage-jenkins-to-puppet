package controllers

import play.api._
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
import shapeless.Reverse
import play.core.Router

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

  def validate(name: String, delay: Int) = Action {
    Async {
      Thread.sleep(delay)
      val checkApp = for {
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
            if (app.actualCluster.forall(_.actual == app.expected))
              Ok("Expected values matched to value: " + app.expected)
            else {
              val machinesListString = app.actualCluster.map(_.machineName).flatten mkString ", "
              Redirect(routes.ScriptController.rollBack(app.name))
              Conflict("Expected app value: " + app.expected + machinesListString)
            }
          case None =>
            NotFound
        }
      }
      checkApp
    }
  }

}

object AppsController
  extends AppsController
  with AppRepositoryProvider
