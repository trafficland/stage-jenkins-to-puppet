package util.playframework

import reactivemongo.api.{DefaultDB, MongoConnection}
import play.api.Application
import collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

object MongoDBTestConnectionManager {

  protected var testMongoDBConnection: Option[MongoConnection] = None
  protected var testMongoDBInterface: Option[DefaultDB] = None
  protected val lock = new AnyRef

  def getTestMongoDBInterface(app: Application): DefaultDB = {
    lock.synchronized {
      testMongoDBInterface match {
        case Some(db) =>
          println("SPEC SETUP: MongoDB connection already exists")
          db
        case None => {
          println("SPEC SETUP: Creating test MongoDB connection.")
          val newConnection = MongoConnection(app.configuration.getStringList("mongodb.servers").get.toList)
          val newDb = newConnection(app.configuration.getString("mongodb.db").get)
          testMongoDBConnection = Some(newConnection)
          testMongoDBInterface = Some(newDb)
          println("SPEC SETUP: Created test MongoDB connection.")
          newDb
        }
      }
    }
  }

  def closeMongoDBConnection() {
    testMongoDBInterface match {
      case Some(db) =>
        testMongoDBConnection.get.close()
        testMongoDBInterface = None
        testMongoDBConnection = None
        println("SPEC CLEANUP: Closed the test MongoDB connection.")
      case None => println("SPEC CLEANUP: There was no open MongoDB connection to be closed.")
    }
  }

}
