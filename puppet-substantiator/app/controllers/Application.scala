package controllers

import play.api.mvc._
import play.api.GlobalSettings

object Application extends Controller with GlobalSettings {

  def version = Action {
    Ok(AppInfo.version)
  }

  def routes = Action {
    req =>
      onHandlerNotFound(req)
  }

  def baseUrl = Action {
    request =>
      val url = request.host
      Ok(url)
  }
}