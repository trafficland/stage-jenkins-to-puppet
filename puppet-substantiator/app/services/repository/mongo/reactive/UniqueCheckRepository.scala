package services.repository.mongo.reactive

import reactivemongo.core.commands.Count
import concurrent.{Future, ExecutionContext}
import models.mongo.reactive.BaseMongoModel

import reactivemongo.bson.{BSONObjectID, BSONDocument}
import services.repository.IUniqueCheckRepository

trait MongoUniqueCheckRepository[TModel <: BaseMongoModel]
  extends IUniqueCheckRepository[BSONObjectID,BaseMongoModel,MongoSearchCriteria] {
  this: MongoBaseRepository[TModel] =>

  def uniqueCheck(criteria: MongoUniqueCheck)
                 (implicit context: ExecutionContext): Future[Boolean] = {

    var doc = criteria.otherCriteria.toAppendable
    criteria.id foreach {
      id => doc = doc append ("_id" -> BSONDocument("$ne" -> id))
    }

    db.command(Count(collectionName, Some(doc))).map(_ == 0)
  }
}
