name := "proto"

version := "0.1"

scalaVersion := "2.12.8"

resolvers += "jwiki" at "https://dl.bintray.com/fastily/maven"

// https://mvnrepository.com/artifact/fastily/jwiki
libraryDependencies += "fastily" % "jwiki" % "1.7.0"
// https://mvnrepository.com/artifact/io.monix/monix-eval
libraryDependencies += "io.monix" %% "monix-eval" % "3.0.0-RC2"
// https://mvnrepository.com/artifact/com.typesafe/config
libraryDependencies += "com.typesafe" % "config" % "1.3.3"
// https://mvnrepository.com/artifact/io.vertx/vertx-lang-scala-stack
libraryDependencies += "io.vertx" %% "vertx-lang-scala-stack" % "3.6.2" % "provided" pomOnly()
// https://mvnrepository.com/artifact/io.vertx/vertx-web-scala
libraryDependencies += "io.vertx" %% "vertx-web-scala" % "3.6.2"
