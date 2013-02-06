package controllers.local

import services.repository.mongo.reactive.impls.{IMachinesRepository, IMachinesRepositoryProvider}
import models.mongo.reactive._
import services.repository.mongo.reactive.MongoSearchCriteria
import reactivemongo.bson.{BSONArray, BSONString, BSONDocument}
import services.repository.{MongoSearchResults, Paging}
import scala.concurrent._
import play.api.libs.iteratee.{Iteratee, Enumerator}

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
  protected val machineIteratee = Iteratee.fold[Machine, List[Machine]](List.empty[Machine]) {
    (a, b) => b :: a
  }
  def machinesExistByNames(names:List[String])(implicit context: ExecutionContext):Future[Map[String,Boolean]] = {
    val multipleNameCriteria = MongoSearchCriteria(BSONDocument("name" -> BSONDocument(
      "$in" -> BSONArray(names.map( s => new BSONString(s)) :_*)
    )),None,None)

    val futResults = repo.search(multipleNameCriteria)
    for {
      results <- futResults
      machineSeq <- results.results.run[List[Machine]](machineIteratee).map(m=>m)
    }yield(names.map(name => (name, machineSeq.exists(m => m.name == name))).toMap)
  }

}
