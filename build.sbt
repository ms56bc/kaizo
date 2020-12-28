name := "kaizo"

version := "0.0.1"

scalaVersion := "2.13.4"
scalacOptions ++= Seq("-language:higherKinds", "-language:postfixOps", "-Ymacro-annotations")
val http4sVersion = "0.21.8"
val zioVersion = "1.0.3"
val catsVersion = "2.3.1"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  Resolver.jcenterRepo,
  "Apache Releases Repository" at "https://repository.apache.org/content/repositories/releases/",
  DefaultMavenRepository,
  Resolver.mavenLocal
)

libraryDependencies ++= cats ++ http4s ++ circe ++ zio ++ Seq(compilerPlugin("org.typelevel" % "kind-projector_2.13.4" % "0.11.2"))

val cats =  Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-effect" % catsVersion withSources() withJavadoc()
)

val zio = Seq(
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion,
  "dev.zio" %% "zio-interop-cats" % "2.2.0.1",
  "dev.zio" %% "zio-macros" % zioVersion
)

val http4s =  Seq("org.http4s" %% "http4s-core" % http4sVersion ,
  "org.http4s" %% "http4s-server" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-client" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion)

val circe: Seq[ModuleID] = {
  def circe(name: String, version: String = "0.13.0") =
    "io.circe" %% s"circe-$name" % version

  Seq(
    circe("core"),
    circe("generic"),
    circe("generic-extras"),
    circe("parser")
  )
}