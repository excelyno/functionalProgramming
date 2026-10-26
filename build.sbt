name := "verdant"
version := "0.1.0"
scalaVersion := "3.3.4"

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "cask" % "0.9.4",
  "com.lihaoyi" %% "upickle" % "3.3.1",
  "com.lihaoyi" %% "scalatags" % "0.13.1",
  "org.xerial" % "sqlite-jdbc" % "3.47.1.0",
  "org.scalameta" %% "munit" % "1.0.3" % Test
)

Test / testFrameworks += new TestFramework("munit.Framework")

fork := true
