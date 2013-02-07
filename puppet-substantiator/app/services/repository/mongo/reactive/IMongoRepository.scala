package services.repository.mongo.reactive

import reactivemongo.bson.{BSONDocument, BSONObjectID}
import play.api.libs.iteratee.{Iteratee, Enumerator}
import concurrent.{ExecutionContext, Future}
import services.repository.{ISearchCriteria, IRepository, IRepositoryProvider, ISearchResults}
import models.IModel
import models.mongo.reactive.App
import scala.App

trait IMongoRepository[MModel <: IModel[BSONObjectID]]
  extends IRepository[BSONObjectID,MModel,BSONDocument] {

  val modelIteratee = Iteratee.fold[MModel, List[MModel]](List.empty[MModel]) {
    (a, b) => b :: a
  }
  def create(entity: MModel)(implicit context: ExecutionContext): Future[Either[Option[MModel],Exception]]
  def update(entity: MModel,doUpsert:Boolean = false)(implicit context: ExecutionContext): Future[Either[Option[MModel],Exception]]
  def remove(id: BSONObjectID)(implicit context: ExecutionContext): Future[Boolean]
  def get(id: BSONObjectID)(implicit context: ExecutionContext): Future[Either[Option[MModel],Exception]]
  def getAll(implicit context: ExecutionContext): Enumerator[MModel]
  def search(criteria: ISearchCriteria[BSONDocument])(implicit context: ExecutionContext): Future[ISearchResults[MModel]]
}

trait IMongoRepositoryProvider[MModel <: IModel[BSONObjectID]]
  extends IRepositoryProvider[BSONObjectID,MModel,BSONDocument] {
  def repository: IRepository[BSONObjectID,MModel,BSONDocument]
}