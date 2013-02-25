package controllers

import _root_.util.actors.fsm.CancellableMapFSMDomainProvider
import _root_.util.actors.ScriptExecutorActor
import models.mongo.reactive.{AppMachineState, IAppReadersWriters, IMachineReadersWriters, App, Machine}
import scala.concurrent._
import services.repository.mongo.reactive.{IMachineRepoHelper, IAppsRepoHelper}
import play.api.test._
import play.api.test.Helpers._
import globals.playframework.ActorsProvider
import ActorsProvider._

class AppsControllerIntegrationSpec
  extends IAppsRepoHelper
  with IRestControllerBehaviors[App]
  with IAppReadersWriters {

  override def createEntities(numberOfEntities: Int): Future[Int] = {
    super.createEntities(numberOfEntities)
  }

  def createValidEntity: App = createEntity

  def createInvalidEntity: App = new App("invalid", "invalid", "admin/version", List.empty[AppMachineState], None)

  def createValidNoIDEntity: App = createEntity.copy(id = None)

  override val entityName = "apps"

  override val collectionName = this.entityName

  override lazy val testName = "test-akka-mock"

  step({
    Await.result(machineRepoHelper.createEntities(2), timeoutSeconds * 2)
  })
  ("Validation" should {

    "validate should return ok" in new ICleanDatabase {
      val entity = createValidEntity
      val request = new FakeRequest(GET, "/%s/validate/%s/%s".format(collectionName, "app199", 5000),
        FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), "")
      createRunningApp(testName) {
        Await.result(db(collectionName).insert[App](entity), timeoutSeconds * 2)
        val result = checkForAsyncResult(route(request).get)
        status(result) must be equalTo OK
      }
    }
  }) :: baseShould


  override def cleanup = {
    machineRepoHelper.clean()
    db.connection.close()
  }

}
