package controllers

import play.api.mvc._
import models.mongo.reactive.ActorState
import services.repository.mongo.reactive.MongoUniqueCheck
import play.api.libs.json.JsBoolean
import concurrent.ExecutionContext.Implicits.global
import services.repository.mongo.reactive.impls.{IActorsStateRepositoryProvider, ActorsStateRepositoryProvider}


abstract class ActorsStateController
  extends RestController[ActorState]
  with IActorsStateRepositoryProvider {

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

object ActorsStateController
  extends ActorsStateController
  with ActorsStateRepositoryProvider