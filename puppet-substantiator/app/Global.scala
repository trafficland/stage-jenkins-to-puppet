
import actors.context.playframework.ActorContextProvider
import ActorContextProvider._
import controllers.ActorsStateController
import play.api._
import util.OptionHelper._

object Global extends GlobalSettings {
  override def onStop(app: Application) {
    ActorsStateController.clean
    super.onStop(app)
  }

  override def onStart(app: Application) {
    super.onStart(app)
    ActorsStateController.clean
    if (app.mode != Mode.Test) {
      actors().createActors()
    }
  }
}

