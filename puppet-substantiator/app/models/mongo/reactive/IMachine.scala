package models.mongo.reactive

import reactivemongo.bson._
import play.api.libs.json._
import models.Model._
import models.IReadersWriters
import play.api.http.Writeable

trait IMachine extends IMongoModel {
  def name: String

  def isAlive: Boolean
}

class Machine(override val name: String,
              override val id: Option[BSONObjectID] = Some(BSONObjectID.generate),
              override val isAlive: Boolean = true) extends IMachine {

}

trait IMachineReadersWriters extends IReadersWriters[Machine] {
  override implicit lazy val jsonReader = Machine.MachineJSONReader
  override implicit lazy val jsonWriter = Machine.MachineJSONWriter
  override implicit lazy val bsonReader = Machine.MachineBSONReader
  override implicit lazy val bsonWriter = Machine.MachineBSONWriter
}

object Machine extends IMachineReadersWriters {

  implicit object MachineBSONReader extends IBSONReaderExtended[Machine] {
    def fromBSON(document: BSONDocument) = {
      val doc = document.toTraversable

      new Machine(
        doc.getAs[BSONString]("name").map(_.value).getOrElse(throw errorFrom("BSONRead", "name")),
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[BSONBoolean]("isAlive").map(_.value).getOrElse(throw errorFrom("BSONRead", "name"))
      )
    }
  }

  implicit object MachineBSONWriter extends IBSONWriterExtended[Machine] {
    def toBSON(entity: Machine) =
      BSONDocument(
        "_id" -> entity.id.getOrElse(BSONObjectID.generate),
        "name" -> BSONString(entity.name),
        "isAlive" -> BSONBoolean(entity.isAlive)
      )
  }

  implicit object MachineJSONReader extends Reads[Machine] {
    def reads(json: JsValue) = {
      JsSuccess(new Machine(
        (json \ "name").as[String],
        (json \ "_id").asOpt[String] map {
          id => new BSONObjectID(id)
        },
        (json \ "isAlive").as[Boolean]
      ))
    }
  }

  implicit object MachineJSONWriter extends Writes[Machine] {
    def writes(entity: Machine): JsValue = {
      val list = scala.collection.mutable.Buffer(
        "name" -> JsString(entity.name),
        "isAlive" -> JsBoolean(entity.isAlive))
      if (entity.id.isDefined)
        list.+=("_id" -> JsString(entity.id.get.stringify))
      JsObject(list.toSeq)
    }
  }

  implicit object MachineCriteriaReader extends BaseCriteriaReader {
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

  implicit object MachineUniqueCheckReader extends UniqueKeyReader("name")

}
