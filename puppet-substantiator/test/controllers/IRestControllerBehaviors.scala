package controllers

import org.specs2.mutable._
import org.specs2.specification.{Fragment, Fragments}

import _root_.util.IPlaySpecHelper
import scala.concurrent._
import concurrent.duration._
import play.api.test._
import play.api.test.Helpers._
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import models.IReadersWriters
import models.mongo.reactive.IMongoModel
import play.api.mvc._
import play.api.libs.json._
import services.repository.IDbProvider
import reactivemongo.api.{MongoConnection, DefaultDB}
import collection.JavaConversions._

trait IRestControllerBehaviors[TModel <: IMongoModel]
  extends Specification with IPlaySpecHelper
  with IReadersWriters[TModel] with IDbProvider[DefaultDB] with Controller {

  def createEntities(numberOfEntities: Int): Future[Int]

  def createValidEntity: TModel

  def createInvalidEntity: TModel

  def entityName: String

  def collectionName: String

  //  def search(criteria: JsValue): Future[(Int, List[TModel])] = {
  //    url("http://localhost:%s/%s/search".format(serverPort, entityName)).post(criteria) map { response =>
  //      val json = response.json
  //      val resultCount = (json \ "resultCount").as[Int]
  //      val results = (json \ "results").as[JsArray].value.map(_.as[TModel]).toList
  //      (resultCount, results)
  //    }
  //  }

  val collections = Seq("apps", "machines")

  trait ICleanDatabase extends After {
    def after =
      Await.result(Future.traverse(collections)(name => db.collection(name).remove(BSONDocument(), firstMatchOnly = false)),
        FiniteDuration(10, "seconds")
      )
  }

  def baseShould: List[Fragments] = List[Fragments](

    "\"POST to /%s/\"".format(entityName) should {
      " create a new entity" in new ICleanDatabase {
        val entity = createValidEntity
        val request = new FakeRequest(POST, "/%s".format(collectionName),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(entity))
        createRunningApp("test") {
          route(request) match {
            case Some(result) =>
              val tuple = resultToStatusContentBool(result, "_id", entity.id.get.stringify)
              tuple match {
                case (OK, true) =>
                  true
                case (_, _) =>
                  false
              }
            case None =>
              false
          }
        }
      }

      "return the invalid entity with errors" in new ICleanDatabase {
        val entity = createInvalidEntity
        val request = new FakeRequest(POST, "/%s".format(collectionName),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(entity))
        createRunningApp("test") {
          val result = checkForAsyncResult(route(request).get)
          val hasStatus = status(result) == 500
          //val hasErrors = (Json.parse(contentAsString(result)) \ "errors").asOpt[JsValue].isDefined
          hasStatus //&& hasErrors
        }
      }
    },

    "PUT to /%s/:id".format(entityName) should {
      " update the existing entity" in new ICleanDatabase {
        val entity = createValidEntity
        val request = new FakeRequest(PUT, "/%s/%s".format(collectionName, entity.id.get.stringify),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(entity))
        createRunningApp("test") {
          await(db(collectionName).insert[TModel](entity))
          route(request) match {
            case Some(result) =>
              val tuple = resultToStatusContentBool(result, "_id", entity.id.get.stringify)
              tuple match {
                case (OK, true) =>
                  true
                case (_, _) =>
                  false
              }
            case None =>
              false
          }
        }
      }
      "return the invalid entity with errors" in {
        val entity = createValidEntity
        val invalid = createInvalidEntity
        await(db(collectionName).insert[TModel](entity))
        val request = new FakeRequest(POST, "/%s".format(collectionName),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(invalid))
        createRunningApp("test") {
          val result = checkForAsyncResult(route(request).get)
          val hasStatus = status(result) == 500
          //val hasErrors = (Json.parse(contentAsString(result)) \ "errors").asOpt[JsValue].isDefined
          hasStatus //&& hasErrors

        }
      }
    })

  override lazy val db = {
    createRunningApp("test") {
      MongoConnection(app.configuration.getStringList("mongodb.servers").get.toList)(app.configuration.getString("mongodb.db").get)
    }
  }
}
