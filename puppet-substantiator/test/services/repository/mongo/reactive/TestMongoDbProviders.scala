package services.repository.mongo.reactive

import impls.MachinesRepository
import models.mongo.reactive._
import play.api.Configuration
import org.scalatest.mock.MockitoSugar.mock

class TestMachineRepository extends MachinesRepository with TestMongoDbProvider {
  val config = mock[Configuration]
  val configuration = config
}

trait TestMachineRepositoryProvider
  extends IMongoRepositoryProvider[Machine] {

  override  def repository = new TestMachineRepository()

}


