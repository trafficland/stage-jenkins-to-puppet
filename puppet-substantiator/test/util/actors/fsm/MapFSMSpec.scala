package util.actors.fsm

import akka.actor._
import akka.testkit._
import org.scalatest._
import org.scalatest.matchers.{ShouldMatchers, MustMatchers}
import util.actors.fsm.BasicState._

trait IMapFSMSPec[T, FSM <: MapFSM[T]] {

  def getStateMap(fsm: TestFSMRef[IState, IStateData, FSM])(implicit domain: MapFSMDomain[T]) = {
    import domain._

    fsm.stateData match {
      case Todo(_, map) => map
      case _ => Map.empty[String, T]
    }
  }

  def createPairs(size: Int): Seq[(String, T)] = {
    (0 until size) map {
      i =>
        val count = i + 1
        "test" + count -> create
    }
  }

  def createAndInject(size: Int, fsm: TestFSMRef[IState, IStateData, FSM])(implicit domain: MapFSMDomain[T]) = {
    import domain._
    createPairs(size).foreach(pair => fsm ! Add(pair._1, pair._2))
  }

  def create: T
}

class AnyMapFSMSpec(_system: ActorSystem)
  extends TestKit(_system) with ImplicitSender
  with WordSpec with MustMatchers with BeforeAndAfterAll
  with BeforeAndAfter with ShouldMatchers with IMapFSMSPec[Any, AnyMapFSM] {

  def this() = this(ActorSystem("AnyMapFSMSpec"))

  override def afterAll {
    system.shutdown()
  }

  implicit val localDomain = AnyMapFSMDomainProvider.domain

  import localDomain._

  var counter = 0

  def create = counter += 1

  def initialize() = {
    val fsm = TestFSMRef[IState, IStateData, AnyMapFSM](new AnyMapFSM())
    fsm ! SetTarget(fsm.underlying.self)
    fsm
  }


  "adding" should {
    "put fsm in Active state with a single add" in {
      val fsm = initialize()
      fsm ! Add("test1", 1)
      fsm.stateName should be(Active)
      getStateMap(fsm)(localDomain)("test1") should be(1)
    }

    "contain two items when two are added" in {
      val fsm = initialize()
      fsm ! Add("test1", 1)
      fsm ! Add("test2", 1)
      fsm.stateName should be(Active)
      val size = fsm.stateData match {
        case Todo(_, map) => map.size
        case _ => 0
      }
      size should be(2)
    }
    "contain one item when two of the same are added" in {
      val fsm = initialize()
      fsm ! Add("test1", 1)
      fsm ! Add("test1", 1)
      fsm.stateName should be(Active)
      val size = fsm.stateData match {
        case Todo(_, map) => map.size
        case _ => 0
      }
      size should be(1)
    }
  }
  "removal" should {
    "a single active should put state into idle" in {
      val fsm = initialize()
      fsm ! Add("test1", 1)
      fsm ! Remove("test1")
      fsm.stateName should be(Idle)
      getStateMap(fsm)(localDomain).size should be(1)
    }
    "a single remove from an empty map should not fail in idle" in {
      val fsm = initialize()
      fsm ! Remove("test1")
      fsm.stateName should be(Idle)
      getStateMap(fsm)(localDomain).size should be(0)
    }
    "multiple removes byond the map size should not fail and go to idle" in {
      val fsm = initialize()
      fsm ! Remove("test1")
      fsm ! Remove("test2")
      fsm ! Remove("test3")
      fsm.stateName should be(Idle)
      getStateMap(fsm)(localDomain).size should be(0)
    }
  }
  "adding many" should {
    "be handled" in {
      val fsm = initialize()
      createAndInject(200, fsm)
      fsm.stateName should be(Active)
      getStateMap(fsm)(localDomain).size should be(200)
    }

    "then flushbe handled and back to idle" in {
      val fsm = initialize()
      createAndInject(200, fsm)
      fsm.stateName should be(Active)
      getStateMap(fsm)(localDomain).size should be(200)
      fsm ! Flush
      getStateMap(fsm)(localDomain).size should be(0)
    }
  }
}
