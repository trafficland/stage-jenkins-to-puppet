package util.evaluations

trait IEvaluate[R] {

  case class Pass(override val result: R) extends IEvaluated[R]

  case class Fail(override val result: R) extends IEvaluated[R]

  def evaluate: IEvaluated[R]

  def name:String

  def failAction(result: R)

  def passAction(result: R)

  def run() = {
    evaluate match {
      case Pass(result) =>
        passAction(result)
      case Fail(result) =>
        failAction(result)
      case _ =>
    }
  }
}
