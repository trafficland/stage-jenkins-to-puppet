package controllers

import models.mongo.reactive.{IMachineReadersWriters, Machine}
import scala.concurrent._

class MachineControllerIntegrationSpec
  extends IRestControllerBehaviors[Machine]
  with IMachineReadersWriters {

  def createEntities(numberOfEntities: Int): Future[Int] = future(1)

  def createValidEntity: Machine = new Machine("test1")

  def createInvalidEntity: Machine = new Machine("invalid", None)

  override val entityName = "machines"

  override val collectionName = this.entityName


  ("MachineController" should {

    "MachineFakeTest1" in new ICleanDatabase {
      true
    }
  }) :: baseShould
}
