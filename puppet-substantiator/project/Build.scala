import sbt._
import Keys._
import trafficland.opensource.sbt.plugins._

object ApplicationBuild extends Build {

  val appName = "puppet-substantiator"
  val appVersion = "1.0.0-SNAPSHOT".toReleaseFormat()

  object Dependencies {

    object V {
      val spray = "1.1-M7"
      val sprayJson="1.2.3"
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
      "io.spray" % "spray-can" % spray,
      "io.spray" % "spray-http" % spray,
      "io.spray" % "spray-util" % spray,
      "io.spray" % "spray-httpx" % spray,
      "io.spray" %% "spray-json" % sprayJson,
      "play" %% "play-test" % play.core.PlayVersion.current
    )
  }


  //  val gitHubDependencies: Array[ClasspathDep[ProjectReference]] =
  //    Array(RootProject(uri("https://github.com/nmccready/scala-erasure-experiments.git")))

  val appDependencies = Dependencies.compileDependencies

  val main = play.Project(appName, appVersion, appDependencies)
    .configs(IntTests)
    .settings(inConfig(IntTests)(Defaults.testTasks): _*)
    .configs(AllTests)
    .settings(inConfig(AllTests)(Defaults.testTasks): _*)
    .settings(
    resolvers ++= Seq(
      "sgodbillon" at "https://bitbucket.org/sgodbillon/repository/raw/master/snapshots/",
      "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
      "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
      "spray repo" at "http://repo.spray.io"
    ),
    sourceGenerators in Compile <+= sourceManaged in Compile map {
      outDir: File =>
        writeVersion(outDir)
    },
    testOptions in Test := Seq(
      Tests.Setup {
        () => System.setProperty("config.file", "conf/test.conf")
      },
      Tests.Filter(s => databaseIndependentSpecsFilter(s))),
    testOptions in IntTests := Seq(
      Tests.Setup {
        () => System.setProperty("config.file", "conf/test.conf")
      },
      Tests.Filter(s => databaseDependentSpecsFilter(s))),
    testOptions in AllTests := Seq(
      Tests.Setup {
        () => System.setProperty("config.file", "conf/test.conf")
      },
      Tests.Filter(s => allSpecsFilter(s))
    ),
    fork in Test := false,
    fork in IntTests := false,
    fork in AllTests := false,
    parallelExecution in Test := false,
    parallelExecution in IntTests := false,
    parallelExecution in AllTests := false,
    javaOptions in Runtime += "-Dconfig.file=conf/test.conf"

  )
  //    .dependsOn(gitHubDependencies: _*)

  def writeVersion(outDir: File) = {
    val file = outDir / "controllers/AppInfo.scala"
    IO.write(file,
      """package controllers
    object AppInfo {
      val version = "%s"
      val name = "%s"
      val vendor = "mccready"
    }""".format(appVersion, appName))
    Seq(file)
  }

  def systemSpecsFilter(name: String): Boolean = name endsWith "SystemSpec"

  def integrationSpecsFilter(name: String): Boolean = name endsWith "IntegrationSpec"

  def allSpecsFilter(name: String): Boolean = name endsWith "Spec"

  def databaseDependentSpecsFilter(name: String): Boolean = systemSpecsFilter(name) || integrationSpecsFilter(name)

  def databaseIndependentSpecsFilter(name: String): Boolean = !systemSpecsFilter(name) && !integrationSpecsFilter(name)

  lazy val IntTests = config("int") extend (Test)
  lazy val AllTests = config("all") extend (Test)
}