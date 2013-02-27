package actors

import util.evaluations.IEvaluate
import concurrent._


case class TestEvaluate(doFail: Boolean,override val name:String = "test") extends IEvaluate[String] {
  def evaluate()(implicit context: ExecutionContext) = {
    if (doFail) {
      state = "fail"
      future(Fail("fail"))
    }
    else {
      state = "pass"
      future(Pass("pass"))
    }
  }

  var state: String = ""

  def failAction(result: String) {
    state = result
  }

  def passAction(result: String) {
    state = result
  }

}
