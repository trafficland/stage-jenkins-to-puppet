import sbt._
import Keys._
import trafficland.opensource.sbt.plugins._

object ApplicationBuild extends Build {

  val appName = "puppet-substantiator"
  val appVersion = "1.0.0-SNAPSHOT".toReleaseFormat()


  val compileDependencies = Seq(
    "com.chuusai" %% "shapeless" % "1.2.3",
    "org.reactivemongo" %% "reactivemongo" % "0.8",
    "org.scalatest" %% "scalatest" % "2.0.M5b",
    "org.mockito" % "mockito-core" % "1.9.0"
  )

  val gitHubDependencies: Array[ClasspathDep[ProjectReference]] =
    Array(RootProject(uri("https://github.com/nmccready/scala-erasure-experiments.git")))

  val appDependencies = compileDependencies

  val main = play.Project(appName, appVersion, appDependencies)
    .configs(IntTests)
    .settings(inConfig(IntTests)(Defaults.testTasks): _*)
    .configs(AllTests)
    .settings(inConfig(AllTests)(Defaults.testTasks): _*)
    .settings(
    resolvers ++= Seq(
      "sgodbillon" at "https://bitbucket.org/sgodbillon/repository/raw/master/snapshots/",
      "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
      "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
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

  ).dependsOn(gitHubDependencies: _*)

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