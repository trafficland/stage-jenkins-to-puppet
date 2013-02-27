package models.mongo.reactive

import reactivemongo.bson.BSONObjectID
import spray.json._

object MongoSprayFormats {

  implicit object BSONObjectIDFormat extends RootJsonFormat[BSONObjectID] {
    def write(b: BSONObjectID) = JsObject(
      "id" -> JsString(b.stringify)
    )

    def read(value: JsValue) = {
      value.asJsObject.getFields("id") match {
        case Seq(JsString(id)) =>
          new BSONObjectID(id)
        case _ => throw new DeserializationException("BSONOBjectID expected")
      }
    }
  }

  implicit object ActoStateFormat extends RootJsonFormat[ActorState] with DefaultJsonProtocol {

    case class actState(name: String, isAlive: Boolean, state: String, id: Option[String])

    val easyFormat = jsonFormat4(actState)

    def write(ent: ActorState) = {
      easyFormat.write(actState(ent.name, ent.isAlive, ent.state, ent.id.map(_.stringify)))
    }

    def read(value: JsValue) = {
      val actState = easyFormat.read(value)
      ActorState(name = actState.name, isAlive = actState.isAlive,
        state = actState.state, id = actState.id.map(BSONObjectID(_)))
    }
  }


}
