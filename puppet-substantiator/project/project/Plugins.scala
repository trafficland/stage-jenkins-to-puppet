import sbt._
// sets up other project dependencies when building our root project
object Plugins extends Build {
  lazy val root = Project("root", file(".")) dependsOn(tapListener)
  lazy val tapListener = RootProject(uri("git://github.com/mkhettry/sbt-tap.git"))
}