package services.repository.mongo.reactive

import reactivemongo.bson.BSONDocument
import services.repository.{Paging, Sorting, ISearchCriteria}

case class MongoSearchCriteria(override val query: BSONDocument,
                               override val sort: Option[Sorting] = None,
                               override val page: Option[Paging] = None) extends ISearchCriteria[BSONDocument]
