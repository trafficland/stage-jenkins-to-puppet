package controllers

import play.api._
import libs.json.{Json, JsBoolean}
import play.api.mvc._
import models._
import mongo.reactive.App
import services.repository.mongo.reactive.impls.{MachineRepositoryProvider, AppRepositoryProvider, IAppsRepositoryProvider}
import services.repository.mongo.reactive.MongoUniqueCheck
import concurrent.ExecutionContext.Implicits.global
import controllers.local._
import scala.concurrent._

abstract class AppsController extends RestController[App]
with IAppsRepositoryProvider {

  implicit val jsonReader = App.AppJSONReader
  implicit val jsonWriter = App.AppJSONWriter
  implicit val criteriaReader = App.AppCriteriaReader
  implicit val uniqueCheckReader = App.AppUniqueCheckReader

  override def create = Action(parse.json) {
    request =>
      request.body.asOpt[App] match {
        case Some(model) =>

          if (model.isValid) {
            Async {
              val futureAll = model.actualCluster.map(clust => LookUp.machineLookUp.machineExists(clust.machineName))
              val simpleResult: Future[Result] = for {
                all <- futureAll
                exists <- all
                result <-
                if (exists)
                  repository.create(model) map {
                    _ match {
                      case Some(saved) => Ok(Json.toJson[App](saved))
                      case None => InternalServerError("Unable to retrieve saved model")
                    }
                  }
                else
                  future(InternalServerError("Machine names do not Exist in App, Create Machines through the controller first!"))
              } yield (result)
              simpleResult
            }
          } else {
            InternalServerError(Json.toJson[App](model))
          }
        case None => InternalServerError("Unable to parse json")
      }
  }

  override def edit(id: String) = Action(parse.json) { request =>
    request.body.asOpt[App] match {
      case Some(model) =>
        model.id match {
          case Some(modelId) =>
            if (modelId.stringify == id) {
              if (model.isValid) {
                Async {
                  repository.update(model) map { _ match {
                    case Some(saved) => Ok(Json.toJson[App](saved))
                    case None => NotFound
                  }}
                }
              } else {
                InternalServerError(Json.toJson[App](model))
              }
            } else {
              NotFound
            }

          case None =>
            NotFound
        }
      case None =>
        InternalServerError("Unable to parse json")
    }
  }

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
