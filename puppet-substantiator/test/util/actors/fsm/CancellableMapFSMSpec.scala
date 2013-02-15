package util.actors.fsm

import akka.actor._
import akka.testkit._
import org.scalatest._
import org.scalatest.matchers.{ShouldMatchers, MustMatchers}
import util.actors.fsm.BasicState._

class CancellableMapFSMSpec(_system: ActorSystem)
  extends TestKit(_system) with ImplicitSender
  with WordSpec with MustMatchers with BeforeAndAfterAll
  with BeforeAndAfter with ShouldMatchers with IMapFSMSPec[ICancellableDelay, CancellableMapFSM] {

  def this() = this(ActorSystem("CancellableMapFSMSpec"))

  override def afterAll {
    system.shutdown()
  }

  implicit val localDomain = CancellableMapFSMDomainProvider.domain

  import localDomain._

  def create(delay: Int) = CancellableDelay(delay, new Cancellable {
    var localCancel = false

    def isCancelled = localCancel

    def cancel() {
      localCancel = true
    }
  })

  def create() = create(5000)

  def initialize() = {
    val fsm = TestFSMRef[IState, IStateData, CancellableMapFSM](new CancellableMapFSM(3000))
    fsm ! SetTarget(Some(fsm.underlying.self))
    fsm
  }


  "adding" should {
    "put fsm in Active state with a single add" in {
      val fsm = initialize()
      val put = create(5000)
      fsm ! Add("test1", put)
      fsm.stateName should be(Active)
      getStateMap(fsm)(localDomain)("test1") should be(put)
    }

    "contain two items when two are added" in {
      val fsm = initialize()
      fsm ! Add("test1", create(5000))
      fsm ! Add("test2", create(5000))
      fsm.stateName should be(Active)
      val size = fsm.stateData match {
        case Todo(_, map) => map.size
        case _ => 0
      }
      size should be(2)
    }
    "contain one item when two of the same are added" in {
      val fsm = initialize()
      fsm ! Add("test1", create(5000))
      fsm ! Add("test1", create(5000))
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
      fsm ! Add("test1", create(5000))
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

    "then flush should be handled and goes back to idle" in {
      val fsm = initialize()
      createAndInject(200, fsm)
      fsm.stateName should be(Active)
      getStateMap(fsm)(localDomain).size should be(200)
      fsm ! Flush
      getStateMap(fsm)(localDomain).size should be(0)
    }
  }
}
