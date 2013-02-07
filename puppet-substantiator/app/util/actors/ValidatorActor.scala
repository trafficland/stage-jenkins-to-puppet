package services.actors

import akka.actor._
import scala.concurrent.duration._
import services.actors.ValidatorActorMessages._
import util.evaluations.IEvaluate
import concurrent.ExecutionContext


object ValidatorActorMessages {

  trait IValidateMsg

  abstract class Validate[T](toBeEvaluated: IEvaluate[T]) extends IValidateMsg

  case class StartValidation[T](delayMilli: Int, toBeEvaluated: IEvaluate[T], system: ActorSystem) extends Validate(toBeEvaluated)

  case class TickValidation[T](toBeEvaluated: IEvaluate[T]) extends Validate(toBeEvaluated)

  case class AbortValidation() extends IValidateMsg

}

class ValidatorActor(context: ExecutionContext) extends Actor {
  implicit val ctx = context

  def receive = {
    case StartValidation(delayMilli, eval, system) =>
      system.scheduler.scheduleOnce(delayMilli milliseconds, self, TickValidation(eval))
    case TickValidation(eval) =>
      eval.run()

  }
}
