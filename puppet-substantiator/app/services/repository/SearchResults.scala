package services.repository

import play.api.libs.iteratee.Enumerator

trait ISearchResults[TModel] {
  def count: Int

  def results: Enumerator[TModel]
}
