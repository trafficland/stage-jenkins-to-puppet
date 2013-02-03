package services.repository

import play.api.libs.iteratee.Enumerator
import concurrent.{ExecutionContext, Future}
import models.IModel

trait IRepository[ID,TModel <: IModel[ID],Q ] {
  def create(entity: TModel)(implicit context: ExecutionContext): Future[Option[TModel]]
  def update(entity: TModel)(implicit context: ExecutionContext): Future[Option[TModel]]
  def remove(id: ID)(implicit context: ExecutionContext): Future[Boolean]
  def get(id: ID)(implicit context: ExecutionContext): Future[Option[TModel]]
  def getAll(implicit context: ExecutionContext): Enumerator[TModel]
  def search(criteria: ISearchCriteria[Q])(implicit context: ExecutionContext): Future[ISearchResults[TModel]]
}

trait IRepositoryProvider[ID,TModel <: IModel[ID],Q] {
  def repository: IRepository[ID,TModel,Q]
}