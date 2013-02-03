package util

import play.api._

trait IConfigurationProvider {
  val configuration: Configuration
}

trait ConfigurationProvider extends IConfigurationProvider {
  override lazy val configuration = Play.maybeApplication match {
    case Some(app: Application) => app.configuration
    case None => Configuration.empty
  }
}
