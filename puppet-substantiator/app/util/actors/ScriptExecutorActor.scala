package services.actors

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

class ScriptExecutorActor(logger: Logger) extends Actor {

  import ScriptExecutorActor._


  def receive = {
    case Script(scriptString, args) =>
      //execute!
      try {
        val toRun = Seq(scriptString) ++ args
        toRun !
      }
      catch {
        case ex: Exception =>
          logger.error("Script Execution Error! With Exception: ", ex)

      }
  }
}
