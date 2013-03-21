package services.repository

trait IUniqueCheck[Id, Query] {
  def id: Option[Id]

  def otherCriteria: Query

}
