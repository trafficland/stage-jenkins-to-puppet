package controllers

import models.mongo.reactive.{AppMachineState, IAppReadersWriters, App}
import scala.concurrent._
import services.repository.mongo.reactive.{MachineCreator, AbstractAppCreator}
import play.api.test._
import play.api.test.Helpers._

class AppsControllerIntegrationSpec
  extends AbstractAppCreator
  with IRestControllerBehaviors[App]
  with IAppReadersWriters {

  override val machineCreator = MachineCreator(collection)

  override def createEntities(numberOfEntities: Int): Future[Int] = {
    super.createEntities(numberOfEntities)
  }

  def createValidEntity: App = createEntity

  def createInvalidEntity: App = new App("invalid", "invalid", "admin/version", List.empty[AppMachineState], None)

  def createValidNoIDEntity: App = createEntity.copy(id = None)

  override val entityName = "apps"

  override val collectionName = this.entityName

  override lazy val testName = "test-akka-mock"

  step(Await.result(machineCreator.createEntities(2), timeoutSeconds * 2))

  ("Validation" should {

    "create App with a renameAppTo" in new ICleanDatabase {
      val test = "renameTest"
      val entity = createValidEntity.copy(renameAppTo = Some(test))
      val request = new FakeRequest(POST, "/%s".format(collectionName),
        FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), jsonWriter.writes(entity))
      val result = checkForAsyncResult(route(request).get)
      status(result) should be equalTo (OK)
      resultToFieldComparison(result, "_id", entity.id.get.stringify) should be equalTo true
      resultToOptField(result, "renameAppTo") should be equalTo Some(test)
    }

    "validate should return ok" in new ICleanDatabase {
      val entity = createValidEntity
      val request = new FakeRequest(GET, "/%s/validate/%s/%s/%s".format(collectionName, "app199", 5000, 5000),
        FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), "")
      await(collection(collectionName).insert[App](entity))
      val result = checkForAsyncResult(route(request).get)
      status(result) must be equalTo OK
    }
  }) :: baseShould

  override def cleanup = {
    machineCreator.clean()
    super.cleanup
  }
}
