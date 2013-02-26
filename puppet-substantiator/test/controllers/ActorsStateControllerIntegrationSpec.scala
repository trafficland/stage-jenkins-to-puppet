package controllers

import models.mongo.reactive.{IActorStateReadersWriters, ActorState}
import scala.concurrent._
import play.api.libs.iteratee.Enumerator
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import play.api.libs.json._

class ActorsStateControllerIntegrationSpec
  extends IRestControllerBehaviors[ActorState]
  with IActorStateReadersWriters {

  def createEntities(numberOfEntities: Int): Future[Int] = {
    val entities = (1 to numberOfEntities) map {
      index =>
        new ActorState("test%d".format(index), true, "state")
    }
    db(collectionName).insert[ActorState](Enumerator(entities: _*))
  }

  def createValidEntity = new ActorState("test1", true, "state")

  def createValidNoIDEntity = new ActorState("test1", false, "state", None)

  def createInvalidEntity = createValidNoIDEntity

  override val entityName = "actors"

  override val collectionName = this.entityName

  override lazy val testName = "test-akka-mock"

  ("POST" should {

    "be accepted and return correct json" in new ICleanDatabase {
      val entity = createValidEntity
      val request = new FakeRequest(POST, "/%s".format(collectionName),
        FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(entity))
      createRunningApp(testName) {
        val result = checkForAsyncResult(route(request).get)
        status(result) should be equalTo (OK)
        val content = contentAsString(result)
        val machine = jsonReader.reads(Json.parse(content)).get
        Json.stringify(jsonWriter.writes(machine.copy(id = None)))
          .shouldEqual( """{"name":"test1","isAlive":true,"state":"state"}""")
        resultToFieldComparison(result, "_id", entity.id.get.stringify) should be equalTo true
      }
    }
  }) :: baseShould

}

