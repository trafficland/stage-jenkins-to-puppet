package models

trait IModel[T] {
  def id: Option[T]
}
