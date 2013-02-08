package controllers

import play.api._
import play.api.mvc._
import scala.io.Source.fromFile
import _root_.util.OptionHelper.getOptionOrDefault
import play.api.libs.concurrent.Akka
import play.api.Play.configuration
import play.api.Logger._
import akka.actor.Props
import services.actors.ScriptExecutorActor
import services.actors.ScriptExecutorActor._

/*
End point to call IO Actor to run scripts
 */
object ScriptController extends Controller {
  implicit val playApp = play.api.Play.current

  lazy val system = Akka.system
  lazy val scriptActorName = getOptionOrDefault(configuration.getString("actor.scriptExecutor"), "validationActorName")
  lazy val scriptExecActorRef = system.actorOf(Props(() => new ScriptExecutorActor(logger), scriptActorName))

  lazy val optScriptFileName = {
    try {
      Some(getOptionOrDefault(play.api.Play.configuration.getString("script.file.location.rollback"),
        "./private./scripts/rollback.sh"))
    }
    catch {
      case ex: Exception =>
        None
    }
  }

  def rollBack(appName: String) = {
    Action {
      optScriptFileName match {
        case Some(scriptPathAndName) =>
          scriptExecActorRef ! new Script(scriptPathAndName, Seq(appName))
          Ok("Execute Rollback script here!")
        case None =>
          InternalServerError("Unable to find a script to run!")
      }
    }
  }

}
