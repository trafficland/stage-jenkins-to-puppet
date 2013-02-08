
import akka.actor.{Props, ActorRef}
import concurrent.ExecutionContext.Implicits._
import play.api._
import libs.concurrent.Akka
import scala.Some
import services.actors.ValidatorActor
import concurrent.ExecutionContext.Implicits.global

object Global extends GlobalSettings {
  override def onStop(app: Application) {
    super.onStop(app)
  }

  override def onStart(app: Application) {
    super.onStart(app)
  }
}