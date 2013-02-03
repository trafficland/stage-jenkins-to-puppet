package models.mongo.reactive

import reactivemongo.bson._
import models.IModel

abstract class BaseMongoModel(var id: Option[BSONObjectID]) extends IModel[BSONObjectID]