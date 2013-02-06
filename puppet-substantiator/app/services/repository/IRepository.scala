package services.repository

import play.api.libs.iteratee.Enumerator
import concurrent.{ExecutionContext, Future}
import models.IModel

trait IRepository[ID,TModel <: IModel[ID],Q ] {
  def create(entity: TModel)(implicit context: ExecutionContext): Future[Either[Option[TModel],Exception]]
  def update(entity: TModel)(implicit context: ExecutionContext): Future[Either[Option[TModel],Exception]]
  def remove(id: ID)(implicit context: ExecutionContext): Future[Boolean]
  def get(id: ID)(implicit context: ExecutionContext): Future[Either[Option[TModel],Exception]]
  def getAll(implicit context: ExecutionContext): Either[Enumerator[TModel],Exception]
  def search(criteria: ISearchCriteria[Q])(implicit context: ExecutionContext): Future[Either[ISearchResults[TModel],Exception]]
}

trait IRepositoryProvider[ID,TModel <: IModel[ID],Q] {
  def repository: IRepository[ID,TModel,Q]
}