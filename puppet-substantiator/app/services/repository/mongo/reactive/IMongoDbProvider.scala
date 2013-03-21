package services.repository.mongo.reactive

import reactivemongo.api.{MongoConnection, DefaultDB}
import services.repository.IDbProvider
import util.ConfigurationProvider
import collection.JavaConversions._
import concurrent.ExecutionContext.Implicits.global

trait IMongoDbProvider extends IDbProvider[DefaultDB] {
  def db: DefaultDB = GlobalReactiveMongoDb.db
}

object GlobalReactiveMongoDb extends ConfigurationProvider {
  lazy val db = {
    val servers = configuration.getStringList("mongodb.servers").get.toList
    val database = configuration.getString("mongodb.db").get
    MongoConnection(servers)(database)
  }
}
