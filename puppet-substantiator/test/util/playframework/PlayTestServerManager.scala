package util.playframework

import play.api.test.TestServer

object PlayTestServerManager {

  protected lazy val port = System.getProperty("http.port").toInt
  protected var testServer: Option[TestServer] = None
  protected val lock = new AnyRef

  def getTestServer(optionalApp: Option[play.api.test.FakeApplication] = None): TestServer = {
    lock.synchronized {
      testServer match {
        case Some(server) =>
          println("SPEC SETUP: Play test server already started on port %s".format(port))
          server
        case None => {
          createNewTestServer(optionalApp)
        }
      }
    }
  }

  protected def createNewTestServer(optionalApp: Option[play.api.test.FakeApplication] = None): TestServer = {
    println("SPEC SETUP: Starting Play test server on port %s".format(port))
    val newTestServer = optionalApp match {
      case Some(app) => TestServer(port, app)
      case None => TestServer(port)
    }
    newTestServer.start()
    testServer = Some(newTestServer)
    println("SPEC SETUP: Started Play test server on port %s".format(port))
    newTestServer
  }

  def stopServer() {

    testServer match {
      case Some(server) =>
        server.stop()
        testServer = None
        println("SPEC CLEANUP: Stopped play server after testing.")
      case None => println("SPEC CLEANUP: There was no running test server.")
    }
  }

}