package controllers

import play.api._
import play.api.mvc._
import models._

object PuppetCheckIn extends Controller {

  def actual(machineId: String, appId: String, actualObjective: String) = Action {
    Ok("Actual Values Set")
  }

  def expected(machineId: String, appId: String, expectedObjective: String)= Action{
    Ok("Expectation Set")
  }
}
