package services.repository.mongo.reactive

import services.repository.mongo.reactive.impls._
import models.mongo.reactive._
import play.api.Configuration
import org.scalatest.mock.MockitoSugar.mock

trait TestMachineRepositoryProvider
  extends IMongoRepositoryProvider[Machine] {

  override lazy val repository = new MachinesRepository with TestMongoDbProvider {
    val config = mock[Configuration]
    val configuration = config
  }
}

trait TestAppsRepositoryProvider
  extends IMongoRepositoryProvider[App] {

  override lazy val repository = new AppsRepository with TestMongoDbProvider {
    val config = mock[Configuration]
    val configuration = config
    val gConfig = config

    override lazy val machineRepo = new MachinesRepository with TestMongoDbProvider {
      val config = mock[Configuration]
      val configuration = gConfig
    }
  }
}


