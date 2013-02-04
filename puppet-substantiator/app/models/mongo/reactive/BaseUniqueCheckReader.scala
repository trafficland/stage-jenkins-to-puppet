package models.mongo.reactive

import play.api.libs.json._
import reactivemongo.bson._
import services.repository.IUniqueCheck
import services.repository.mongo.reactive.MongoUniqueCheck

abstract class BaseUniqueCheckReader extends Reads[IUniqueCheck[BSONObjectID,BSONDocument]] {
  private def parseId(json: JsValue) = {
    (json \ "_id").asOpt[String].map(id => new BSONObjectID(id))
  }

  protected def parseOtherCriteria(json: JsValue): BSONDocument

  def reads(json: JsValue) = {
    JsSuccess(MongoUniqueCheck(parseId(json), parseOtherCriteria(json)))
  }
}

abstract class UniqueKeyReader(key: String) extends BaseUniqueCheckReader {
  protected def parseOtherCriteria(json: JsValue) = {
    (json \ key).asOpt[String].map(value => BSONDocument(key -> BSONRegex("^" + value + "$", "i"))).getOrElse(BSONDocument())
  }
}
