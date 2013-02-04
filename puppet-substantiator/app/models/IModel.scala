package models

trait IModel[T] {
  def id: Option[T]
}

object Model{
  val bsonError: String = "From %s property %s!"

  def errorFrom(from: String, propertyError: String): Exception = {
    new Exception(bsonError.format(from, propertyError))
  }
}
