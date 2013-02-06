package services.repository.mongo.reactive

import reactivemongo.bson.{BSONDocument, BSONObjectID}
import play.api.libs.iteratee.Enumerator
import concurrent.{ExecutionContext, Future}
import services.repository.{ISearchCriteria, IRepository, IRepositoryProvider, ISearchResults}
import models.IModel

trait IMongoRepository[MModel <: IModel[BSONObjectID]]
  extends IRepository[BSONObjectID,MModel,BSONDocument] {
  def create(entity: MModel)(implicit context: ExecutionContext): Future[Either[Option[MModel],Exception]]
  def update(entity: MModel)(implicit context: ExecutionContext): Future[Either[Option[MModel],Exception]]
  def remove(id: BSONObjectID)(implicit context: ExecutionContext): Future[Boolean]
  def get(id: BSONObjectID)(implicit context: ExecutionContext): Future[Either[Option[MModel],Exception]]
  def getAll(implicit context: ExecutionContext): Either[Enumerator[MModel],Exception]
  def search(criteria: ISearchCriteria[BSONDocument])(implicit context: ExecutionContext): Future[Either[ISearchResults[MModel],Exception]]
}

trait IMongoRepositoryProvider[MModel <: IModel[BSONObjectID]]
  extends IRepositoryProvider[BSONObjectID,MModel,BSONDocument] {
  def repository: IRepository[BSONObjectID,MModel,BSONDocument]
}