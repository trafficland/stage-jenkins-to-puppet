//package util.actors
//
//import akka.actor.{Actor, ActorRef, FSM}
//import util.actors.fsm.BasicState._
//
//object ScheduleCancelFSM {
//
//  //states
//  case object Idle extends IState
//
//  case object Running extends IState
//
//  case object Closing extends IState
//
//  case object Closed extends IState
//
//  //stateData
//
//  case class IdleStateData(target: ActorRef) extends IStateData
//
//  trait INotIdleStateData extends IStateData
//
//  case class StartStateData(target: ActorRef) extends INotIdleStateData
//
//  case class KillStateData(target: ActorRef) extends INotIdleStateData
//
//  case class AttemptStateData(attempt: Int = 0, doGoToIdle: Boolean = false, target: ActorRef) extends INotIdleStateData
//
//  //Messages to be wrapped by events
//  trait IMsg
//
//  case class StartSystemMsg(onSystemFailureGoToIdle: Boolean = false) extends IMsg
//
//  case class SetTarget(target: ActorRef)
//
//  case object IdlSystemMsg extends IMsg
//
//  case class EndSystemMsg(doKill: Boolean = false) extends IMsg
//
//  case object InitStateMsg extends IMsg
//
//  protected[ConnectionFSM] case object PollMsg extends IMsg
//
//}
//
//case class ScheduleCancelFSM(
//                          thresholdConnectDisconnect: Int,
//                          timeOutSecondsInt: Int,
//                          pollInSeconds: Int,
//                          useConsoleForLog: Boolean = false) extends Actor with FSM[IState, IStateData] {
//
//  implicit val console = useConsoleForLog
//
//  import ConnectionFSM._
//
//  val timeoutDuration = timeOutSecondsInt seconds
//  val pollDuration = pollInSeconds seconds
//  val connectedTimerName = "ConnectedTimer"
//  val connectingTimerName = "ConnectingTimer"
//  val closingTimerName = "ClosingTimer"
//
//  startWith(Idle, IdleStateData(null))
//
//  onTransition {
//    case left -> Connecting =>
//      handleLeftLeavingState(left)
//      handleTransition(ExecuteConnectingWork)
//      setTimer(connectingTimerName, PollMsg, pollDuration, true)
//    case left -> Connected =>
//      handleLeftLeavingState(left)
//      handleTransition(ExecuteConnectedWork)
//      setTimer(connectedTimerName, PollMsg, pollDuration, true)
//    case left -> Closing =>
//      handleLeftLeavingState(left)
//      handleTransition(ExecuteClosingWork)
//      setTimer(closingTimerName, PollMsg, pollDuration, true)
//    case left -> Closed =>
//      handleLeftLeavingState(left)
//      handleTransition(ExecuteClosedWork)
//    case left -> Idle =>
//      handleLeftLeavingState(left)
//  }
//
//  def handleLeftLeavingState(state: IState) = {
//    state match {
//      case Connecting =>
//        cancelTimer(connectingTimerName)
//      case Connected =>
//        cancelTimer(connectedTimerName)
//      case Closing =>
//        cancelTimer(closingTimerName)
//      case _ =>
//    }
//  }
//
//  def handleTransition(executeWork: IExecuteWork) = {
//    nextStateData match {
//      case data: IStateData =>
//        self ! InitStateMsg
//        data.target ! executeWork
//    }
//  }
//
//  def handleEndMsg(doKill: Boolean, target: ActorRef) = {
//    if (!doKill)
//      goto(Closing) using StartStateData(target)
//    else
//      goto(Closed) using KillStateData(target)
//  }
//
//  when(Idle) {
//    case Event(SetTarget(target), _) =>
//      stay() using IdleStateData(target)
//    case Event(StartSystemMsg(idle), IdleStateData(target)) =>
//      goto(Connecting) using AttemptStateData(0, idle, target)
//    case Event(InitStateMsg | IdlSystemMsg, _) =>
//      stay()
//    case Event(EndSystemMsg(idle), data: IStateData) =>
//      goto(Closed) using AttemptStateData(0, idle, data.target)
//    case Event(EndSystemMsg, _) =>
//      goto(Closed)
//  }
//
//  when(Connecting) {
//    case Event(InitStateMsg | StateTimeout | ConnectingFail, data: INotIdleStateData) =>
//      data match {
//        case AttemptStateData(attempt, doGoToIdle, target) =>
//          if (attempt < thresholdConnectDisconnect)
//            stay using AttemptStateData(attempt + 1, doGoToIdle, target)
//          else {
//            goto(Closing) using AttemptStateData(0, doGoToIdle, target)
//          }
//        case _ =>
//          goto(Closing)
//      }
//    case Event(IdlSystemMsg, data: IStateData) =>
//      goto(Closing) using AttemptStateData(0, true, data.target)
//
//    case Event(EndSystemMsg(doKill), data: INotIdleStateData) =>
//      handleEndMsg(doKill, data.target)
//
//    case Event(ConnectingSuccess, data: INotIdleStateData) =>
//      logOrConsole("CONNECTING " + com.trafficland.utils.PrintStrings.MANY_/)
//      goto(Connected) using StartStateData(data.target)
//
//    case Event(PollMsg, data: INotIdleStateData) =>
//      data.target ! ExecuteConnectingWork
//      stay()
//  }
//
//  when(Connected, stateTimeout = timeoutDuration) {
//    case Event(InitStateMsg, _) =>
//      stay()
//
//    case Event(PollMsg, data: INotIdleStateData) =>
//      data.target ! ExecuteConnectedWork
//      stay()
//
//    case Event(IdlSystemMsg, data: INotIdleStateData) =>
//      goto(Closing) using AttemptStateData(0, true, data.target)
//
//    case Event(EndSystemMsg(doKill), data: INotIdleStateData) =>
//      handleEndMsg(doKill, data.target)
//
//    case Event(ConnectedSuccess, data: INotIdleStateData) =>
//      goto(Connected) using StartStateData(data.target)
//
//    case Event(StateTimeout | ConnectedFail, data: INotIdleStateData) =>
//      goto(Closing) using StartStateData(data.target)
//
//  }
//
//  when(Closing, stateTimeout = timeoutDuration) {
//    case Event(InitStateMsg | ClosingFail, data: INotIdleStateData) =>
//      data match {
//        case StartStateData(targ) =>
//          stay() using AttemptStateData(target = targ)
//        case AttemptStateData(curAttempt, idle, target) =>
//          stay() using AttemptStateData(curAttempt + 1, idle, target)
//      }
//
//    case Event(StateTimeout, data: INotIdleStateData) =>
//      goto(Closed) using KillStateData(data.target)
//
//    case Event(EndSystemMsg(doKill), data: INotIdleStateData) =>
//      handleEndMsg(doKill, data.target)
//
//    case Event(ClosingSuccess, _: INotIdleStateData) =>
//      goto(Closed)
//
//    case Event(PollMsg, AttemptStateData(attempt, idle, target)) =>
//      if (attempt < thresholdConnectDisconnect) {
//        target ! ExecuteClosingWork
//        stay()
//      } else {
//        goto(Closed) using KillStateData(target)
//      }
//  }
//
//  when(Closed) {
//    case Event(InitStateMsg, StartStateData(target)) =>
//      stop()
//    case Event(InitStateMsg, IdleStateData(target)) =>
//      stop()
//    case Event(InitStateMsg, attemptObj: AttemptStateData) =>
//      handleGoToIDleOrNot(attemptObj)
//    case Event(_, KillStateData(target)) =>
//      target ! ExecuteKillWork
//      stop()
//
//  }
//
//  def handleGoToIDleOrNot(closeData: AttemptStateData) =
//    closeData.doGoToIdle match {
//      case true =>
//        closeData.target ! RestartNotification(Idle)
//        goto(Idle) using closeData.copy(attempt = 0)
//      case false => stop()
//    }
//
//  whenUnhandled {
//    case Event(msg, work) => {
//      logOrConsole("CurrentState unhandled !!! : " + stateName)
//      logOrConsole("MSG unhandled !!! : " + msg.getClass)
//      logOrConsole("Work unhandled !!! : " + work.getClass)
//    }
//    stay()
//  }
//
//  initialize
//
//  def logOrConsole(msg: String)(implicit useConsole: Boolean) {
//    if (useConsole)
//      Console.println(msg)
//    else
//      log.debug(msg)
//  }
//}
//
//
//
