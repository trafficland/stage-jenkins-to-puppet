package models.mongo.reactive

import play.api.libs.json._
import reactivemongo.bson._
import reactivemongo.bson.handlers._
import models.Model._
import models.json.{IWritesExtended, IReadsExtended}

/*
Hybrid Mongo Schema of Embedded to Normalized , normalizing machines for deal with updating independent
machine state - ie machine isAlive.

If machine is dead or has undesired state and application will remove that machine (AppMachineState) from its cluster
 */
case class AppMachineState(val machineName: String, val actual: String = "EMPTY")

object AppMachineState {

  implicit object AppMachineBSONReader extends BSONReader[AppMachineState] with IBSONReaderExtended[AppMachineState]   {
    override def fromBSON(document: BSONDocument) = {
      val doc = document.toTraversable

      AppMachineState(
        doc.getAs[BSONString]("machineName").map(_.value).getOrElse(throw errorFrom("BSONRead", "machineName")),
        doc.getAs[BSONString]("actual").map(_.value).getOrElse(throw errorFrom("BSONRead", "key"))
      )
    }
  }

  implicit object AppMachineStateBSONWriter extends IBSONWriterExtended[AppMachineState] {
    override def toBSON(entity: AppMachineState) =
      BSONDocument(
        "machineName" -> BSONString(entity.machineName),
        "actual" -> BSONString(entity.actual)
      )
  }

  implicit object AppMachineJSONReader extends IReadsExtended[AppMachineState] {
    def reads(json: JsValue) =
      JsSuccess(new AppMachineState(
        (json \ "machineName").as[String],
        (json \ "actual").as[String]
      ))
  }

  implicit object AppMachineJSONWriter extends IWritesExtended[AppMachineState] {
    def writes(entity: AppMachineState): JsValue =
      JsObject(Seq(
        "machineName" -> JsString(entity.machineName),
        "actual" -> JsString(entity.actual)
      ))
  }

}
