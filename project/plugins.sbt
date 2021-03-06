resolvers += Resolver.bintrayIvyRepo("kamon-io", "sbt-plugins")

lazy val root: Project = project.in(file(".")).dependsOn(latestSbtUmbrella)
lazy val latestSbtUmbrella = uri("git://github.com/kamon-io/kamon-sbt-umbrella.git")

addSbtPlugin("io.kamon" % "sbt-aspectj-runner" % "1.0.1")