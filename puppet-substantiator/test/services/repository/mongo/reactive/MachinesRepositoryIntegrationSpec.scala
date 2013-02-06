package services.repository.mongo.reactive

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, WordSpec}
import org.scalatest.matchers.ShouldMatchers
import play.api.libs.iteratee.{Iteratee, Enumerator}
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import models.mongo.reactive._
import concurrent.ExecutionContext.Implicits.global
import concurrent.Await
import concurrent.duration._

class MachinesRepositoryIntegrationSpec
  extends WordSpec
  with BeforeAndAfter
  with BeforeAndAfterAll
  with ShouldMatchers
  with TestMongoDbProvider
  with TestMachineRepositoryProvider
  with RepositoryBehaviors[Machine] {

  override def collectionName = "machines"

  after {
    clean
  }

  def clean() = Await.result(cleanAsync(), 10 seconds)

  def cleanAsync() = db.collection(collectionName).remove(query = BSONDocument(), firstMatchOnly = false)

  override def afterAll(configMap: Map[String, Any]) {
    db.connection.close()
  }

  implicit val reader = Machine.MachineBSONReader
  implicit val writer = Machine.MachineBSONWriter

  def createEntity = {
    new Machine(Some(BSONObjectID.generate), "testMachineName1?")
  }

  def createEntities(numberOfEntities: Int) = {
    var counter = 1
    val entities = (0 until numberOfEntities) map {
      index => {
        val mac = new Machine(Some(BSONObjectID.generate), "testMachineName" + counter)
        counter += 1
        mac
      }
    }

    db(collectionName).insert[Machine](Enumerator(entities: _*))
  }

  behave like baseModelRepository
}