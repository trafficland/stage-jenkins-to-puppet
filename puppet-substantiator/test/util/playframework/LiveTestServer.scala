package util.playframework


trait LiveTestServer {
  protected lazy val fakeApp: Option[play.api.test.FakeApplication] = None
  private val testServer = PlayTestServerManager.getTestServer(fakeApp)
  protected val port = testServer.port
  protected implicit val app = testServer.application

  protected def stopServer(){
    PlayTestServerManager.stopServer()
  }

}
