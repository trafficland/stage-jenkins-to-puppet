package services.repository.mongo.reactive

import reactivemongo.bson.{BSONObjectID, BSONDocument}
import services.repository.IUniqueCheck

case class MongoUniqueCheck
(override val id: Option[BSONObjectID],
 override val otherCriteria: BSONDocument) extends IUniqueCheck[BSONObjectID, BSONDocument]
