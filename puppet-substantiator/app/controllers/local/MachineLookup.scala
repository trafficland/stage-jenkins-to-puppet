package controllers.local

import services.repository.mongo.reactive.impls.{IMachinesRepository, IMachinesRepositoryProvider}
import models.mongo.reactive._
import services.repository.mongo.reactive.MongoSearchCriteria
import reactivemongo.bson.{BSONString, BSONDocument}
import services.repository.{MongoSearchResults, Paging}
import scala.concurrent._

case class MachineLookup(val repository: IMachinesRepository) {

  private val repo = repository

  def getByName(name: String)(implicit context: ExecutionContext): Future[MongoSearchResults[Machine]] = {
      repo.search(MongoSearchCriteria(BSONDocument("name" -> BSONString(name)), None, Some(Paging(0, 10))))
  }

  def machineExists(name: String)(implicit context: ExecutionContext): Future[Boolean] = {
    for {
      results <- getByName(name)
    } yield results.count > 0
  }
}
