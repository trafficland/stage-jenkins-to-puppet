package controllers

import play.api.mvc._
import models.mongo.reactive._
import services.repository.mongo.reactive.MongoUniqueCheck
import play.api.libs.json.JsBoolean
import concurrent.ExecutionContext.Implicits.global
import services.repository.mongo.reactive.impls.{MachineRepositoryProvider, IMachinesRepositoryProvider}


abstract class MachinesController extends RestController[Machine]
with IMachinesRepositoryProvider {
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

object MachinesController
  extends MachinesController
  with MachineRepositoryProvider