package controllers

import org.specs2.mutable._
import org.specs2.specification.Fragment

import _root_.util.IPlaySpecHelper
import scala.concurrent._
import concurrent.duration._
import play.api.test._
import play.api.test.Helpers._
import services.repository.mongo.reactive.TestMongoDbProvider
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import models.IReadersWriters
import models.mongo.reactive.IMongoModel
import play.api.mvc.Controller
import play.api.http.Writeable

trait IRestControllerBehaviors[TModel <: IMongoModel]
  extends Specification with IPlaySpecHelper
  with IReadersWriters[TModel] with TestMongoDbProvider with Controller {

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

  trait databaseManagement extends After {
    def after = Await.result(
      Future.traverse(collections)(name => db.collection(name).remove(BSONDocument(), firstMatchOnly = false)),
      FiniteDuration(10, "seconds")
    )
  }

  def baseShould: Seq[Fragment] = Seq[Fragment](
    "\"POST to /%s/\"".format(entityName) + " create a new entity" in {
      val entity = createValidEntity
      val request = new FakeRequest(POST, "/%s".format(collectionName, entityName),
        FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))),
        jsonWriter.writes(entity))
//      (new Writeable({
//        s: String => s.getBytes
//      }, None))
      createRunningApp("test") {
        route(request) match {
          case Some(result) =>
            val tuple = resultToStatusContentTupleJsonErrors(result)
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
    },
    "test2" in {
      true
    }
  )

}
