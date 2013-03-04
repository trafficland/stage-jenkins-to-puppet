package models.mongo.reactive

import play.api.libs.json._
import reactivemongo.bson._
import models.Model._
import models.IReadersWriters
import models.json.{IWritesExtended, IReadsExtended}

case class ActorState(
                       var name: String,
                       val isAlive: Boolean,
                       val state: String,
                       override var id: Option[BSONObjectID] = Some(BSONObjectID.generate)) extends IMongoModel[ActorState] {
  def getWithID = this.copy(id = Some(BSONObjectID.generate))

  def isEqualTo(other: ActorState, useID: Boolean): Boolean = {
    super.isEqualTo(other, useID) &&
      name == other.name &&
      isAlive == other.isAlive &&
      state == other.state
  }
}

trait IActorStateReadersWriters
  extends IReadersWriters[ActorState] {

  import ActorStateDomain._

  override implicit val bsonReader = BSONReader
  override implicit val bsonWriter = BSONWriter
  override implicit val jsonReader = JSONReader
  override implicit val jsonWriter = JSONWriter
  implicit val criteriaReader = CriteriaReader
  implicit val uniqueCheckReader = UniqueCheckReader
}

object ActorStateDomain extends IActorStateReadersWriters {

  implicit object BSONReader extends IBSONReaderExtended[ActorState] {
    def fromBSON(document: BSONDocument) = {
      val doc = document.toTraversable
      ActorState(
        doc.getAs[BSONString]("name").map(_.value).getOrElse(throw errorFrom("BSONRead", "name")),
        doc.getAs[BSONBoolean]("isAlive").map(_.value).getOrElse(throw errorFrom("BSONRead", "isAlive")),
        doc.getAs[BSONString]("state").map(_.value).getOrElse(throw errorFrom("BSONRead", "state")),
        doc.getAs[BSONObjectID]("_id")
      )
    }
  }

  implicit object BSONWriter extends IBSONWriterExtended[ActorState] {
    def toBSON(entity: ActorState) =
      BSONDocument(
        "_id" -> entity.id.getOrElse(BSONObjectID.generate),
        "name" -> BSONString(entity.name),
        "isAlive" -> BSONBoolean(entity.isAlive),
        "state" -> BSONString(entity.state)
      )
  }

  implicit object JSONReader extends IReadsExtended[ActorState] {
    def reads(json: JsValue) = {
      JsSuccess(ActorState(
        (json \ "name").as[String],
        (json \ "isAlive").as[Boolean],
        (json \ "state").as[String],
        (json \ "_id").asOpt[String] map {
          id => new BSONObjectID(id)
        })
      )
    }
  }

  implicit object JSONWriter extends IWritesExtended[ActorState] {
    def writes(entity: ActorState): JsValue = {
      val list = scala.collection.mutable.Buffer(
        "name" -> JsString(entity.name),
        "isAlive" -> JsBoolean(entity.isAlive),
        "state" -> JsString(entity.state))

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

      (json \ "state").asOpt[String] foreach {
        port =>
          doc = doc append ("state" -> new BSONString(port))
      }
      doc
    }
  }

  implicit object UniqueCheckReader extends UniqueKeyReader("name")

}
