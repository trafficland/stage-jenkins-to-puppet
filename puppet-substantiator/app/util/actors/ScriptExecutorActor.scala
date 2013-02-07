package services.actors

import akka.actor.Actor

/*
Likley to be a IO actor to execute scripts
Scripts to be stored on File system or mongo db? Only a read operation so FS could be ok
 */
class ScriptExecutorActor extends Actor {
  def receive = {
    case scriptName: String =>
    //find and run script!
  }
}
