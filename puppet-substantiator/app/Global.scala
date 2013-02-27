
import globals.playframework.ActorsProvider
import ActorsProvider._
import play.api._

object Global extends GlobalSettings {
  override def onStop(app: Application) {
    super.onStop(app)
  }

  override def onStart(app: Application) {
    super.onStart(app)
    if (app.mode != Mode.Test) {
      actors().createActors()
    }
  }
}