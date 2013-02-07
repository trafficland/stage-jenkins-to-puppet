package models.mongo.reactive

import reactivemongo.bson._
import play.api.libs.json._
import models.Model._

trait IMachine extends IMongoModel {
  def name: String

  def isAlive: Boolean
}

class Machine(override val id: Option[BSONObjectID],
              override val name: String,
              override val isAlive: Boolean = true) extends IMachine {

}

object Machine {

  implicit object MachineBSONReader extends IBSONReaderExtended[Machine] {
    def fromBSON(document: BSONDocument) = {
      val doc = document.toTraversable

      new Machine(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[BSONString]("key").map(_.value).getOrElse(throw errorFrom("BSONRead", "key")),
        doc.getAs[BSONBoolean]("isAlive").map(_.value).getOrElse(throw errorFrom("BSONRead", "key"))
      )
    }
  }

  implicit object MachineBSONWriter extends IBSONWriterExtended[Machine] {
    def toBSON(entity: Machine) =
      BSONDocument(
        "_id" -> entity.id.getOrElse(BSONObjectID.generate),
        "key" -> BSONString(entity.name),
        "isAlive" -> BSONBoolean(entity.isAlive)
      )
  }

  implicit object MachineJSONReader extends Reads[Machine] {
    def reads(json: JsValue) = {
      JsSuccess(new Machine(
        (json \ "_id").asOpt[String] map {
          id => new BSONObjectID(id)
        },
        (json \ "key").as[String],
        (json \ "isAlive").as[Boolean]
      ))
    }
  }

  implicit object MachineJSONWriter extends Writes[Machine]{
    def writes(entity: Machine): JsValue = {
      val list = scala.collection.mutable.Buffer(
        "key" -> JsString(entity.name),
        "disabled" -> JsBoolean(entity.isAlive))

      if (entity.id.isDefined)
        list.+=("_id" -> JsString(entity.id.get.stringify))
      JsObject(list.toSeq)
    }
  }

  implicit object MachineCriteriaReader extends BaseCriteriaReader {
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

  implicit object MachineUniqueCheckReader extends UniqueKeyReader("key")

}
