package models.json

import play.api.libs.json._

trait IReadsExtended[Model] extends Reads[Model] {
  def readsArray(array: JsArray): List[Model] = array.value.flatMap(reads(_).asOpt).toList
}

trait IWritesExtended[Model] extends Writes[Model]{
  def writesArray(objs: List[Model]): JsArray = JsArray(objs.map(writes))
}
