package globals

import concurrent.ExecutionContext.Implicits._
import play.api.Logger._
import util.actors.{ScriptExecutorActor, ValidatorActor, fsm}
import util.actors.fsm.CancellableMapFSM
import util.OptionHelper._
import akka.actor.Props
import play.api.libs.concurrent.Akka

object Actors {
  implicit lazy val playApp = play.api.Play.current
  implicit lazy val configuration = play.api.Play.configuration
  lazy val ourlogger = logger

  lazy val delayMilli = getOptionOrDefault(configuration.getInt("actor.schedule.delayMilli"), 1000)

  lazy val schedule = Akka.system.actorOf(Props(() => new CancellableMapFSM(delayMilli)).withDispatcher("akka.actor.default-dispatcher"))
  lazy val validatorActorRef = Akka.system.actorOf(Props(() => new ValidatorActor(global, schedule)).withDispatcher("akka.actor.default-dispatcher"))
  lazy val scriptExecActorRef = Akka.system.actorOf(Props(() => new ScriptExecutorActor()).withDispatcher("akka.actor.default-dispatcher"))

  lazy val actors = schedule :: validatorActorRef :: scriptExecActorRef :: Nil
}
