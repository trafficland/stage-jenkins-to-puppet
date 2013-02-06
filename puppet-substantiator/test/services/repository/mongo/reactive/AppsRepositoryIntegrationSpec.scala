package services.repository.mongo.reactive

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, WordSpec}
import org.scalatest.matchers.ShouldMatchers
import play.api.libs.iteratee.Enumerator
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import models.mongo.reactive._
import concurrent.ExecutionContext.Implicits.global
import concurrent.Await
import concurrent.duration._

class AppsRepositoryIntegrationSpec
  extends WordSpec
  with BeforeAndAfter
  with BeforeAndAfterAll
  with ShouldMatchers
  with TestMongoDbProvider
  with TestAppsRepositoryProvider
  with RepositoryBehaviors[App] {

  override def collectionName = "apps"

  val machineRepoSpec = new MachinesRepositoryIntegrationSpec()

  override def beforeAll {
    machineRepoSpec.createEntities(2)
    super.beforeAll()
  }

  after {
    Await.result(db.collection(collectionName).remove(query = BSONDocument(), firstMatchOnly = false), 10 seconds)
  }

  override def afterAll(configMap: Map[String, Any]) {
    machineRepoSpec.clean()
    machineRepoSpec.db.connection.close()
    db.connection.close()
  }

  implicit val reader = App.AppBSONReader
  implicit val writer = App.AppBSONWriter

  def createEntity = {

    new App(Some(BSONObjectID.generate), "app1?", "1.0.0", List(
      AppMachineState("testMachineName1", "0.0.1"),
      AppMachineState("testMachineName2", "0.0.1")))
  }

  def createEntities(numberOfEntities: Int) = {
    val entities = (0 until numberOfEntities) map {
      index => {
        new App(Some(BSONObjectID.generate), "app" + index + 1, "1.0.0", List(
          AppMachineState("testMachineName1", "0.0.1"),
          AppMachineState("testMachineName2", "0.0.1")))
      }
    }

    db(collectionName).insert[App](Enumerator(entities: _*))
  }

  behave like baseModelRepository
}