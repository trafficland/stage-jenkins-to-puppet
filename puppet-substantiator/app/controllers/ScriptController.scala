package controllers

import play.api.mvc._
import java.io.File
import scala.io._
import actors.ScriptExecutorActor
import ScriptExecutorActor._
import _root_.util.OptionHelper.getOptionOrDefault
import actors.context.playframework.ActorContextProvider
import ActorContextProvider._
import util.PlaySettings
import PlaySettings._
import java.net.URL
import play.api.test.Helpers._
import play.api.test.FakeRequest

/*
End point to call IO Actor to run scripts
 */
abstract class ScriptController extends Controller {
  implicit val app = play.api.Play.current

  lazy val scriptPathAndName =
    getOptionOrDefault(play.api.Play.configuration.getString("script.file.location.rollback"),
      "/assets/scripts/rollback_remote.sh")

  lazy val urlToScript = "http://" + absUrl + scriptPathAndName

  lazy val optScriptExists = {
    route(FakeRequest(GET, scriptPathAndName)) match {
      case Some(result) =>
        val stat = status(result)
        stat match {
          case OK =>
            Some(scriptPathAndName)
          case _ =>
            None
        }
      case None =>
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
        case Some(workingLocation) =>
          """
              RollBackScript Required Args
            |applicationName=${1?missing application name}
            |stagePath=${2?missing stage path}
            |extension=${3?missing extension}
            |destinationAddress=${4?missing destination address}
            |extractCmd=${5?missing extraction command like "unzip"}
            |applicationPortNumber=${6?missing port number for application hosting}
          """
          actors().getActor(scriptorName) ! new ScriptURL(new URL(urlToScript),
            Seq(appName, puppetServerStageHome + appName + "/", extension, puppetHostNameOrAddress, extractCommand, appPortNumber.toString))
          Ok("Executed Rollback script!")
        case None =>
          InternalServerError("Script not Found!")
      }
    }
  }
}

object ScriptController extends ScriptController
