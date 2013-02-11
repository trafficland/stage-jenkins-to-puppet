package models

import play.api.libs.json._
import reactivemongo.bson.handlers.{BSONWriter, BSONReader}

trait IReadersWriters[T] extends IReaders[T] with IWriters[T]

trait IWriters[T] {
  implicit val jsonWriter: Writes[T]
  implicit val bsonWriter: BSONWriter[T]
}

trait IReaders[T] {
  implicit val jsonReader: Reads[T]
  implicit val bsonReader: BSONReader[T]
}