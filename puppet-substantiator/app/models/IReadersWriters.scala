package models

import json.{IReadsExtended, IWritesExtended}
import mongo.reactive.{IBSONWriterExtended, IBSONReaderExtended}

trait IReadersWriters[T] extends IReaders[T] with IWriters[T]

trait IWriters[T] {
  implicit val jsonWriter: IWritesExtended[T]
  implicit val bsonWriter: IBSONWriterExtended[T]
}

trait IReaders[T] {
  implicit val jsonReader: IReadsExtended[T]
  implicit val bsonReader: IBSONReaderExtended[T]
}