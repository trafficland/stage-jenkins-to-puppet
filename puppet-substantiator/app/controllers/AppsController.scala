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
import _root_.util.actors._
import _root_.util.actors.ValidatorActorMessages._
import services.evaluations.AppEvaluate
import _root_.util.actors.fsm.{CancellableMapFSM}
import _root_.util.OptionHelper._
import play.api.Play.configuration

abstract class AppsController extends RestController[App]
with IAppsRepositoryProvider {

  implicit val jsonReader = App.AppJSONReader
  implicit val jsonWriter = App.AppJSONWriter
  implicit val criteriaReader = App.AppCriteriaReader
  implicit val uniqueCheckReader = App.AppUniqueCheckReader

  implicit val playApp = play.api.Play.current
  lazy val validatorName = getOptionOrDefault(configuration.getString("validationActorName"), "validationActorName")
  lazy val schedulerName = getOptionOrDefault(configuration.getString("cancelActorName"), "scheduler")
  lazy val delayMilli = getOptionOrDefault(configuration.getInt("actor.schedule.delayMilli"), 1000)

  lazy val system = Akka.system
  lazy val schedule = system.actorOf(Props(() => new CancellableMapFSM(delayMilli), validatorName))
  lazy val validatorActorRef = system.actorOf(Props(() => new ValidatorActor(global, schedule), schedulerName))


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
        result <- repository.search(MongoSearchCriteria(BSONDocument("key" -> BSONString(name)), None, Some(Paging(0, 1))))
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

  def cancelValidation(name: String) = Action {
    import _root_.util.actors.fsm.CancellableMapFSMDomainProvider.domain._
    schedule ! Remove(name)
    Ok("Cancellation sent for name: " + name)
  }

}

object AppsController
  extends AppsController
  with AppRepositoryProvider
