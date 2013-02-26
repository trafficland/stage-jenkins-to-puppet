package models.mongo.reactive

import play.api.libs.json._
import reactivemongo.bson._
import models.Model._
import models.IReadersWriters
import models.json.{IWritesExtended, IReadsExtended}

case class App(
                var name: String,
                val expected: String,
                val testUrl: String,
                val actualCluster: List[AppMachineState],
                val port: Option[String] = Some("9000"),
                override var id: Option[BSONObjectID] = Some(BSONObjectID.generate)) extends IMongoModel[App] {
  def getWithID = this.copy(id = Some(BSONObjectID.generate))

  def isEqualTo(other: App, useID: Boolean): Boolean = {
    super.isEqualTo(other, useID) &&
      expected == other.expected &&
      name == other.name &&
      testUrl == other.testUrl &&
      port == other.port &&
      actualCluster.forall(appMach => other.actualCluster.exists(otherAppMach =>
        otherAppMach.machineName == appMach.machineName &&
          otherAppMach.actual == appMach.actual))
  }
}

trait IAppReadersWriters extends IReadersWriters[App] {

  import App._

  override implicit val bsonReader = BSONReader
  override implicit val bsonWriter = BSONWriter
  override implicit val jsonReader = JSONReader
  override implicit val jsonWriter = JSONWriter

  implicit val criteriaReader = CriteriaReader
  implicit val uniqueCheckReader = UniqueCheckReader

  import AppMachineState._

  implicit val bsonReaderAppMach = AppMachineBSONReader
  implicit val bsonWriterAppMach = AppMachineStateBSONWriter
  implicit val jsonReaderAppmach = AppMachineJSONReader
  implicit val jsonWriterAppMach = AppMachineJSONWriter

}

object App extends IAppReadersWriters {

  implicit object BSONReader extends IBSONReaderExtended[App] {
    def fromBSON(document: BSONDocument) = {
      val doc = document.toTraversable
      App(
        doc.getAs[BSONString]("name").map(_.value).getOrElse(throw errorFrom("BSONRead", "name")),
        doc.getAs[BSONString]("expected").map(_.value).getOrElse(throw errorFrom("BSONRead", "expected")),
        doc.getAs[BSONString]("testUrl").map(_.value).getOrElse(throw errorFrom("BSONRead", "testUrl")),
        bsonReaderAppMach.fromBSONArray(doc.getAs[BSONArray]("actualCluster").getOrElse(throw errorFrom("BSONRead", "actualCluster"))),
        doc.getAs[BSONString]("port").map(_.value),
        doc.getAs[BSONObjectID]("_id")
      )
    }
  }

  implicit object BSONWriter extends IBSONWriterExtended[App] {
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

  implicit object JSONReader extends IReadsExtended[App] {
    def reads(json: JsValue) = {
      JsSuccess(App(
        (json \ "name").as[String],
        (json \ "expected").as[String],
        (json \ "testUrl").as[String],
        jsonReaderAppmach.readsArray((json \ "actualCluster").as[JsArray])
        ,
        (json \ "port").asOpt[String],
        (json \ "_id").asOpt[String] map {
          id => new BSONObjectID(id)
        })
      )
    }
  }

  implicit object JSONWriter extends IWritesExtended[App] {
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

  implicit object CriteriaReader extends BaseCriteriaReader {
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

  implicit object UniqueCheckReader extends UniqueKeyReader("name")

}
