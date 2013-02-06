package services.repository

import play.api.libs.iteratee.Enumerator
import concurrent.{ExecutionContext, Future}
import models.IModel

trait IRepository[ID,TModel <: IModel[ID],Q ] {
  def create(entity: TModel)(implicit context: ExecutionContext): Future[Either[Option[TModel],Exception]]
  def update(entity: TModel,doUpsert:Boolean = false)(implicit context: ExecutionContext): Future[Either[Option[TModel],Exception]]
  def save(entity: TModel)(implicit context: ExecutionContext): Future[Either[Option[TModel],Exception]] = update(entity,true)
  def remove(id: ID)(implicit context: ExecutionContext): Future[Boolean]
  def get(id: ID)(implicit context: ExecutionContext): Future[Either[Option[TModel],Exception]]
  def getAll(implicit context: ExecutionContext): Enumerator[TModel]
  def search(criteria: ISearchCriteria[Q])(implicit context: ExecutionContext): Future[ISearchResults[TModel]]
}

trait IRepositoryProvider[ID,TModel <: IModel[ID],Q] {
  def repository: IRepository[ID,TModel,Q]
}