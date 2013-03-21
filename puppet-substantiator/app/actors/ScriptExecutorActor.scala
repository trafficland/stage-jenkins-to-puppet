package actors

import akka.actor.Actor
import sys.process._
import org.slf4j.Logger
import util.{LogAndConsole, TidyConsole}

object ScriptExecutorActor {

  case class ScriptProcess(toRun: ProcessBuilder)

  case class ScriptStringSeq(args: String*)

  case class ScriptFile(fileName: String, args: Seq[String])

}

case class ScriptExecutorActor(toConsole: Boolean = false, rethrow: Boolean = false, logger: Option[Logger] = None) extends Actor {

  import ScriptExecutorActor._

  implicit val ourLogger = logger

  def receive = {
    case ScriptFile(fileName, args) =>
      val script = fileName.head match {
        case '.' =>
          fileName.replaceFirst(".", new java.io.File(".").getCanonicalPath)
        case '~' =>
          fileName.replaceFirst(".", System.getProperty("user.home"))
        case _ =>
          fileName
      }
      handleProcessBuilder((Seq(script) ++ args).cat)
    case ScriptStringSeq(args) =>
      handleProcessBuilder(args.cat)
    case ScriptProcess(proc) =>
      handleProcessBuilder(proc)
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
            LogAndConsole.error("ScriptFile Execution Error! With Exception: ", Some(ex))(logger)
          case None =>
            if (toConsole)
              Console.println(ex.getMessage)
        }
        if (rethrow)
          throw ex

    }
  }
}
