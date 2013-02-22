package models.mongo.reactive

import play.api.libs.json._
import reactivemongo.bson._
import models.Model._
import models.IReadersWriters
import models.json.{IWritesExtended, IReadsExtended}

case class App(
                var name: String,
                val expected: String,
                val testUrl:String,
                val port: Option[String] = Some("9000"),
                val actualCluster: List[AppMachineState],
                override var id: Option[BSONObjectID] = Some(BSONObjectID.generate)) extends IMongoModel[App] {
  def getWithID = this.copy(id = Some(BSONObjectID.generate))

  def isEqualTo(other: App, useID: Boolean): Boolean = {
    super.isEqualTo(other, useID) &&
      expected == other.expected &&
      name == other.name &&
      actualCluster.forall(appMach => other.actualCluster.exists(otherAppMach =>
        otherAppMach.machineName == appMach.machineName &&
          otherAppMach.actual == appMach.actual))
  }
}

trait IAppReadersWriters extends IReadersWriters[App] {
  override implicit val bsonReader = App.AppBSONReader
  override implicit val bsonWriter = App.AppBSONWriter
  override implicit val jsonReader = App.AppJSONReader
  override implicit val jsonWriter = App.AppJSONWriter

  implicit val bsonReaderAppMach = AppMachineState.AppMachineBSONReader
  implicit val bsonWriterAppMach = AppMachineState.AppMachineStateBSONWriter
  implicit val jsonReaderAppmach = AppMachineState.AppMachineJSONReader
  implicit val jsonWriterAppMach = AppMachineState.AppMachineJSONWriter

}

object App extends IAppReadersWriters {

  implicit object AppBSONReader extends IBSONReaderExtended[App] {
    def fromBSON(document: BSONDocument) = {
      val doc = document.toTraversable
      App(
        doc.getAs[BSONString]("name").map(_.value).getOrElse(throw errorFrom("BSONRead", "name")),
        doc.getAs[BSONString]("expected").map(_.value).getOrElse(throw errorFrom("BSONRead", "expected")),
        doc.getAs[BSONString]("testUrl").map(_.value).getOrElse(throw errorFrom("BSONRead", "testUrl")),
        doc.getAs[BSONString]("port").map(_.value),
        bsonReaderAppMach.fromBSONArray(doc.getAs[BSONArray]("actualCluster").getOrElse(throw errorFrom("BSONRead", "actualCluster"))),
        doc.getAs[BSONObjectID]("_id")
      )
    }
  }

  implicit object AppBSONWriter extends IBSONWriterExtended[App] {
    def toBSON(entity: App) =
      BSONDocument(
        "_id" -> entity.id.getOrElse(BSONObjectID.generate),
        "name" -> BSONString(entity.name),
        "expected" -> BSONString(entity.expected),
        "testUrl" -> BSONString(entity.testUrl),
        "port" -> BSONString(entity.expected),
        "actualCluster" -> bsonWriterAppMach.toBSONArray(entity.actualCluster)
      )
  }

  implicit object AppJSONReader extends IReadsExtended[App] {
    def reads(json: JsValue) = {
      JsSuccess(App(
        (json \ "name").as[String],
        (json \ "expected").as[String],
        (json \ "testUrl").as[String],
        (json \ "port").asOpt[String],
        jsonReaderAppmach.readsArray((json \ "actualCluster").as[JsArray])
        ,
        (json \ "_id").asOpt[String] map {
          id => new BSONObjectID(id)
        })
      )
    }
  }

  implicit object AppJSONWriter extends IWritesExtended[App] {
    def writes(entity: App): JsValue = {
      val list = scala.collection.mutable.Buffer(
        "name" -> JsString(entity.name),
        "expected" -> JsString(entity.expected),
        "testUrl" -> JsString(entity.testUrl),
        "actualCluster" -> jsonWriterAppMach.writesArray(entity.actualCluster))

      if (entity.id.isDefined)
        list.+=("_id" -> JsString(entity.id.get.stringify))
      if (entity.port.isDefined)
        list.+=("port" -> JsString(entity.port.get))
      JsObject(list.toSeq)
    }
  }

  implicit object AppCriteriaReader extends BaseCriteriaReader {
    def criteria(json: JsValue) = {

      var doc = BSONDocument()

      (json \ "name").asOpt[String] foreach {
        name =>
          doc = doc append ("name" -> new BSONString(name))
      }

      (json \ "isAlive").asOpt[Boolean] foreach {
        isAlive =>
          doc = doc append ("isAlive" -> new BSONBoolean(isAlive))
      }

      (json \ "port").asOpt[String] foreach {
        port =>
          doc = doc append ("port" -> new BSONString(port))
      }
      doc
    }
  }

  implicit object AppUniqueCheckReader extends UniqueKeyReader("name")

}
