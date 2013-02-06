package services.repository.mongo.reactive.impls

import models.mongo.reactive._
import util.ConfigurationProvider
import services.repository.mongo.reactive._
import concurrent._
import services.repository.{ISearchResults, Paging, MongoSearchResults}
import reactivemongo.bson.{BSONArray, BSONString, BSONDocument}
import play.api.libs.iteratee.Iteratee

trait IMachinesRepository extends MongoBaseRepository[Machine] with IMongoUniqueCheckRepository[Machine] {
  val machineIteratee = Iteratee.fold[Machine, List[Machine]](List.empty[Machine]) {
    (a, b) => b :: a
  }

  def getByName(name: String)(implicit context: ExecutionContext): Future[Either[ISearchResults[Machine], Exception]]

  def machineExists(name: String)(implicit context: ExecutionContext): Future[Boolean]

  def machinesExistByNames(names: List[String])(implicit context: ExecutionContext): Future[Either[Map[String, Boolean], Exception]]
}

abstract class MachinesRepository
  extends IMachinesRepository {

  override protected def collectionName = "machines"

  implicit val reader = Machine.MachineBSONReader
  implicit val writer = Machine.MachineBSONWriter

  def getByName(name: String)
               (implicit context: ExecutionContext): Future[Either[ISearchResults[Machine], Exception]] = {
    search(MongoSearchCriteria(BSONDocument("name" -> BSONString(name)), None, Some(Paging(0, 10))))
  }

  def machineExists(name: String)(implicit context: ExecutionContext): Future[Boolean] = {
    for {
      eitherResults <- getByName(name)
    } yield {
      eitherResults match {
        case Left(results) =>
          results.count > 0
        case Right(ex) => false
      }
    }
  }

  def machinesExistByNames(names: List[String])
                          (implicit context: ExecutionContext): Future[Either[Map[String, Boolean], Exception]] = {
    val multipleNameCriteria = MongoSearchCriteria(BSONDocument("name" -> BSONDocument(
      "$in" -> BSONArray(names.map(s => new BSONString(s)): _*)
    )), None, None)

    val futResults = search(multipleNameCriteria)
    try {
      for {
        eitherResults <- futResults
        machineSeq <- {
          eitherResults match {
            case Left(searchRes) =>
              searchRes.results.run[List[Machine]](machineIteratee).map(m => m)
            case Right(ex) =>
              throw ex
          }
        }
      } yield {
        val res = names.map(name => (name, machineSeq.exists(m => m.name == name))).toMap
        Left(res)
      }
    }
    catch {
      case e: Exception =>
        future(Right(e))
    }
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