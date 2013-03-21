package controllers

import org.specs2.mutable._
import org.specs2.specification.Fragments
import scala.concurrent._
import play.api.test._
import play.api.test.Helpers._
import reactivemongo.bson._
import models.IReadersWriters
import models.mongo.reactive.IMongoModel
import play.api.mvc._
import play.api.libs.json._
import _root_.util.playframework._
import services.repository.mongo.reactive.IMongoCollection

trait IRestControllerBehaviors[TModel <: IMongoModel[TModel]]
  extends Specification with IPlaySpecHelper
  with IMongoCollection
  with LiveMongoDBConnection with LiveTestServer
  with IReadersWriters[TModel] with Controller {

  def createEntities(numberOfEntities: Int): Future[Int]

  def createValidEntity: TModel

  def createInvalidEntity: TModel

  def createValidNoIDEntity: TModel

  def entityName: String

  def collectionName: String

  override protected lazy val fakeApp = Some(createFakeApp(testName))

  trait ICleanDatabase extends After {
    def after =
      clean()
  }

  def baseShould: List[Fragments] = List[Fragments](

    "\"POST SAVE to /%s/save\"".format(entityName) should {
      sequential

      " saving new entity should create" in new ICleanDatabase {
        val entity = createValidNoIDEntity
        val request = new FakeRequest(POST, "/%s/save".format(collectionName),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(entity))
        val result = checkForAsyncResult(route(request).get)
        status(result) should be equalTo (OK)
        resultToOptField(result, "_id").isDefined shouldEqual true
        jsonReader.reads(Json.parse(contentAsString(result))).get.isEqualTo(entity, false)
      }
      " save existing should update values" in new ICleanDatabase {
        val entity = createValidNoIDEntity
        val request = new FakeRequest(POST, "/%s/save".format(collectionName),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(entity))
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
    },
    "\"POST to /%s/\"".format(entityName) should {

      " create a new entity" in new ICleanDatabase {
        val entity = createValidEntity
        val request = new FakeRequest(POST, "/%s".format(collectionName),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(entity))
        val result = checkForAsyncResult(route(request).get)
        status(result) should be equalTo (OK)
        resultToFieldComparison(result, "_id", entity.id.get.stringify) should be equalTo true
      }

      "return the invalid entity with errors" in new ICleanDatabase {
        val entity = createInvalidEntity
        val request = new FakeRequest(POST, "/%s".format(collectionName),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(entity))
        val result = checkForAsyncResult(route(request).get)
        val hasStatus = status(result) == INTERNAL_SERVER_ERROR
        //val hasErrors = (Json.parse(contentAsString(result)) \ "errors").asOpt[JsValue].isDefined
        hasStatus //&& hasErrors
      }
    },

    "PUT to /%s/:id".format(entityName) should {

      " update the existing entity" in new ICleanDatabase {
        val entity = createValidEntity
        val request = new FakeRequest(PUT, "/%s/%s".format(collectionName, entity.id.get.stringify),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(entity))
        await(collection(collectionName).insert[TModel](entity))
        val result = checkForAsyncResult(route(request).get)
        status(result) should be equalTo (OK)
        resultToFieldComparison(result, "_id", entity.id.get.stringify) should be equalTo true
      }
      "return the invalid entity with errors" in new ICleanDatabase {
        val entity = createValidEntity
        val invalid = createInvalidEntity
        await(collection(collectionName).insert[TModel](entity))
        val request = new FakeRequest(PUT, "/%s/%s".format(collectionName, entity.id.get.stringify),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(invalid))
        val result = checkForAsyncResult(route(request).get)
        status(result) should be equalTo NOT_FOUND

      }
      "return a 404 when the original entity cannot be found" in new ICleanDatabase {
        val entity = createValidEntity
        val request = new FakeRequest(PUT, "/%s/%s".format(collectionName, entity.id.get.stringify),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(createValidEntity))
        val result = checkForAsyncResult(route(request).get)
        status(result) must be equalTo NOT_FOUND
      }
    },
    "DELETE to /%s/:id".format(entityName) should {

      "delete the entity with the given id and return a status of NO_CONTENT(204)" in new ICleanDatabase {
        val entity = createValidEntity
        await(collection(collectionName).insert[TModel](entity))
        val request = new FakeRequest(DELETE, "/%s/%s".format(collectionName, entity.id.get.stringify),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(createValidEntity))
        val result = checkForAsyncResult(route(request).get)
        status(result) should be equalTo NO_CONTENT
      }
      "return a NO_CONTENT (204) status when the entity does not exist" in new ICleanDatabase {
        val request = new FakeRequest(DELETE, "/%s/%s".format(collectionName, BSONObjectID.generate.stringify),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(createValidEntity))
        val result = checkForAsyncResult(route(request).get)
        status(result) should be equalTo NO_CONTENT
      }
    },

    "GET to /%s".format(entityName) should {

      "return all entities" in new ICleanDatabase {
        val request = new FakeRequest(GET, "/%s".format(collectionName),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), "")
        Await.result(createEntities(20), timeoutSeconds * 20)
        val result = checkForAsyncResult(route(request).get)
        status(result) must be equalTo OK
        val list = chunksToModelList(result.asInstanceOf[ChunkedResult[String]])
        list.size should be equalTo 20
      }
      "return an empty array when no entities exist" in new ICleanDatabase {
        val request = new FakeRequest(GET, "/%s".format(collectionName),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), "")
        val result = checkForAsyncResult(route(request).get)
        status(result) must be equalTo OK
        val list = chunksToModelList(result.asInstanceOf[ChunkedResult[String]])
        list.size should be equalTo 0
      }
    },
    "GET to /%s/:id".format(entityName) should {
      "return the entity with the given :id" in new ICleanDatabase {
        val entity = createValidEntity
        val request = new FakeRequest(GET, "/%s/%s".format(collectionName, entity.id.get.stringify),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), "")
        await(collection(collectionName).insert[TModel](entity))
        val result = checkForAsyncResult(route(request).get)
        status(result) should be equalTo OK
        Json.parse(contentAsString(result)).asOpt[TModel] should beSome[TModel]
      }
      "return a 404 status when the entity does not exist" in new ICleanDatabase {
        val entity = createValidEntity
        val request = new FakeRequest(GET, "/%s/%s".format(collectionName, entity.id.get.stringify),
          FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), "")
        val result = checkForAsyncResult(route(request).get)
        status(result) should be equalTo NOT_FOUND
        contentAsString(result) must be equalTo ""
      }
      step(cleanup)
    }
  )

  def cleanup = {
    closeConnection()
    stopServer()
  }
}