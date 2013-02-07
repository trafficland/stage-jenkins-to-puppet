package controllers

import play.api._
import play.api.mvc._

/*
End point to call IO Actor to run scripts
 */
object ScriptController extends Controller{
  def rollBack(appName:String) ={
    Action{
      Ok("Execute Rollback script here!")
    }
  }
}
