package actors.context.playframework

import util.OptionHelper._
import actors.context.{IActorContextProvider, IActorContext}

trait IActorContextProviderImpl extends IActorContextProvider {
  implicit val app = play.api.Play.current

  protected object Actors extends AbstractActorContext

  protected object MockActors extends AbstractMockActorContext

  protected val isMock = getOptionOrDefault(play.api.Play.configuration.getBoolean("akka.isMock"), false)

  def actors(): IActorContext = {
    if (isMock)
      MockActors
    else
      Actors
  }
}
