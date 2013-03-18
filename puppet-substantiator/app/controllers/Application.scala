package controllers

import play.api.mvc._
import play.api._
import AppInfo._

object Application extends Controller with GlobalSettings {

  def getVersion = Action {
    Ok("%s : %s : %s".format(name, version, vendor))
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