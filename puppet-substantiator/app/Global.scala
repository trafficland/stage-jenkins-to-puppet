
import actors.context.playframework.ActorContextProvider
import ActorContextProvider._
import play.api._
import util.OptionHelper._

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

