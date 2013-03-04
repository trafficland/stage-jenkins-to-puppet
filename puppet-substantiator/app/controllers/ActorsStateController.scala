package controllers

import play.api.mvc._
import models.mongo.reactive.ActorState
import services.repository.mongo.reactive.MongoUniqueCheck
import play.api.libs.json.JsBoolean
import concurrent.ExecutionContext.Implicits.global
import services.repository.mongo.reactive.impls.{IActorsStateRepositoryProvider, ActorsStateRepositoryProvider}
import actors.SystemStateActor._
import actors.context.playframework.ActorContextProvider._

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


  def pollOn = Action {
    actors().getActor(actorStateHandlerName) ! PollOn
    Ok("pollOn")
  }

  def pollOff = Action {
    actors().getActor(actorStateHandlerName) ! PollOff
    Ok("pollOff")
  }

  //replace actor hook scheduled spray
  def refreshState = Action {
    actors().getActor(actorStateHandlerName) ! Ping
    Ok("refreshedState")
  }

  def resources = Action {
    Ok(views.html.actors.resources())
  }
}

object ActorsStateController
  extends ActorsStateController
  with ActorsStateRepositoryProvider