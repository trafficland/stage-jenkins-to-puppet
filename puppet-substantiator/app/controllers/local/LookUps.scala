package controllers.local

import controllers.{AppsController, MachinesController}

object LookUp {

  protected val machinesRepo = MachinesController.repository
  protected val appsRepo = AppsController.repository

  val machineLookUp = MachineLookup(machinesRepo)


}
