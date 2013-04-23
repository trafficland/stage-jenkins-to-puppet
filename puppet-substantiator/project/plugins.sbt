// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.url("Artifactory Online", url("http://repo.scala-sbt.org/scalasbt/repo"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.trafficland" % "sbt-plugins" % "0.6.8")

addSbtPlugin("play" % "sbt-plugin" % "2.1.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.1")