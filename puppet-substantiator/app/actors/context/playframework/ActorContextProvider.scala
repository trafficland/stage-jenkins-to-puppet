package actors.context.playframework

import util.OptionHelper._
import actors.context.{ActorNames, IActorContextProvider, IActorContext}

trait ActorContextProvider extends IActorContextProvider {
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

object ActorContextProvider extends ActorContextProvider with ActorNames
