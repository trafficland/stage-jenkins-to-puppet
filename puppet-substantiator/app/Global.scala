
import globals.Actors
import play.api._
import util.actors.{ScriptExecutorActor, fsm}

object Global extends GlobalSettings {
  override def onStop(app: Application) {
    super.onStop(app)
  }

  override def onStart(app: Application) {
    import fsm.CancellableMapFSMDomainProvider.domain._
    if (app.mode != Mode.Test) {
      Actors.schedule ! SetTarget(None) // initialize scheduler
      Actors.scriptExecActorRef ! ScriptExecutorActor.SetLogger(Actors.ourlogger) // initialize scheduler
    }
    super.onStart(app)
  }
}