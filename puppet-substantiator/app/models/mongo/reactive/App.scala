package models.mongo.reactive

import reactivemongo.bson.handlers._
import play.api.libs.json._
import reactivemongo.bson._
import models.Model._


trait IApp extends IMongoModel {
  def name: String

  def expected: String

  //list of machines to actual App state / actual
  def actualCluster: List[AppMachineState]
}

case class App(override val id: Option[BSONObjectID],
               override val name: String,
               override val expected: String,
               override val actualCluster: List[AppMachineState]) extends IApp {

}

object App {

  protected implicit val bsonReaderAppMach = AppMachineState.AppMachineBSONReader
  protected implicit val bsonWriterAppMach = AppMachineState.AppMachineStateBSONWriter

  protected implicit val jsonReaderAppMac = AppMachineState.AppMachineJSONReader
  protected implicit val jsonWriterAppMach = AppMachineState.AppMachineJSONWriter

  implicit object AppBSONReader extends BSONReader[App] {
    def fromBSON(document: BSONDocument) = {
      val doc = document.toTraversable
      App(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[BSONString]("key").map(_.value).getOrElse(throw errorFrom("BSONRead", "key")),
        doc.getAs[BSONString]("expected").map(_.value).getOrElse(throw errorFrom("BSONRead", "expected")),
        bsonReaderAppMach.fromBSONArray(doc.getAs[BSONArray]("actualCluster").getOrElse(throw errorFrom("BSONRead", "actualCluster")))
      )
    }
  }

  implicit object AppBSONWriter extends BSONWriter[App] {
    def toBSON(entity: App) =
      BSONDocument(
        "_id" -> entity.id.getOrElse(BSONObjectID.generate),
        "key" -> BSONString(entity.name),
        "expected" -> BSONString(entity.expected),
        "actualCluster" -> bsonWriterAppMach.toBSONArray(entity.actualCluster)
      )
  }

  implicit object AppJSONReader extends Reads[App] {
    def reads(json: JsValue) = {
      JsSuccess(App(
        (json \ "_id").asOpt[String] map {
          id => new BSONObjectID(id)
        },
        (json \ "key").as[String],
        (json \ "expected").as[String],
        jsonReaderAppMac.readsArray((json \ "actualCluster").as[JsArray])
      ))
    }
  }

  implicit object AppJSONWriter extends Writes[App] {
    def writes(entity: App): JsValue = {
      val list = scala.collection.mutable.Buffer(
        "key" -> JsString(entity.name),
        "expected" -> JsString(entity.expected),
        "actualCluster" -> jsonWriterAppMach.writesArray(entity.actualCluster))

      if (entity.id.isDefined)
        list.+=("_id" -> JsString(entity.id.get.stringify))
      JsObject(list.toSeq)
    }
  }

  implicit object AppCriteriaReader extends BaseCriteriaReader {
    def criteria(json: JsValue) = {

      var doc = BSONDocument()

      (json \ "key").asOpt[String] foreach {
        name =>
          doc = doc append ("key" -> new BSONString(name))
      }

      (json \ "isAlive").asOpt[Boolean] foreach {
        isAlive =>
          doc = doc append ("isAlive" -> new BSONBoolean(isAlive))
      }

      doc
    }
  }

  implicit object AppUniqueCheckReader extends UniqueKeyReader("key")

}
