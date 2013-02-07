package controllers

import play.api._
import play.api.mvc._

object ScriptController extends Controller{
  def rollBack(appName:String) ={
    Action{
      Ok("Execute Rollback script here!")
    }
  }
}
