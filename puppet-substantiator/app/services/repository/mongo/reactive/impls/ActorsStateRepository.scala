package services.repository.mongo.reactive.impls

import models.mongo.reactive.{IActorStateReadersWriters, ActorState}
import util.ConfigurationProvider
import services.repository.mongo.reactive._

trait IActorsStateRepository
  extends MongoBaseRepository[ActorState]
  with IMongoUniqueCheckRepository[ActorState] with IActorStateReadersWriters

abstract class ActorsStateRepository
  extends IActorsStateRepository {
  override protected def collectionName = "actors"
}

trait IActorsStateRepositoryProvider
  extends IMongoRepositoryProvider[ActorState] with IActorStateReadersWriters {
  def repository: IActorsStateRepository
}

trait ActorsStateRepositoryProvider
  extends IActorsStateRepositoryProvider {
  override val repository = ActorsStateRepository
}

object ActorsStateRepository
  extends ActorsStateRepository
  with IMongoDbProvider
  with ConfigurationProvider

class ActorStateRepositoryException(msg: String) extends Exception(msg)