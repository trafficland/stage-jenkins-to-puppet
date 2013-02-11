package controllers

import org.specs2.mutable.Specification
import org.specs2.specification.{Fragment, Fragments}
import services.repository.mongo.reactive.{TestMongoDbProvider, TestMachineRepositoryProvider}
import models.mongo.reactive.{IMachineReadersWriters, Machine}
import scala.concurrent._
import reactivemongo.bson.BSONObjectID

class MachineControllerIntegrationSpec
  extends IRestControllerBehaviors[Machine]
  with TestMachineRepositoryProvider
  with IMachineReadersWriters {

  def createEntities(numberOfEntities: Int): Future[Int] = future(1)

  def createValidEntity: Machine = new Machine(Some(BSONObjectID.generate), "test1")

  def createInvalidEntity: Machine = new Machine(Some(BSONObjectID.generate), "invalid")

  override val entityName = "machines"

  override val collectionName = this.entityName


  ("MachineController" should {

    "MachineFakeTest1" in {
      true
    }
  }).add(baseShould)
}
