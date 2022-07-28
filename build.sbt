ThisBuild / organization := "com.github.jberggg"

Compile / compile / wartremoverErrors ++= Warts.allBut(Wart.Any, Wart.Nothing, Wart.Serializable)

val catsEffectVersion = "3.3.13"
val fs2Version = "3.2.9"

lazy val root = (project in file("."))
    .enablePlugins(ScalaJSPlugin)
    .settings(
        name := "fs2-spa-router",
        scalaVersion := "2.13.7",
        crossScalaVersions := List("2.13.7","3.1.3"),
        libraryDependencies ++=  List(
            "org.typelevel" %%% "cats-effect" % catsEffectVersion,
            "org.typelevel" %%% "cats-effect-kernel" % catsEffectVersion,
            "org.typelevel" %%% "cats-effect-std" % catsEffectVersion,
            "co.fs2" %%% "fs2-core"  % fs2Version,
            "org.http4s" %%% "http4s-dsl" % "0.23.13",
            "org.scala-js" %%% "scalajs-dom" % "2.2.0",
        ),
        scalacOptions ++= {
            if(scalaVersion.value.startsWith("3"))
                List(
                    "-Xfatal-warnings",
                    "-language:postfixOps",
                    "-deprecation",
                    "-unchecked",
                    "-Ykind-projector",
                )
            else
                List(
                    "-Xfatal-warnings",
                    "-language:postfixOps",
                    "-deprecation",
                    "-unchecked",
                    "-Ywarn-value-discard",
                    "-Ywarn-unused",
                )
        }
    )
