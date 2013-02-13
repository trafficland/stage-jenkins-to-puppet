package controllers

import models.mongo.reactive.{AppMachineState, IAppReadersWriters, IMachineReadersWriters, App, Machine}
import scala.concurrent._
import collection.JavaConversions._
import services.repository.mongo.reactive.{IMachineRepoHelper, IAppsRepoHelper}
import reactivemongo.api.MongoConnection

class AppsControllerIntegrationSpec
  extends IAppsRepoHelper
  with IRestControllerBehaviors[App]
  with IAppReadersWriters  {

  override def createEntities(numberOfEntities: Int): Future[Int] = {
    super.createEntities(numberOfEntities)
  }

  def createValidEntity: App = createEntity

  def createInvalidEntity: App = new App("invalid", "invalid", List.empty[AppMachineState], None)

  override val entityName = "apps"

  override val collectionName = this.entityName

  override lazy val machineRepoHelper = new IMachineRepoHelper() {
    override lazy val db =
      createRunningApp("test") {
        MongoConnection(app.configuration.getStringList("mongodb.servers")
          .get.toList)(app.configuration.getString("mongodb.db").get)
      }
  }

  step(machineRepoHelper.createEntities(2))
  ("AppController" should {

    "AppControllerTest1" in new ICleanDatabase {
      true
    }
  }) :: baseShould
  step({
    machineRepoHelper.clean()
    machineRepoHelper.db.connection.close()
  })


}
