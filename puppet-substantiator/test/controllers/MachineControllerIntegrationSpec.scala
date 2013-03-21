package controllers

import models.mongo.reactive.{IMachineReadersWriters, Machine}
import scala.concurrent._
import play.api.libs.iteratee.Enumerator
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import play.api.libs.json._

class MachineControllerIntegrationSpec
  extends IRestControllerBehaviors[Machine]
  with IMachineReadersWriters {

  def createEntities(numberOfEntities: Int): Future[Int] = {
    val entities = (1 to numberOfEntities) map {
      index =>
        new Machine("name%d".format(index))
    }
    collection(collectionName).insert[Machine](Enumerator(entities: _*))
  }

  def createValidEntity: Machine = new Machine("test1")

  def createValidNoIDEntity: Machine = new Machine("test1", None)

  def createInvalidEntity: Machine = new Machine("invalid", None)

  override val entityName = "machines"

  override val collectionName = this.entityName

  override lazy val testName = "test-akka-mock"

  ("POST" should {

    "be accepted and return correct json" in new ICleanDatabase {
      val entity = createValidEntity
      val request = new FakeRequest(POST, "/%s".format(collectionName),
        FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(entity))
        val result = checkForAsyncResult(route(request).get)
        status(result) should be equalTo (OK)
        val content = contentAsString(result)
        val machine = jsonReader.reads(Json.parse(content)).get
        Json.stringify(jsonWriter.writes(machine.copy(id = None))) shouldEqual ("""{"name":"test1","isAlive":true}""")
        resultToFieldComparison(result, "_id", entity.id.get.stringify) should be equalTo true
    }
  }) :: baseShould

}
