package util.actors

import util.evaluations.IEvaluate

case class TestEvaluate(doFail: Boolean) extends IEvaluate[String] {
  def evaluate = {
    if (doFail)
      Fail("fail")
    else
      Pass("pass")
  }

  def name = "test"

  var state: String = ""

  def failAction(result: String) {
    state = result
  }

  def passAction(result: String) {
    state = result
  }
}
