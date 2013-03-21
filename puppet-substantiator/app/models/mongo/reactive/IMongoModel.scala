package models.mongo.reactive

import reactivemongo.bson._
import models.IModel

trait IMongoModel[ThisModel] extends IModel[BSONObjectID] {
  def isValid: Boolean = true

  var name: String

  def isEqualTo(other: IMongoModel[ThisModel], useID: Boolean): Boolean = {
    if (useID) {
      (id, other.id) match {
        case (Some(id), Some(otherId)) => id == otherId
        case (None, None) => true
        case _ => false
      }
    }
    else
      true
  }

}
