// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.trafficland" % "sbt-plugins" % "0.6.4")

addSbtPlugin("play" % "sbt-plugin" % "2.1-RC4")