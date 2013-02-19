package controllers

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee.{Enumerator, Enumeratee}
import reactivemongo.bson.BSONObjectID
import concurrent.ExecutionContext.Implicits.global
import models.mongo.reactive.IMongoModel
import services.repository.mongo.reactive.{IMongoRepositoryProvider, MongoSearchCriteria}

abstract class RestController[TModel <: IMongoModel[TModel]]
  extends Controller
  with IMongoRepositoryProvider[TModel] {

  implicit val jsonReader: Reads[TModel]
  implicit val jsonWriter: Writes[TModel]
  implicit val criteriaReader: Reads[MongoSearchCriteria]

  def index = Action {
    request =>
      Ok.stream(
        Enumerator("[") andThen modelToSerializedJsArray(repository.getAll) andThen Enumerator("]") andThen Enumerator.eof[String]
      )

  }

  def get(id: String) = Action {
    request =>
      Async {
        repository.get(new BSONObjectID(id)) map {
          either =>
            either match {
              case Left(optModel) => optModel match {
                case Some(model) =>
                  Ok(Json.toJson[TModel](model))
                case None =>
                  NotFound
              }
              case Right(ex) => InternalServerError(ex.getMessage)

            }
        }
      }
  }

  def create = Action(parse.json) {
    request =>
      request.body.asOpt[TModel] match {
        case Some(model) =>
          if (model.isValid) {
            Async {
              val toBeInserted: TModel = model.id match {
                case Some(id) => model
                case None =>
                  model.id = Some(BSONObjectID.generate)
                  model
              }
              repository.create(toBeInserted) map {
                either =>
                  either match {
                    case Left(optModel) => optModel match {
                      case Some(saved) =>
                        Ok(Json.toJson[TModel](saved))
                      case None =>
                        InternalServerError("Unable to retrieve saved model")
                    }
                    case Right(ex) =>
                      InternalServerError(ex.getMessage)
                  }
              }
            }
          } else {
            InternalServerError(Json.toJson[TModel](model))
          }
        case None =>
          InternalServerError("Unable to parse json")
      }
  }

  def edit(id: String) = Action(parse.json) {
    request =>
      request.body.asOpt[TModel] match {
        case Some(model) =>
          model.id match {
            case Some(modelId) =>
              if (modelId.stringify == id) {
                if (model.isValid) {
                  Async {
                    repository.update(model) map {
                      either =>
                        either match {
                          case Left(optModel) => optModel match {
                            case Some(saved) => Ok(Json.toJson[TModel](saved))
                            case None => NotFound
                          }
                          case Right(ex) => InternalServerError(ex.getMessage)
                        }
                    }
                  }
                } else {
                  InternalServerError(Json.toJson[TModel](model))
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

  def save = Action(parse.json) {
    request =>
      request.body.asOpt[TModel] match {
        case Some(model) =>
          if (model.isValid) {
            Async {
              repository.save(model) map {
                either =>
                  either match {
                    case Left(optModel) => optModel match {
                      case Some(saved) =>
                        Ok(Json.toJson[TModel](saved))
                      case None =>
                        NotFound
                    }
                    case Right(ex) =>
                      InternalServerError(ex.getMessage)
                  }
              }
            }
          } else {
            InternalServerError(Json.toJson[TModel](model))
          }
        case None =>
          InternalServerError("Unable to parse json")
      }
  }


  def remove(id: String) = Action {
    Async {
      repository.remove(new BSONObjectID(id)) map {
        ok =>
          if (ok) Status(204)
          else InternalServerError
      }
    }
  }

  def search = Action(parse.json) {
    request =>
      val criteria = request.body.as[MongoSearchCriteria]

      Async {
        repository.search(criteria) map {
          results =>
            val enumerator =
              Enumerator( """{"resultCount":%d,"results":[""".format(results.count))
                .andThen(modelToSerializedJsArray(results.results))
                .andThen(Enumerator("]}"))
                .andThen(Enumerator.eof[String])

            Ok.stream(enumerator)
        }
      }
  }

  private def modelToSerializedJsArray(enumerator: Enumerator[TModel]) = {
    var isFirst = true

    enumerator.through(Enumeratee.map[TModel] {
      model =>
        val js = Json.stringify(Json.toJson[TModel](model))

        if (isFirst) {
          isFirst = false
          js
        } else {
          ",%s".format(js)
        }

    })
  }

}
