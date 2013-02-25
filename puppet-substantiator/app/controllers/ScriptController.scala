package controllers

import play.api.mvc._
import java.io.File
import scala.io._
import _root_.util.actors.ScriptExecutorActor._
import _root_.util.OptionHelper.getOptionOrDefault
import globals.playframework.ActorsProvider
import ActorsProvider._

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

  def rollBack(appName: String) = {
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
              actors().getActor(scriptorName) ! new Script(scriptPathAndName.replaceFirst(".", new File(".").getCanonicalPath()), Seq(appName))
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
