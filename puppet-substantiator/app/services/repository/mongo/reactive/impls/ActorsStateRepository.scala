package services.repository.mongo.reactive.impls

import models.mongo.reactive.ActorState
import util.ConfigurationProvider
import services.repository.mongo.reactive._

trait IActorsStateRepository extends MongoBaseRepository[ActorState] with IMongoUniqueCheckRepository[ActorState]

abstract class ActorsStateRepository
  extends IActorsStateRepository {
  override protected def collectionName = "actorsstate"
  implicit val reader = ActorState.BSONReader
  implicit val writer = ActorState.BSONWriter
}

trait IActorsStateRepositoryProvider
  extends IMongoRepositoryProvider[ActorState] {

  def repository: IActorsStateRepository
}

trait ActorsStateRepositoryProvider
  extends IActorsStateRepositoryProvider {

  override val repository = ActorsStateRepository
}

object ActorsStateRepository
  extends ActorsStateRepository
  with IMongoDbProvider
  with ConfigurationProvider {
  protected def machineRepo = MachinesRepository
}

class ActorStateRepositoryException(msg: String) extends Exception(msg)