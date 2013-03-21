package services.repository

import concurrent.{Future, ExecutionContext}
import models.IModel

trait IUniqueCheckRepository[ID,TModel <: IModel[ID],Q] {

  def uniqueCheck(criteria: IUniqueCheck[ID,Q])
                 (implicit context: ExecutionContext): Future[Boolean]
}
