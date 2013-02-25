package globals.playframework

import util.OptionHelper._
import globals.{IActors, IActorsProvider}

trait IActorsProviderImpl extends IActorsProvider {
  implicit val app = play.api.Play.current

  protected object Actors extends AbstractActors

  protected object MockActors extends AbstractMockActors

  protected val isMock = getOptionOrDefault(play.api.Play.configuration.getBoolean("akka.isMock"), false)

  def actors(): IActors = {
    if (isMock)
      MockActors
    else
      Actors
  }
}
