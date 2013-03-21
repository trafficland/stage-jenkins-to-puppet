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
import reactivemongo.api.DefaultCollection

trait IRepoTestHelper[TestModel <: IModel[BSONObjectID]] extends IMongoDbProvider with IMongoCollection{

  implicit val bsonReader: BSONReader[TestModel]
  implicit val bsonWriter: BSONWriter[TestModel]

  def createEntity: TestModel

  def createEntities(numberOfEntities: Int): Future[Int]

}

trait IMachineRepoHelper extends IRepoTestHelper[Machine] with AbstractMachineCreator with IMachineReadersWriters {
  def collection(name: String) = db(name)
}

trait AbstractMachineCreator extends IMongoCollection {
  val collectionName: String = "machines"

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
    collection(collectionName).insert[Machine](Enumerator(entities: _*))
  }
}

abstract class AbstractCreator(fColl: (String) => DefaultCollection) {
  def collection(name: String) = fColl(name)
}

trait AbstractAppCreator extends IMongoCollection {
  def machineCreator: AbstractMachineCreator

  val collectionName: String = "apps"

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
    collection(collectionName).insert[App](Enumerator(ents: _*))
  }
}

trait IAppsRepoHelper extends IRepoTestHelper[App] with AbstractAppCreator with IAppReadersWriters {

  def appDb = db

  def machineRepoHelper: IMachineRepoHelper = new IMachineRepoHelper {
    override def db = appDb
  }

  override def collection(name: String) =
    db(name)

  def machineCreator = MachineCreator(collection)
}

trait IMongoCollection {
  def collectionName: String

  def collection(name: String): DefaultCollection

  def clean() = Await.result(cleanAsync(), 10 seconds)

  def cleanAsync() = collection(collectionName).remove(query = BSONDocument(), firstMatchOnly = false)
}

trait IActorsStateRepoHelper extends IRepoTestHelper[ActorState] with AbstractActorsStateCreator {
  def collection(name: String) = db(name)
}

trait AbstractActorsStateCreator extends IMongoCollection with IActorStateReadersWriters {
  val collectionName: String = "actors"

  def createEntity = {
    new ActorState("test999", true, "some state")
  }

  def createEntities(numberOfEntities: Int) = {
    val entities = (0 until numberOfEntities) map {
      index => {
        val count = index + 1
        ActorState("test" + count, true, "some state")
      }
    }
    collection(collectionName).insert[ActorState](Enumerator(entities: _*))
  }
}

case class MachineCreator(fColl: (String) => DefaultCollection) extends AbstractCreator(fColl) with AbstractMachineCreator

case class AppCreator(fColl: (String) => DefaultCollection) extends AbstractCreator(fColl) with AbstractAppCreator {
  def machineCreator = MachineCreator(fColl)
}

case class ActorsStateCreator(fColl: (String) => DefaultCollection) extends AbstractCreator(fColl) with AbstractActorsStateCreator