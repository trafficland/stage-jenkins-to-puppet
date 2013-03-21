package models.mongo.reactive

import reactivemongo.bson.{BSONDocument, BSONArray}
import reactivemongo.bson.handlers.{BSONWriter, BSONReader}

trait IBSONWriterExtended[Model] extends BSONWriter[Model] {
  def toBSONArray(many: List[Model]): BSONArray = {
    BSONArray.apply(many.map(toBSON): _*)
  }
}

trait IBSONReaderExtended[Model] extends BSONReader[Model] {
  def fromBSONArray(bsonArray: BSONArray): List[Model] =
    bsonArray.toTraversable.toList.map(bson => fromBSON(bson.asInstanceOf[BSONDocument]))
}