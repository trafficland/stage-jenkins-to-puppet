import sbt._
import Keys._
import trafficland.opensource.sbt.plugins._
import releasemanagement.ReleaseManagementPlugin
import scalaconfiguration.ScalaConfigurationPlugin
import versionmanagement.VersionManagementPlugin
import trafficland.opensource.sbt.plugins.packagemanagement.PackageManagementPlugin

object ApplicationBuild extends Build {

  val appName = "puppet-substantiator"
  val appVersion = "1.0.0-SNAPSHOT".toReleaseFormat()

  object Dependencies {

    object V {
      val reactiveMongo = "0.8"
      val scalaTest = "2.0.M5b"
      val mockito = "1.9.0"
      val akka = "2.1.0"
    }

    import V._

    val compileDependencies = Seq(
      "org.reactivemongo" %% "reactivemongo" % reactiveMongo,
      "org.scalatest" %% "scalatest" % scalaTest,
      "org.mockito" % "mockito-core" % mockito,
      "com.typesafe.akka" %% "akka-testkit" % akka,
      "play" %% "play-test" % play.core.PlayVersion.current
    )
  }

  val plugs: Seq[Setting[_]] =
    Seq[Setting[_]](PackageManagementPlugin.plug: _*) ++
//      Seq[Setting[_]](ReleaseManagementPlugin.plug: _ *) ++
      Seq[Setting[_]](ScalaConfigurationPlugin.plug: _ *) ++
//      Seq[Setting[_]](VersionManagementPlugin.plug: _ *) ++
      Seq[Setting[_]](Play20.plug: _*)

  val appDependencies = Dependencies.compileDependencies

  val main = play.Project(appName, appVersion, appDependencies)
    .configs(IntTests, AllTests)
    .settings(inConfig(IntTests)(Defaults.testTasks): _*)
    .settings(inConfig(AllTests)(Defaults.testTasks): _*)
    .settings(
    resolvers ++= Seq(
      "sgodbillon" at "https://bitbucket.org/sgodbillon/repository/raw/master/snapshots/",
      "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
      "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
    ),
    testListeners += SbtTapReporting(),
    testOptions in Test := Seq(
      Tests.Setup {
        () =>
          System.setProperty("config.file", "conf/test.conf")
          System.setProperty("http.port", "19007")
      },
      Tests.Filter(s => databaseIndependentSpecsFilter(s))),
    testOptions in IntTests := Seq(
      Tests.Setup {
        () =>
          System.setProperty("config.file", "conf/test.conf")
          System.setProperty("http.port", "19007")
      },
      Tests.Filter(s => databaseDependentSpecsFilter(s))),
    testOptions in AllTests := Seq(
      Tests.Setup {
        () => System.setProperty("config.file", "conf/test.conf")
        System.setProperty("http.port", "19007")
      },
      Tests.Filter(s => allSpecsFilter(s))
    ),
    fork in Test := false,
    fork in IntTests := false,
    fork in AllTests := false,
    parallelExecution in Test := false,
    parallelExecution in IntTests := false,
    parallelExecution in AllTests := false,
    javaOptions in Runtime += "-Dconfig.file=conf/test.conf",
    resourceDirectory in Compile <<= baseDirectory {
      _ / "resource"
    }
  )
    .settings(plugs: _*)
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings : _*)

  def systemSpecsFilter(name: String): Boolean = name endsWith "SystemSpec"

  def integrationSpecsFilter(name: String): Boolean = name endsWith "IntegrationSpec"

  def allSpecsFilter(name: String): Boolean = name endsWith "Spec"

  def databaseDependentSpecsFilter(name: String): Boolean = systemSpecsFilter(name) || integrationSpecsFilter(name)

  def databaseIndependentSpecsFilter(name: String): Boolean = !systemSpecsFilter(name) && !integrationSpecsFilter(name)

  lazy val IntTests = config("int") extend (Test)
  lazy val AllTests = config("all") extend (Test)
}