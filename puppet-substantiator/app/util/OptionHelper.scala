package util

object OptionHelper {
  def getOptionOrDefault[T](option: Option[T], default: T): T = {
    option match {
      case Some(value) => value
      case None => default
    }
  }
}