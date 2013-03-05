package controllers

import play.api.mvc._
import scala.io._
import actors.ScriptExecutorActor
import ScriptExecutorActor._
import _root_.util.OptionHelper.getOptionOrDefault
import actors.context.playframework.ActorContextProvider
import ActorContextProvider._

abstract class ScriptController extends Controller {
  implicit val app = play.api.Play.current

  lazy val scriptPathAndName =
    getOptionOrDefault(play.api.Play.configuration.getString("script.file.location.rollback"),
      "~/stage/rollback_remote.sh")
      .replace("~", System.getProperty("user.home"))

  lazy val optScriptExists = {
    try {
      val s = Source.fromFile(scriptPathAndName)
      s.getLines()
      Some(scriptPathAndName)
    }
    catch {
      case ex: Exception =>
        None
    }
  }

  val puppetServerStageHome = getOptionOrDefault(play.api.Play.configuration.getString("puppet.stage.home"), "~/stage/")
  val extension = getOptionOrDefault(play.api.Play.configuration.getString("puppet.stage.extension"), ".zip")
  val puppetHostNameOrAddress = getOptionOrDefault(play.api.Play.configuration.getString("puppet.hostName"), "127.0.0.1")
  val extractCommand = getOptionOrDefault(play.api.Play.configuration.getString("puppet.stage.extractCommand"), "unzip")

  def rollBack(appName: String, appPortNumber: Int) = {
    Action {
      optScriptExists match {
        case Some(script) =>
          """
              RollBackScript Required Args
            |applicationName=${1?missing application name}
            |stagePath=${2?missing stage path}
            |extension=${3?missing extension}
            |destinationAddress=${4?missing destination address}
            |extractCmd=${5?missing extraction command like "unzip"}
            |applicationPortNumber=${6?missing port number for application hosting}
          """
          actors().getActor(scriptorName) ! new Script(script,
            Seq(appName, puppetServerStageHome + appName + "/", extension, puppetHostNameOrAddress, extractCommand, appPortNumber.toString))
          Ok("Executed Rollback script!")
        case None =>
          InternalServerError("Script not Found!")
      }
    }
  }
}

object ScriptController extends ScriptController
