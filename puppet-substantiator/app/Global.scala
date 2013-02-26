
import globals.playframework.ActorsProvider
import ActorsProvider._
import play.api._
import util.actors.{ScriptExecutorActor, fsm}

object Global extends GlobalSettings {
  override def onStop(app: Application) {
    super.onStop(app)
  }

  override def onStart(app: Application) {
    if (app.mode != Mode.Test) {
      actors().createActors()
    }
    super.onStart(app)
  }
}