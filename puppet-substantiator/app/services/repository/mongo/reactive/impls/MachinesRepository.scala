package services.repository.mongo.reactive.impls

import models.mongo.reactive._
import util.ConfigurationProvider
import services.repository.mongo.reactive._
import concurrent.{Future, ExecutionContext}
import services.repository.{Paging, MongoSearchResults}
import reactivemongo.bson.{BSONArray, BSONString, BSONDocument}
import play.api.libs.iteratee.Iteratee

trait IMachinesRepository extends MongoBaseRepository[Machine] with IMongoUniqueCheckRepository[Machine] {
  val machineIteratee = Iteratee.fold[Machine, List[Machine]](List.empty[Machine]) {
    (a, b) => b :: a
  }

  def getByName(name: String)(implicit context: ExecutionContext): Future[MongoSearchResults[Machine]]

  def machineExists(name: String)(implicit context: ExecutionContext): Future[Boolean]

  def machinesExistByNames(names: List[String])(implicit context: ExecutionContext): Future[Map[String, Boolean]]
}

abstract class MachinesRepository
  extends IMachinesRepository {

  override protected def collectionName = "machines"

  implicit val reader = Machine.MachineBSONReader
  implicit val writer = Machine.MachineBSONWriter

  def getByName(name: String)(implicit context: ExecutionContext): Future[MongoSearchResults[Machine]] = {
    search(MongoSearchCriteria(BSONDocument("name" -> BSONString(name)), None, Some(Paging(0, 10))))
  }

  def machineExists(name: String)(implicit context: ExecutionContext): Future[Boolean] = {
    for {
      results <- getByName(name)
    } yield results.count > 0
  }

  def machinesExistByNames(names: List[String])(implicit context: ExecutionContext): Future[Map[String, Boolean]] = {
    val multipleNameCriteria = MongoSearchCriteria(BSONDocument("name" -> BSONDocument(
      "$in" -> BSONArray(names.map(s => new BSONString(s)): _*)
    )), None, None)

    val futResults = search(multipleNameCriteria)
    for {
      results <- futResults
      machineSeq <- results.results.run[List[Machine]](machineIteratee).map(m => m)
    } yield (names.map(name => (name, machineSeq.exists(m => m.name == name))).toMap)
  }
}

trait IMachinesRepositoryProvider
  extends IMongoRepositoryProvider[Machine] {

  def repository: IMachinesRepository
}

trait MachineRepositoryProvider
  extends IMachinesRepositoryProvider {

  override val repository = MachinesRepository
}

object MachinesRepository
  extends MachinesRepository
  with IMongoDbProvider
  with ConfigurationProvider
  with MachineRepositoryProvider