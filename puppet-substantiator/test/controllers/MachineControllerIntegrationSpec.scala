package controllers

import models.mongo.reactive.{IMachineReadersWriters, Machine}
import scala.concurrent._
import play.api.libs.iteratee.Enumerator

class MachineControllerIntegrationSpec
  extends IRestControllerBehaviors[Machine]
  with IMachineReadersWriters {

  def createEntities(numberOfEntities: Int): Future[Int] = {
    val entities = (1 to numberOfEntities) map {
      index =>
        new Machine("name%d".format(index))
    }
    db(collectionName).insert[Machine](Enumerator(entities: _*))
  }

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
