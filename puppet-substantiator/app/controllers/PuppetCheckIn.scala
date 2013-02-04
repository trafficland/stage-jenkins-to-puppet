package controllers

import play.api._
import play.api.mvc._
import models._

object PuppetCheckIn extends Controller {

  def actual(machineId: String, appId: String, actualObjective: String) = Action {
    Ok("Actual Values Set")
  }

  def expected = Action{
    //accept a json blob of a system (Application cluster that is expected
    Ok("Expectation Set")
  }
}
