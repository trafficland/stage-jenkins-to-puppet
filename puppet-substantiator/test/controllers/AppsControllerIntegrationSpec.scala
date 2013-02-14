package controllers

import models.mongo.reactive.{AppMachineState, IAppReadersWriters, IMachineReadersWriters, App, Machine}
import scala.concurrent._
import collection.JavaConversions._
import services.repository.mongo.reactive.{IMachineRepoHelper, IAppsRepoHelper}
import reactivemongo.api.MongoConnection

class AppsControllerIntegrationSpec
  extends IAppsRepoHelper
  with IRestControllerBehaviors[App]
  with IAppReadersWriters {

  override def createEntities(numberOfEntities: Int): Future[Int] = {
    super.createEntities(numberOfEntities)
  }

  def createValidEntity: App = createEntity

  def createInvalidEntity: App = new App("invalid", "invalid", List.empty[AppMachineState], None)

  override val entityName = "apps"

  override val collectionName = this.entityName

  step({
    Await.result(machineRepoHelper.createEntities(2), timeoutSeconds * 2)
  })
  ("AppController" should {

    "AppControllerTest1" in new ICleanDatabase {
      //            val entity = createValidEntity
      //            val request = new FakeRequest(GET, "/%s/validate/%s/%s".format(collectionName, "app199", 5000),
      //              FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), "")
      //            createRunningApp("test") {
      //              await(db(collectionName).insert[App](entity), timeoutSeconds * 5)
      //              val result = checkForAsyncResult(route(request).get)
      //              status(result) must be equalTo OK
      //            }
    }
  }) :: baseShould


  override def cleanup = {
    machineRepoHelper.clean()
    db.connection.close()
  }

}
