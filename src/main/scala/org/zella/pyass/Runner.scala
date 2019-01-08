package org.zella.pyass

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.zella.pyass.config.impl.TypesafeConfig
import org.zella.pyass.net.HttpServer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await

object Runner extends LazyLogging {

  def main(args: Array[String]): Unit = {

    val config = new TypesafeConfig(ConfigFactory.load())

    val started = new HttpServer(config).startFuture()
    started.foreach(port => logger.info(s"Server started at port $port"))

    Await.result(started, 30.seconds)

  }
}

