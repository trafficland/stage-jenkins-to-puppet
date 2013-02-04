package models.mongo.reactive

import reactivemongo.bson._
import models.IModel

trait IMongoModel extends IModel[BSONObjectID]{
  def isValid:Boolean = true
}
