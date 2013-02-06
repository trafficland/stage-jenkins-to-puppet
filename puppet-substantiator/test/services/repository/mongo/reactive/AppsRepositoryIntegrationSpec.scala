package services.repository.mongo.reactive

import impls.{AppsRepository, AppRepositoryException}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, WordSpec}
import org.scalatest.matchers.ShouldMatchers
import play.api.libs.iteratee.Enumerator
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import models.mongo.reactive._
import concurrent.ExecutionContext.Implicits.global
import concurrent._
import concurrent.duration._
import org.scalatest.mock.MockitoSugar._
import scala.Some
import play.api.Configuration

class AppsRepositoryIntegrationSpec
  extends WordSpec
  with BeforeAndAfter
  with BeforeAndAfterAll
  with ShouldMatchers
  with TestMongoDbProvider
  with TestAppsRepositoryProvider
  with RepositoryBehaviors[App] {

  val machineRepoSpec = new MachinesRepositoryIntegrationSpec()

  override lazy val repository = new AppsRepository with TestMongoDbProvider {

    val config = mock[Configuration]
    val configuration = config

    override protected def machineRepo = machineRepoSpec.repository
  }

  override def collectionName = "apps"


  before {
    machineRepoSpec.createEntities(2)
  }
  after {
    machineRepoSpec.clean()
    Await.result(db.collection(collectionName).remove(query = BSONDocument(), firstMatchOnly = false), 10 seconds)
  }

  override def afterAll(configMap: Map[String, Any]) {
    machineRepoSpec.clean()
    machineRepoSpec.db.connection.close()
    db.connection.close()
  }

  implicit val reader = App.AppBSONReader
  implicit val writer = App.AppBSONWriter

  "creation with no machines" should {
    "fail" in {
      val entity = createEntity
      val except = try{
        machineRepoSpec.clean()
        Await.result(repository.create(entity), 10 seconds) match{
          case Some(app) =>
            None
          case None =>
            Some("yay failed")
        }
      }
      catch {
        case a: AppRepositoryException =>
          Some("worked")
      }

      except should be('defined)
    }
  }

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