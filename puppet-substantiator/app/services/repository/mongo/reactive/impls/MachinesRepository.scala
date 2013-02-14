package services.repository.mongo.reactive.impls

import models.mongo.reactive._
import util.ConfigurationProvider
import services.repository.mongo.reactive._
import concurrent._
import services.repository.{ISearchResults, Paging}
import reactivemongo.bson.{BSONArray, BSONString, BSONDocument}

trait IMachinesRepository extends MongoBaseRepository[Machine] with IMongoUniqueCheckRepository[Machine] {
  def getByName(name: String)(implicit context: ExecutionContext): Future[ISearchResults[Machine]]

  def machineExists(name: String)(implicit context: ExecutionContext): Future[Boolean]

  def machinesExistByNames(names: List[String])(implicit context: ExecutionContext): Future[Map[String, Boolean]]
}

abstract class MachinesRepository
  extends IMachinesRepository {

  override protected def collectionName = "machines"

  implicit val reader = Machine.MachineBSONReader
  implicit val writer = Machine.MachineBSONWriter

  def getByName(name: String)(implicit context: ExecutionContext): Future[ISearchResults[Machine]] = {
    search(MongoSearchCriteria(BSONDocument("name" -> BSONString(name)), None, Some(Paging(0, 10))))
  }

  def machineExists(name: String)(implicit context: ExecutionContext): Future[Boolean] = {
    for {
      results <- getByName(name)
    } yield results.count > 0
    }

  def machinesExistByNames(names: List[String])
                          (implicit context: ExecutionContext): Future[Map[String, Boolean]] = {
    val multipleNameCriteria = MongoSearchCriteria(BSONDocument("name" -> BSONDocument(
      "$in" -> BSONArray(names.map(s => new BSONString(s)): _*)
    )), None, None)

    val futResults = search(multipleNameCriteria)
      for {
      results <- futResults
      machineSeq <- results.results.run[List[Machine]](modelIteratee).map(m => m)
    } yield (names.map(name => (name, machineSeq.exists(m => m.name == name))).toMap)
  }

  def getAllMem()(implicit context: ExecutionContext) ={
    val enum = getAll
    for {
      machineSeq <- enum.run[List[Machine]](modelIteratee).map(m => m)
    } yield (machineSeq)
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