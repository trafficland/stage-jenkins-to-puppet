package services.repository

case class Sorting(field: String, direction: Int)

case class Paging(skip: Int, limit: Int)

trait ISearchCriteria[Q] {
  def query: Q

  def sort: Option[Sorting]

  def page: Option[Paging]
}
