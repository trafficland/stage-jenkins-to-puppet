package services.repository.mongo.reactive

import reactivemongo.bson.{BSONString, BSONDocument, BSONObjectID}
import play.api.libs.iteratee._
import concurrent._
import services.repository._
import models.IModel
import models.mongo.reactive.IMongoModel
import reactivemongo.bson.BSONString
import scala.Some

trait IMongoRepository[MModel <: IMongoModel[MModel]]
  extends IRepository[BSONObjectID, MModel, BSONDocument] {

  val modelIteratee = Iteratee.fold[MModel, List[MModel]](List.empty[MModel]) {
    (a, b) => b :: a
  }

  def create(entity: MModel)(implicit context: ExecutionContext): Future[Either[Option[MModel], Exception]]

  def update(entity: MModel, doUpsert: Boolean = false)(implicit context: ExecutionContext): Future[Either[Option[MModel], Exception]]

  def remove(id: BSONObjectID)(implicit context: ExecutionContext): Future[Boolean]

  def get(id: BSONObjectID)(implicit context: ExecutionContext): Future[Either[Option[MModel], Exception]]

  def getAll(implicit context: ExecutionContext): Enumerator[MModel]

  def search(criteria: ISearchCriteria[BSONDocument])(implicit context: ExecutionContext): Future[ISearchResults[MModel]]

  def searchSingle(criteria: ISearchCriteria[BSONDocument])(implicit context: ExecutionContext): Future[Option[MModel]]

  def removeAll()(implicit context: ExecutionContext): Future[Boolean]

  override def getByName(name: String)(implicit context: ExecutionContext): Future[Option[MModel]] = {
    searchSingle(MongoSearchCriteria(BSONDocument("name" -> BSONString(name))))
  }

  protected def existsTrip(entity: MModel)(implicit context: ExecutionContext): Future[MModel] = {
    entity.id match {
      case Some(id) => future(entity)
      case None =>
        for {
          toSave <- getByName(entity.name).map(_ match {
            case Some(found) =>
              entity.id = found.id
              entity
            case None =>
              entity.id = Some(BSONObjectID.generate)
              entity
          })
        } yield (toSave)
    }
  }
}

trait IMongoRepositoryProvider[MModel <: IModel[BSONObjectID]]
  extends IRepositoryProvider[BSONObjectID, MModel, BSONDocument] {
  def repository: IRepository[BSONObjectID, MModel, BSONDocument]
}