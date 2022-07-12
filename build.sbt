ThisBuild / organization := "com.github.jberggg"
ThisBuild / scalaVersion := "2.13.5"

ThisBuild / scalacOptions ++= List(
    "-Xfatal-warnings",
    "-language:postfixOps",
    "-deprecation",
    "-unchecked",
    "-Ywarn-value-discard",
    "-Ywarn-unused",
)

Compile / compile / wartremoverErrors ++= Warts.allBut(Wart.Any, Wart.Nothing, Wart.Serializable)

val catsEffectVersion = "3.3.13"
val fs2Version = "3.2.9"

lazy val root = (project in file("."))
    .settings(
        name := "fs2-spa-router",
        libraryDependencies ++= Seq(
            "org.typelevel" %%% "cats-effect" % catsEffectVersion,
            "org.typelevel" %%% "cats-effect-kernel" % catsEffectVersion,
            "org.typelevel" %%% "cats-effect-std" % catsEffectVersion,
            "co.fs2" %%% "fs2-core"  % fs2Version,
            "co.fs2" %%% "fs2-io" % fs2Version,
            "org.scala-js" %%% "scala-js-macrotask-executor" % "1.0.0",
            "com.github.cornerman" %%% "colibri-router" % "0.5.0", // TODO: Get rid of this dependency
        ),
        addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full),
    )
    .enablePlugins(ScalaJSPlugin)