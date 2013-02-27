package controllers

import play.api.mvc._
import play.api.GlobalSettings
import play.api._

object Application extends Controller with GlobalSettings {

  def version = Action {
    Ok(AppInfo.version)
  }

  def getRoutes = Action {
    req =>
      Ok(views.html.defaultpages.devNotFound(req, Play.maybeApplication.flatMap(_.routes)))
  }

  def baseUrl = Action {
    request =>
      val url = request.host
      Ok(url)
  }
}