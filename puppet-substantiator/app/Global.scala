
import akka.actor.{Props, ActorRef}
import play.api._

object Global extends GlobalSettings {
  override def onStop(app: Application) {
    super.onStop(app)
  }

  override def onStart(app: Application) {
    super.onStart(app)
  }
}