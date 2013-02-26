package models.mongo.reactive

import reactivemongo.bson._
import play.api.libs.json._
import models.Model._
import models.IReadersWriters
import play.api.http.Writeable
import models.json.{IReadsExtended, IWritesExtended}

case class Machine(var name: String,
                   override var id: Option[BSONObjectID] = Some(BSONObjectID.generate),
                   val isAlive: Boolean = true) extends IMongoModel[Machine] {
  def isEqualTo(other: Machine, useID: Boolean): Boolean = {
    super.isEqualTo(other, useID) &&
      name == other.name &&
      isAlive == other.isAlive
  }
}

trait IMachineReadersWriters extends IReadersWriters[Machine] {
  import Machine._
  override implicit lazy val jsonReader = JSONReader
  override implicit lazy val jsonWriter = JSONWriter
  override implicit lazy val bsonReader = BSONReader
  override implicit lazy val bsonWriter = BSONWriter

  implicit val criteriaReader = CriteriaReader
  implicit val uniqueCheckReader = UniqueCheckReader
}

object Machine extends IMachineReadersWriters {

  implicit object BSONReader extends IBSONReaderExtended[Machine] {
    def fromBSON(document: BSONDocument) = {
      val doc = document.toTraversable

      new Machine(
        doc.getAs[BSONString]("name").map(_.value).getOrElse(throw errorFrom("BSONRead", "name")),
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[BSONBoolean]("isAlive").map(_.value).getOrElse(throw errorFrom("BSONRead", "name"))
      )
    }
  }

  implicit object BSONWriter extends IBSONWriterExtended[Machine] {
    def toBSON(entity: Machine) =
      BSONDocument(
        "_id" -> entity.id.getOrElse(BSONObjectID.generate),
        "name" -> BSONString(entity.name),
        "isAlive" -> BSONBoolean(entity.isAlive)
      )
  }

  implicit object JSONReader extends IReadsExtended[Machine] {
    def reads(json: JsValue) = {
      val m = new Machine(
        (json \ "name").as[String],
        (json \ "_id").asOpt[String] map {
          id => new BSONObjectID(id)
        },
        (json \ "isAlive").as[Boolean]
      )
      JsSuccess(m)
    }
  }

  implicit object JSONWriter extends IWritesExtended[Machine] {
    def writes(entity: Machine): JsValue = {
      val list = scala.collection.mutable.Buffer(
        "name" -> JsString(entity.name),
        "isAlive" -> JsBoolean(entity.isAlive))
      if (entity.id.isDefined)
        list.+=("_id" -> JsString(entity.id.get.stringify))
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

      doc
    }
  }

  implicit object UniqueCheckReader extends UniqueKeyReader("name")

}
