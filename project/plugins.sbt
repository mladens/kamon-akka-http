import sbt._

resolvers += Resolver.bintrayIvyRepo("kamon-io", "sbt-plugins")

lazy val root: Project = (project in file(".")).dependsOn(latestSbtUmbrella)
lazy val latestSbtUmbrella = RootProject(uri("git://github.com/kamon-io/kamon-sbt-umbrella.git"))

addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % "0.1.4")
