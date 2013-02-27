package actors.fsm

import concurrent._

object Messages {

  trait IWork {
  }
  trait IWorkItem {
    def func: () => Future[Boolean]
  }

  trait IExecuteWork extends IWork {
    def objectName: String
  }
}
