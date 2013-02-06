package services.repository.mongo.reactive.impls

import models.mongo.reactive._
import util.ConfigurationProvider
import services.repository.mongo.reactive._

trait IAppsRepository extends MongoBaseRepository[App] with IMongoUniqueCheckRepository[App]

abstract class AppsRepository
  extends IAppsRepository {

  override protected def collectionName = "apps"

  implicit val reader = App.AppBSONReader
  implicit val writer = App.AppBSONWriter
}

trait IAppsRepositoryProvider
  extends IMongoRepositoryProvider[App] {

  def repository: IAppsRepository
}

trait AppRepositoryProvider
  extends IAppsRepositoryProvider {

  override val repository = AppsRepository
}

object AppsRepository
  extends AppsRepository
  with IMongoDbProvider
  with ConfigurationProvider
  with AppRepositoryProvider