package util.playframework

import reactivemongo.core.commands.DropDatabase
import play.api.Application
import scala.concurrent.ExecutionContext.Implicits.global
import services.repository.mongo.reactive.IMongoCollection

trait LiveMongoDBConnection extends IMongoCollection {

  protected implicit val app:Application

  private def getTestMongoDBInterface() = MongoDBTestConnectionManager.getTestMongoDBInterface(app)

  protected def dropDatabase() = {
    getTestMongoDBInterface.command(new DropDatabase())
  }

  override def collection(collectionName: String) =
    getTestMongoDBInterface().collection(collectionName)

  protected def closeConnection() = MongoDBTestConnectionManager.closeMongoDBConnection()
}
