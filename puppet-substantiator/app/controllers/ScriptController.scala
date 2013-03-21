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
      "~/stage/roll_back_remote.sh")
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

  def rollBack = {
    Action(parse.json) {
      request =>
        request.body.asOpt[models.mongo.reactive.App] match {
          case Some(app) =>
            optScriptExists match {
              case Some(script) =>
                /*
                RollBackScript Required Args
                  applicationName=${1?missing application name}
                  stagePath=${2?missing stage path}
                  extension=${3?missing extension}
                  destinationAddress=${4?missing destination address}
                  extractCmd=${5?missing extraction command like "unzip"}
                  renameApplicationTo=${6:-$applicationName}
                  startName=${7:-}
                */
                val rename = app.renameAppTo.getOrElse(app.name)
                val rollBackLogMsg = "------------------------ROLLBACK OCCURRING FOR APP: %s------------------------".format(rename)
                play.api.Logger.logger.info(rollBackLogMsg)
                Console.println(rollBackLogMsg)
                actors().getActor(scriptorName) ! new Script(script,
                  Seq(app.name, puppetServerStageHome + rename + "/", extension, puppetHostNameOrAddress, extractCommand, rename))
                //TODO: Excute script to send an email out to whomever to notify rollback
                //echo ""$PROG" on (`uname -n`) failed deployment. Rollback was completed  @ "$NOW"" | mail -s "$PROG (`uname -n`) deployment failed" -c REPLACE_WITH_SPACED_EMAILS
                Ok("Executed Rollback script!")
              case None =>
                InternalServerError("Script not Found!")
            }
          case None =>
            BadRequest("Json for App / Application not defined!")
        }

    }
  }
}

object ScriptController extends ScriptController
