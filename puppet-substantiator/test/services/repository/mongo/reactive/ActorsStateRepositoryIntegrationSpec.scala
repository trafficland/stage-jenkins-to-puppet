package services.repository.mongo.reactive

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, WordSpec}
import org.scalatest.matchers.ShouldMatchers
import models.mongo.reactive.ActorState
import concurrent._
import concurrent.ExecutionContext.Implicits.global
import concurrent.duration._

class ActorsStateRepositoryIntegrationSpec
  extends WordSpec
  with BeforeAndAfter
  with BeforeAndAfterAll
  with ShouldMatchers
  with IActorsStateRepoHelper
  with TestMongoDbProvider
  with TestActorsStateRepositoryProvider
  with RepositoryBehaviors[ActorState] {

  after {
    clean
  }

  override def afterAll(configMap: Map[String, Any]) {
    db.connection.close()
  }

  "searching for actor by name " should {
    "not find actor with empty db" in {
      val entity = createEntity
      val result = Await.result(repository.getByName(entity.name), 5 seconds)
      result.isEmpty should equal(true)
    }

    "find actor" in {
      val entity = createEntity
      Await.result(repository.create(entity), 3 seconds)
      val result = Await.result(repository.getByName(entity.name), 5 seconds)
      result.isDefined should equal(true)
      entity.isEqualTo(result.get, false) should be(true)
    }
  }
  behave like baseModelRepository
}