package actors

import akka.actor.{Props, Actor}
import sys.process._
import org.slf4j.Logger

/*
Likley to be a IO actor to execute scripts
Scripts to be stored on File system or mongo db? Only a read operation so FS could be ok
 */
object ScriptExecutorActor {

  case class Script(fileName: String, args: Seq[String])

}

case class ScriptExecutorActor(toConsole: Boolean = false, rethrow: Boolean = false, logger: Option[Logger] = None) extends Actor {

  import ScriptExecutorActor._

  def receive = {
    case Script(scriptString, args) =>
      //execute!
      try {
        val toRun = Seq(scriptString) ++ args
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
