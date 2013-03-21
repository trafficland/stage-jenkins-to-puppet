package services.repository

import play.api.libs.iteratee.Enumerator
import concurrent.{ExecutionContext, Future}
import models.IModel

trait IRepository[ID, TModel <: IModel[ID], Q] {
  def create(entity: TModel)(implicit context: ExecutionContext): Future[Either[Option[TModel], Exception]]

  def update(entity: TModel, doUpsert: Boolean = false)(implicit context: ExecutionContext): Future[Either[Option[TModel], Exception]]

  def save(entity: TModel)(implicit context: ExecutionContext): Future[Either[Option[TModel], Exception]] = {
    for {
      toSave <- existsTrip(entity)
      saved <- update(toSave, true)
    } yield (saved)
  }

  protected def existsTrip(entity: TModel)(implicit context: ExecutionContext): Future[TModel]

  def remove(id: ID)(implicit context: ExecutionContext): Future[Boolean]

  def get(id: ID)(implicit context: ExecutionContext): Future[Either[Option[TModel], Exception]]

  def getAll(implicit context: ExecutionContext): Enumerator[TModel]

  def search(criteria: ISearchCriteria[Q])(implicit context: ExecutionContext): Future[ISearchResults[TModel]]

  def getByName(name: String)(implicit context: ExecutionContext): Future[Option[TModel]]

  def removeAll()(implicit context: ExecutionContext): Future[Boolean]
}

trait IRepositoryProvider[ID, TModel <: IModel[ID], Q] {
  def repository: IRepository[ID, TModel, Q]
}