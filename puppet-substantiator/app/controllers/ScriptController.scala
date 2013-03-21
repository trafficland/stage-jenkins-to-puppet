package controllers

import play.api.mvc._
import scala.io._
import actors.ScriptExecutorActor
import ScriptExecutorActor._
import _root_.util.OptionHelper.getOptionOrDefault
import actors.context.playframework.ActorContextProvider
import ActorContextProvider._
import org.joda.time._

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

  val puppetServerStageHome = getOptionOrDefault(app.configuration.getString("puppet.stage.home"), "~/stage/")
  val extension = getOptionOrDefault(app.configuration.getString("puppet.stage.extension"), ".zip")
  val puppetHostNameOrAddress = getOptionOrDefault(app.configuration.getString("puppet.hostName"), "127.0.0.1")
  val extractCommand = getOptionOrDefault(app.configuration.getString("puppet.stage.extractCommand"), "unzip")
  val optEmailAddresses = app.configuration.getString("notify.commaDelimitedEmails")

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
                actors().getActor(scriptorName) ! new ScriptFile(script,
                  Seq(app.name, puppetServerStageHome + rename + "/", extension, puppetHostNameOrAddress, extractCommand, rename))
                optEmailAddresses match {
                  case Some(addresses) =>
                    sendEmail(addresses, "Puppet Substantiator Rollback for %s".
                      format(app.renameAppTo),
                      "%s application rollback was initiated at %s".format(app.renameAppTo, DateTime.now())
                    )
                    Ok("Executed Rollback script! Emails sent to: %s".format(addresses))
                  case None =>
                    Ok("Executed Rollback script!")
                }

              case None =>
                InternalServerError("ScriptFile not Found!")
            }
          case None =>
            BadRequest("Json for App / Application not defined!")
        }
    }
  }

  def sendEmail(commaDelimitedEmails: String, subject: String, body: String): String = {
    //TODO: Excute script to send an email out to whomever to notify rollback
    //echo body | mail -s subject -c commaDelimitedEmails
    val emailProc = {
      import sys.process._
      Seq("echo", body) #| Seq("mail", "-s %s".format(subject), commaDelimitedEmails)
    }
    actors().getActor(scriptorName) ! new ScriptProcess(emailProc)
    emailProc.toString
  }

  def emailEndpoint(commaDelimitedEmails: String, subject: String, body: String) = Action {
    Ok(sendEmail(commaDelimitedEmails, subject, body))
  }
}

object ScriptController extends ScriptController
