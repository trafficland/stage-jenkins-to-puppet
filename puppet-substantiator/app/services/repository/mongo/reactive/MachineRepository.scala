package services.repository.mongo.reactive

import models.mongo.reactive._
import util.ConfigurationProvider

trait IMachineRepository extends MongoBaseRepository[Machine] with IMongoUniqueCheckRepository[Machine]

abstract class MachineRepository
  extends IMachineRepository {

  override protected def collectionName = "machines"

  implicit val reader = Machine.MachineBSONReader
  implicit val writer = Machine.MachineBSONWriter
}

trait IMachineRepositoryProvider
  extends IMongoRepositoryProvider[Machine] {

  def repository: IMachineRepository
}

trait MachineRepositoryProvider
  extends IMachineRepositoryProvider {

  def repository = MachineRepository
}

object MachineRepository
  extends MachineRepository
  with IMongoDbProvider
  with ConfigurationProvider
  with MachineRepositoryProvider