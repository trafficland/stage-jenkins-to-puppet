package actors

import akka.actor.Actor
import sys.process._
import org.slf4j.Logger

object ScriptExecutorActor {

  case class Script(fileName: String, args: Seq[String])

}

case class ScriptExecutorActor(toConsole: Boolean = false, rethrow: Boolean = false, logger: Option[Logger] = None) extends Actor {

  import ScriptExecutorActor._

  def receive = {
    case Script(fileName, args) =>
      val script = fileName.head match {
        case '.' =>
          fileName.replaceFirst(".", new java.io.File(".").getCanonicalPath)
        case '~' =>
          fileName.replaceFirst(".", System.getProperty("user.home"))
        case _ =>
          fileName
      }
      handleProcessBuilder((Seq(script) ++ args).cat)
  }

  def handleProcessBuilder(toRun: ProcessBuilder) {
    try {
      logger match {
        case Some(logger) =>
          if (logger.isDebugEnabled)
            logger.debug(toRun !!)
          else {
            if (toConsole)
              Console.println(toRun !!)
            else
              toRun !
          }
        case None =>
          if (toConsole)
            Console.println(toRun !!)
          else
            toRun !
      }
    }
    catch {
      case ex: Exception =>
        logger match {
          case Some(logger) =>
            logger.error("Script Execution Error! With Exception: ", ex)
          case None =>
            if (toConsole)
              Console.println(ex.getMessage)
        }
        if (rethrow)
          throw ex

    }
  }
}
