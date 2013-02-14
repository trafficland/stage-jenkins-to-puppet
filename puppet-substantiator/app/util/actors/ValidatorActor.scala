package util.actors

import akka.actor._
import scala.concurrent.duration._
import util.evaluations.IEvaluate
import concurrent.ExecutionContext
import util.actors.fsm.CancellableDelay

object ValidatorActor {

  trait IValidateMsg

  abstract class Validate[T](toBeEvaluated: IEvaluate[T]) extends IValidateMsg

  case class StartValidation[T](delayMilli: Int, toBeEvaluated: IEvaluate[T], system: ActorSystem) extends Validate(toBeEvaluated)

  case class TickValidation[T](toBeEvaluated: IEvaluate[T]) extends Validate(toBeEvaluated)

  case class AbortValidation() extends IValidateMsg

}

import ValidatorActor._

class ValidatorActor(execCtx: ExecutionContext, scheduleMaintainer: ActorRef) extends Actor {
  implicit val ctx = execCtx

  import util.actors.fsm.CancellableMapFSMDomainProvider.domain._


  def receive = {
    case StartValidation(delayMilli, eval, system) =>
      //dependent on evaluation name being unique
      val cancel = system.scheduler.scheduleOnce(delayMilli milliseconds, self, TickValidation(eval))
      scheduleMaintainer ! Add(eval.name, CancellableDelay(delayMilli, cancel))
    case TickValidation(eval) =>
      scheduleMaintainer ! Remove(eval.name)
      eval.run()

  }
}
