package util.playframework

//NOTE: This class should only be used in the SBT build configuration to ensure the test server is stopped properly.
class StopPlayTestServerCommand() {
  PlayTestServerManager.stopServer()
}
