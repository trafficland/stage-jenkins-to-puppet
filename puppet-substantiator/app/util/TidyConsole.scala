package util

object TidyConsole {
  def println(any: Any) {
    Console.println()
    Console.println(any)
  }
}

object LogAndConsole {

  import TidyConsole._

  def debug(log: String, opt: Option[Any] = None)(implicit logger: org.slf4j.Logger) =
    opt match {
      case Some(ref) =>
        logger.debug(log, ref)
        println(log)
      case None =>
        logger.debug(log)
        println(log)
    }

  def info(log: String, opt: Option[Any] = None)(implicit logger: org.slf4j.Logger) =
    opt match {
      case Some(ref) =>
        logger.info(log, ref)
        println(log)
      case None =>
        logger.info(log)
        println(log)
    }

  def error(log: String, opt: Option[Any] = None)(implicit logger: org.slf4j.Logger) =
    opt match {
      case Some(ref) =>
        logger.error(log, ref)
        println(log)
      case None =>
        logger.error(log)
        println(log)
    }
}
