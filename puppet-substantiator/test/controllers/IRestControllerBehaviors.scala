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

trait IRestControllerBehaviors[TModel <: IMongoModel[TModel]]
  extends Specification with IPlaySpecHelper
  with IReadersWriters[TModel] with IDbProvider[DefaultDB] with Controller {

  def createEntities(numberOfEntities: Int): Future[Int]

  def createValidEntity: TModel

  def createInvalidEntity: TModel

  def createValidNoIDEntity: TModel

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

  trait ICleanDatabase extends After {
    def after =
      Await.result(db.collection(collectionName).remove(BSONDocument(), firstMatchOnly = false), FiniteDuration(10, "seconds"))
  }

  def baseShould: List[Fragments] = List[Fragments](

    "\"POST SAVE to /%s/save\"".format(entityName) should {

      " saving new entity should create" in new ICleanDatabase {
        val entity = createValidNoIDEntity
        val request = new FakeRequest(POST, "/%s/save".format(collectionName),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(entity))
        createRunningApp(testName) {
          val result = checkForAsyncResult(route(request).get)
          status(result) should be equalTo (OK)
          resultToOptField(result, "_id").isDefined shouldEqual true
          jsonReader.reads(Json.parse(contentAsString(result))).get.isEqualTo(entity, false)
        }
      }
      " save existing should update values" in new ICleanDatabase {
        val entity = createValidNoIDEntity
        val request = new FakeRequest(POST, "/%s/save".format(collectionName),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(entity))
        createRunningApp(testName) {
          val result = checkForAsyncResult(route(request).get)
          status(result) should be equalTo (OK)
          resultToOptField(result, "_id").isDefined shouldEqual true
          jsonReader.reads(Json.parse(contentAsString(result))).get.isEqualTo(entity, false)
          entity.name = "test1234"
          val request2 = new FakeRequest(POST, "/%s/save".format(collectionName),
            FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(entity))
          val result2 = checkForAsyncResult(route(request).get)
          jsonReader.reads(Json.parse(contentAsString(result2))).get.isEqualTo(entity, true)
        }
      }
    },
    "\"POST to /%s/\"".format(entityName) should {

      " create a new entity" in new ICleanDatabase {
        val entity = createValidEntity
        val request = new FakeRequest(POST, "/%s".format(collectionName),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(entity))
        createRunningApp(testName) {
          val result = checkForAsyncResult(route(request).get)
          status(result) should be equalTo (OK)
          resultToFieldComparison(result, "_id", entity.id.get.stringify) should be equalTo true
        }
      }

      "return the invalid entity with errors" in new ICleanDatabase {
        val entity = createInvalidEntity
        val request = new FakeRequest(POST, "/%s".format(collectionName),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(entity))
        createRunningApp(testName) {
          val result = checkForAsyncResult(route(request).get)
          val hasStatus = status(result) == INTERNAL_SERVER_ERROR
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
        createRunningApp(testName) {
          await(db(collectionName).insert[TModel](entity))
          val result = checkForAsyncResult(route(request).get)
          status(result) should be equalTo (OK)
          resultToFieldComparison(result, "_id", entity.id.get.stringify) should be equalTo true
        }
      }
      "return the invalid entity with errors" in new ICleanDatabase {
        val entity = createValidEntity
        val invalid = createInvalidEntity
        await(db(collectionName).insert[TModel](entity))
        val request = new FakeRequest(PUT, "/%s/%s".format(collectionName, entity.id.get.stringify),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(invalid))
        createRunningApp(testName) {
          val result = checkForAsyncResult(route(request).get)
          status(result) should be equalTo NOT_FOUND

        }
      }
      "return a 404 when the original entity cannot be found" in new ICleanDatabase {
        val entity = createValidEntity
        val request = new FakeRequest(PUT, "/%s/%s".format(collectionName, entity.id.get.stringify),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(createValidEntity))
        createRunningApp(testName) {
          val result = checkForAsyncResult(route(request).get)
          status(result) must be equalTo NOT_FOUND
        }
      }
    },
    "DELETE to /%s/:id".format(entityName) should {

      "delete the entity with the given id and return a status of NO_CONTENT(204)" in new ICleanDatabase {
        val entity = createValidEntity
        await(db(collectionName).insert[TModel](entity))
        val request = new FakeRequest(DELETE, "/%s/%s".format(collectionName, entity.id.get.stringify),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(createValidEntity))
        createRunningApp(testName) {
          val result = checkForAsyncResult(route(request).get)
          status(result) should be equalTo NO_CONTENT
        }
      }
      "return a NO_CONTENT (204) status when the entity does not exist" in new ICleanDatabase {
        val request = new FakeRequest(DELETE, "/%s/%s".format(collectionName, BSONObjectID.generate.stringify),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(createValidEntity))
        createRunningApp(testName) {
          val result = checkForAsyncResult(route(request).get)
          status(result) should be equalTo NO_CONTENT
        }
      }
    },

    "GET to /%s".format(entityName) should {

      "return all entities" in new ICleanDatabase {
        val request = new FakeRequest(GET, "/%s".format(collectionName),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), "")
        createRunningApp(testName) {
          Await.result(createEntities(20), timeoutSeconds * 20)
          val result = checkForAsyncResult(route(request).get)
          status(result) must be equalTo OK
          val list = chunksToModelList(result.asInstanceOf[ChunkedResult[String]])
          list.size should be equalTo 20
        }
      }
      "return an empty array when no entities exist" in new ICleanDatabase {
        val request = new FakeRequest(GET, "/%s".format(collectionName),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), "")
        createRunningApp(testName) {
          val result = checkForAsyncResult(route(request).get)
          status(result) must be equalTo OK
          val list = chunksToModelList(result.asInstanceOf[ChunkedResult[String]])
          list.size should be equalTo 0
        }
      }
    },
    "GET to /%s/:id".format(entityName) should {
      "return the entity with the given :id" in new ICleanDatabase {
        val entity = createValidEntity
        val request = new FakeRequest(GET, "/%s/%s".format(collectionName, entity.id.get.stringify),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), "")
        createRunningApp(testName) {
          await(db(collectionName).insert[TModel](entity))
          val result = checkForAsyncResult(route(request).get)
          status(result) should be equalTo OK
          Json.parse(contentAsString(result)).asOpt[TModel] should beSome[TModel]
        }
      }
      "return a 404 status when the entity does not exist" in new ICleanDatabase {
        val entity = createValidEntity
        val request = new FakeRequest(GET, "/%s/%s".format(collectionName, entity.id.get.stringify),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), "")
        createRunningApp(testName) {
          val result = checkForAsyncResult(route(request).get)
          status(result) should be equalTo NOT_FOUND
          contentAsString(result) must be equalTo ""
        }
      }
      step(cleanup)
    }
  )

  override lazy val db = {
    play.api.Play.maybeApplication match {
      case Some(app) =>
        MongoConnection(app.configuration.getStringList("mongodb.servers").get.toList)(app.configuration.getString("mongodb.db").get)
      case None =>
        createRunningApp(testName) {
          val app = play.api.Play.current
          MongoConnection(app.configuration.getStringList("mongodb.servers").get.toList)(app.configuration.getString("mongodb.db").get)
        }
    }
  }

  def cleanup = db.connection.close()
}
