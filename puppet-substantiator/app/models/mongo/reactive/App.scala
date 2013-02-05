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

class App(override val id: Option[BSONObjectID],
          override val name: String,
          override val expected: String,
          override val actualCluster: List[AppMachineState]) extends IApp {

}

object App {

  protected implicit val bsonReader = AppMachineState.AppMachineBSONReader
  protected implicit val bsonWriter = AppMachineState.AppMachineStateBSONWriter

  protected implicit val jsonReader = AppMachineState.AppMachineJSONReader
  protected implicit val jsonWriter = AppMachineState.AppMachineJSONWriter

  implicit object AppBSONReader extends BSONReader[App] {
    def fromBSON(document: BSONDocument) = {
      val doc = document.toTraversable
      val array = doc.getAs[BSONArray]("actualCluster").getOrElse(throw errorFrom("BSONRead", "actualCluster"))
      new App(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[BSONString]("name").map(_.value).getOrElse(throw errorFrom("BSONRead", "name")),
        doc.getAs[BSONString]("expected").map(_.value).getOrElse(throw errorFrom("BSONRead", "expected")),
        bsonReader.fromBSONArray(array)
      )
    }
  }

  implicit object AppBSONWriter extends BSONWriter[App] {
    def toBSON(entity: App) =
      BSONDocument(
        "_id" -> entity.id.getOrElse(BSONObjectID.generate),
        "name" -> BSONString(entity.name),
        "expected" -> BSONString(entity.expected),
        "actualCluster" -> bsonWriter.toBSONArray(entity.actualCluster)
      )
  }

  implicit object AppJSONReader extends Reads[App] {
    def reads(json: JsValue) = {
      JsSuccess(new App(
        (json \ "_id").asOpt[String] map {
          id => new BSONObjectID(id)
        },
        (json \ "name").as[String],
        (json \ "expected").as[String],
        jsonReader.readsArray((json \ "actualCluster").as[JsArray])
      ))
    }
  }

  implicit object AppJSONWriter extends Writes[App] {
    def writes(entity: App): JsValue = {
      val list = scala.collection.mutable.Buffer(
        "name" -> JsString(entity.name),
        "expected" -> JsString(entity.expected),
        "actualCluster" -> jsonWriter.writesArray(entity.actualCluster))

      if (entity.id.isDefined)
        list.+=("_id" -> JsString(entity.id.get.stringify))
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

      doc
    }
  }

  implicit object AppUniqueCheckReader extends UniqueKeyReader("name")

}
