package services.repository.mongo.reactive

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, WordSpec}
import org.scalatest.matchers.ShouldMatchers
import models.mongo.reactive._
import concurrent._
import concurrent.ExecutionContext.Implicits.global
import concurrent.duration._

class MachinesRepositoryIntegrationSpec
  extends WordSpec
  with BeforeAndAfter
  with BeforeAndAfterAll
  with ShouldMatchers
  with IMachineRepoHelper
  with TestMongoDbProvider
  with TestMachineRepositoryProvider
  with RepositoryBehaviors[Machine] {

  after {
    clean
  }

  override def afterAll(configMap: Map[String, Any]) {
    db.connection.close()
  }

  "searching for machine names " should {
    "fail with empty database" in {
      val entity = createEntity
      val testList = List("test1", "test2", "test3")
      val resultMap = Await.result(repository.machinesExistByNames(testList), 20 seconds)

      resultMap.keys.toList should equal(testList)
      resultMap.values.forall(_ == false) should be(true)
    }

    "pass with entities" in {
      val entity = createEntity
      Await.result(createEntities(3),10 seconds )
      val insertedList = Await.result(repository.getAllMem(),10 seconds )
      insertedList.size should be(3)
      val testList = insertedList.map(_.name)
      val resultMap = Await.result(repository.machinesExistByNames(testList), 20 seconds)

      resultMap.keys.toList should equal(testList)
      resultMap.values.forall(_ == true) should be(true)
    }
  }
  behave like baseModelRepository
}