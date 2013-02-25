
import globals.playframework.ActorsProvider
import ActorsProvider._
import play.api._
import util.actors.{ScriptExecutorActor, fsm}

object Global extends GlobalSettings {
  override def onStop(app: Application) {
    super.onStop(app)
  }

  override def onStart(app: Application) {
    import fsm.CancellableMapFSMDomainProvider.domain._
    if (app.mode != Mode.Test) {
      actors().createActors()
      actors().getActor(scheduleName) ! SetTarget(None) // initialize scheduler
    }
    super.onStart(app)
  }
}