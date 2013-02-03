package services.repository.mongo.reactive

import reactivemongo.api.DefaultDB
import services.repository.IDbProvider

trait IMongoDbProvider extends IDbProvider[DefaultDB] {
  def db: DefaultDB
}
