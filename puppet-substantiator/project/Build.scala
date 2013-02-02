import sbt._
import Keys._
import trafficland.opensource.sbt.plugins._

object OurBuild extends Build {

  val appName         = "puppet-substantiator"
  val appVersion      = "1.0.0-SNAPSHOT".toReleaseFormat()


  val compileDependencies = Seq(
    "com.chuusai" %% "shapeless" % "1.2.3",
    "org.reactivemongo" %% "reactivemongo" % "0.8",
    "play.modules.reactivemongo" %% "play2-reactivemongo" % "0.1-SNAPSHOT" cross CrossVersion.full
  )

  val gitHubDependencies: Array[ClasspathDep[ProjectReference]] =
    Array(RootProject(uri("https://github.com/nmccready/scala-erasure-experiments.git")))

  val appDependencies = compileDependencies

  val main = play.Project(appName, appVersion, appDependencies)
    .configs( DatabaseTests )
    .settings( inConfig(DatabaseTests)(Defaults.testTasks) : _* )
    .configs( AllTests )
    .settings( inConfig(AllTests)(Defaults.testTasks) : _* )
    .settings(
      resolvers ++= Seq(
        "sgodbillon" at "https://bitbucket.org/sgodbillon/repository/raw/master/snapshots/",
        "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
        "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
        ),
      testOptions in Test := Seq( Tests.Filter(s => databaseIndependentSpecsFilter(s)) ),
      testOptions in DatabaseTests := Seq( Tests.Filter(s => databaseDependentSpecsFilter(s)) ),
      testOptions in AllTests := Seq( Tests.Filter(s => allSpecsFilter(s))
    ),
    fork in Test := false,
    fork in DatabaseTests := false,
    fork in AllTests := false
  ).dependsOn(gitHubDependencies: _*)

  def systemSpecsFilter(name:String) : Boolean = name endsWith "SystemSpec"
  def integrationSpecsFilter(name:String) : Boolean = name endsWith "IntegrationSpec"
  def allSpecsFilter(name:String) : Boolean = name endsWith "Spec"
  def databaseDependentSpecsFilter(name:String) : Boolean = systemSpecsFilter(name) || integrationSpecsFilter(name)
  def databaseIndependentSpecsFilter(name:String) : Boolean = !systemSpecsFilter(name) && !integrationSpecsFilter(name)

  lazy val DatabaseTests =  config("dbt") extend(Test)
  lazy val AllTests =  config("all") extend(Test)
}