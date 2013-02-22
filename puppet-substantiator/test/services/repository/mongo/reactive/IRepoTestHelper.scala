package services.repository.mongo.reactive

import concurrent.{Future, Await}
import concurrent.ExecutionContext.Implicits.global
import concurrent.duration._
import reactivemongo.bson.handlers.{BSONWriter, BSONReader}
import reactivemongo.bson.{BSONObjectID, BSONDocument}
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import models.mongo.reactive._
import play.api.libs.iteratee.Enumerator
import models.IModel

trait IRepoTestHelper[TestModel <: IModel[BSONObjectID]] extends IMongoDbProvider {

  implicit val reader: BSONReader[TestModel]
  implicit val writer: BSONWriter[TestModel]

  def createEntity: TestModel

  def createEntities(numberOfEntities: Int): Future[Int]

  def collectionName: String

  def clean() = Await.result(cleanAsync(), 10 seconds)

  def cleanAsync() = db.collection(collectionName).remove(query = BSONDocument(), firstMatchOnly = false)
}

trait IMachineRepoHelper extends IRepoTestHelper[Machine] {

  implicit val reader: BSONReader[Machine] = Machine.MachineBSONReader
  implicit val writer: BSONWriter[Machine] = Machine.MachineBSONWriter

  override val collectionName: String = "machines"

  def createEntity = {
    new Machine("testMachineName1?")
  }

  def createEntities(numberOfEntities: Int) = {
    val entities = (0 until numberOfEntities) map {
      index => {
        val count = index + 1
        val mac = new Machine("testMachineName" + count)
        mac
      }
    }
    db(collectionName).insert[Machine](Enumerator(entities: _*))
  }
}

trait IAppsRepoHelper extends IRepoTestHelper[App] {
  implicit val reader: BSONReader[App] = App.AppBSONReader
  implicit val writer: BSONWriter[App] = App.AppBSONWriter

  override val collectionName: String = "apps"

  def appDb = db

  def machineRepoHelper: IMachineRepoHelper = new IMachineRepoHelper {
    override def db = appDb
  }

  def createEntity = {
    new App("app199", "1.0.0", "admin/version", List(
      AppMachineState("testMachineName1", Some("0.0.1")),
      AppMachineState("testMachineName2", Some("0.0.1"))))
  }

  def makeSomeEntities(numberOfEntities: Int): IndexedSeq[App] = {
    (0 until numberOfEntities) map {
      index => {
        val count = index + 1
        new App("app" + count, "1.0.0", "admin/version", List(
          AppMachineState("testMachineName1", Some("0.0.1")),
          AppMachineState("testMachineName2", Some("0.0.1"))))
      }
    }
  }

  def createEntities(numberOfEntities: Int) = {
    val ents = makeSomeEntities(numberOfEntities)
    db(collectionName).insert[App](Enumerator(ents: _*))
  }
}
