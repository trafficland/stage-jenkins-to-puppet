package actors

import akka.actor.Actor
import sys.process._
import org.slf4j.Logger
import util.{LogAndConsole, TidyConsole}

object ScriptExecutorActor {

  case class Script(fileName: String, args: Seq[String])

}

case class ScriptExecutorActor(toConsole: Boolean = false, rethrow: Boolean = false, logger: Option[Logger] = None) extends Actor {

  import ScriptExecutorActor._

  implicit val ourLogger = logger

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
          if (toConsole)
            LogAndConsole.debug(toRun !!)(logger)
          else
            toRun !
        case None =>
          if (toConsole)
            TidyConsole.println(toRun !!)
          else
            toRun !
      }
    }
    catch {
      case ex: Exception =>
        logger match {
          case Some(logger) =>
            LogAndConsole.error("Script Execution Error! With Exception: ", Some(ex))(logger)
          case None =>
            if (toConsole)
              Console.println(ex.getMessage)
        }
        if (rethrow)
          throw ex

    }
  }
}
