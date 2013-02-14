package services.repository.mongo.reactive

import impls.{AppsRepository, AppRepositoryException}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, WordSpec}
import org.scalatest.matchers.ShouldMatchers
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import models.mongo.reactive._
import concurrent.ExecutionContext.Implicits.global
import concurrent._
import concurrent.duration._

class AppsRepositoryIntegrationSpec
  extends WordSpec
  with BeforeAndAfter
  with BeforeAndAfterAll
  with ShouldMatchers
  with IAppsRepoHelper
  with TestMongoDbProvider
  with TestAppsRepositoryProvider
  with RepositoryBehaviors[App] {


  before {
    Await.result(machineRepoHelper.createEntities(2), 10 seconds)
  }
  after {
    machineRepoHelper.clean()
    Await.result(db.collection(collectionName).remove(query = BSONDocument(), firstMatchOnly = false), 10 seconds)
  }

  override def afterAll(configMap: Map[String, Any]) {
    machineRepoHelper.clean()
    db.connection.close()
  }

  "creation with no machines" should {
    "fail" in {
      machineRepoHelper.clean()
      val failed = Await.result(repository.create(createEntity), 20 seconds) match {
        case Left(app) =>
          app
        case Right(ex) =>
          None
      }
      failed should be('empty)
    }
  }
  behave like baseModelRepository

}