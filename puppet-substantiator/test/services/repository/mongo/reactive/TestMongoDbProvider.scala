
package services.repository.mongo.reactive

import services.repository.IDbProvider
import reactivemongo.api.{DefaultDB, MongoConnection}
import collection.JavaConversions._
import concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory
import java.io.File

trait TestMongoDbProvider extends IDbProvider[DefaultDB] {
  lazy val _config = ConfigFactory.parseFile(new File("conf/test.conf"))
  override lazy val db:DefaultDB = MongoConnection(_config.getStringList("mongodb.servers").toList)(_config.getString("mongodb.db"))
}
