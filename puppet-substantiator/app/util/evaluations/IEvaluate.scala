package util.evaluations

import scala.concurrent._

trait IEvaluate[R] {

  abstract class PassFail(override val result: R) extends IEvaluated[R]
  case class Pass(pResult: R) extends PassFail(pResult)
  case class Fail(fResult: R) extends PassFail(fResult)

  def evaluate()(implicit context: ExecutionContext): Future[IEvaluated[R]]

  def name: String

  def failAction(result: R)

  def passAction(result: R)
}
