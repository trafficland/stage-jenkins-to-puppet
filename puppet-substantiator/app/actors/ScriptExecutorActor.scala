package actors

import akka.actor.Actor
import sys.process._
import org.slf4j.Logger

object ScriptExecutorActor {

  case class Script(fileName: String, args: Seq[String])

  case class ScriptFile(f: java.io.File, args: Seq[String])

  case class ScriptURL(uri:java.net.URL, args: Seq[String])

}

case class ScriptExecutorActor(toConsole: Boolean = false, rethrow: Boolean = false, logger: Option[Logger] = None) extends Actor {

  import ScriptExecutorActor._

  def receive = {
    case Script(scriptString, args) =>
      handleProcessBuilder((Seq(scriptString) ++ args).cat)

    case ScriptFile(file, args) =>
      val pipedCommand = Seq("echo") ++ args
      handleProcessBuilder(file.cat #| pipedCommand.cat)
    case ScriptURL(url, args) =>
      val pipedCommand = Seq("echo") ++ args
      handleProcessBuilder(url.cat #| pipedCommand.cat)
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
