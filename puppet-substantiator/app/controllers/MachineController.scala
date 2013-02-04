package controllers

import play.api.mvc._
import models.mongo.reactive._
import services.repository.mongo.reactive.{IMachineRepositoryProvider, MongoUniqueCheck, MachineRepositoryProvider, IMachineRepository}
import play.api.libs.json.JsBoolean
import concurrent.ExecutionContext.Implicits.global


abstract class MachineController extends RestController[Machine]
with IMachineRepositoryProvider {

implicit val jsonReader = Machine.MachineJSONReader
implicit val jsonWriter = Machine.MachineJSONWriter
implicit val criteriaReader = Machine.MachineCriteriaReader
implicit val uniqueCheckReader = Machine.MachineUniqueCheckReader

def uniqueCheck = Action(parse.json) { request =>
  Async {
    repository.uniqueCheck(request.body.as[MongoUniqueCheck]) map { result =>
     Ok(JsBoolean(result))
    }
  }
}

}

object MachinesController
  extends MachineController
  with MachineRepositoryProvider