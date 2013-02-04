package services.repository.mongo.reactive

import models.mongo.reactive._
import util.ConfigurationProvider

trait IMachinesRepository extends MongoBaseRepository[Machine] with IMongoUniqueCheckRepository[Machine]

abstract class MachinesRepository
  extends IMachinesRepository {

  override protected def collectionName = "machines"

  implicit val reader = Machine.MachineBSONReader
  implicit val writer = Machine.MachineBSONWriter
}

trait IMachinesRepositoryProvider
  extends IMongoRepositoryProvider[Machine] {

  def repository: IMachinesRepository
}

trait MachineRepositoryProvider
  extends IMachinesRepositoryProvider {

  def repository = MachinesRepository
}

object MachinesRepository
  extends MachinesRepository
  with IMongoDbProvider
  with ConfigurationProvider
  with MachineRepositoryProvider