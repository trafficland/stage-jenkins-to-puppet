package controllers

import play.api.mvc._
import java.io.File
import scala.io._
import actors.ScriptExecutorActor
import ScriptExecutorActor._
import _root_.util.OptionHelper.getOptionOrDefault
import actors.context.playframework.ActorContextProvider
import ActorContextProvider._

/*
End point to call IO Actor to run scripts
 */
abstract class ScriptController extends Controller {

  lazy val optScriptFileName = {
    try {
      Some(getOptionOrDefault(play.api.Play.configuration.getString("script.file.location.rollback"),
        "./private/scripts/rollback.sh"))
    }
    catch {
      case ex: Exception =>
        None
    }
  }

  val puppetServerStageHome = getOptionOrDefault(play.api.Play.configuration.getString("puppet.stage.home"), "~/stage")
  val extension = getOptionOrDefault(play.api.Play.configuration.getString("puppet.stage.extension"), "~/.zip")
  val puppetHostNameOrAddress = getOptionOrDefault(play.api.Play.configuration.getString("puppet.hostName"), "127.0.0.1")
  val extractCommand = getOptionOrDefault(play.api.Play.configuration.getString("puppet.stage.extractCommand"), "unzip")

  def rollBack(appName: String, appPortNumber: Int) = {
    Action {
      optScriptFileName match {
        case Some(scriptPathAndName) =>
          val someFile = try {
            val s = Source.fromFile(scriptPathAndName)
            s.getLines()
            Some(scriptPathAndName)
          }
          catch {
            case ex: Exception =>
              None
          }
          someFile match {
            case Some(file) =>
              """
                RollBackScript Required Args
                applicationName=${1?missing application name}
                |stagePath=${2?missing stage path}
                |extension=${3?missing extension}
                |destinationAddress=${4?missing destination address}
                |extractCmd=${5?missing extraction command like "unzip"}
                |applicationPortNumber=${6?missing port number for application hosting}
              """
              actors().getActor(scriptorName) ! new Script(scriptPathAndName.replaceFirst(".", new File(".").getCanonicalPath()),
                Seq(appName, puppetServerStageHome + "/" + appName, extension, puppetHostNameOrAddress, extractCommand, appPortNumber.toString))
              Ok("Execute Rollback script here!")
            case None =>
              InternalServerError("Script not Found!")
          }
        case None =>
          InternalServerError("No script defined to look up!")
      }
    }
  }
}

object ScriptController extends ScriptController
