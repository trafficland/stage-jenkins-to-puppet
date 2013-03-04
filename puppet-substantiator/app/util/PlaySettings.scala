package util

import util.OptionHelper._

object PlaySettings {
  protected lazy val app = play.api.Play.current
  lazy val absUrl = getOptionOrDefault(app.configuration.getString("playBaseUrl"), "localhost:9000")
}
