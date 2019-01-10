package org.zella.pyass

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.zella.pyass.config.impl.TypesafeConfig
import org.zella.pyass.net.HttpServer
import org.zella.pyass.proc.ProcessRunner

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

object Runner extends LazyLogging {

  def main(args: Array[String]): Unit = {

    val config = new TypesafeConfig(ConfigFactory.load())

    val pr = new ProcessRunner

    val startedF = new HttpServer(config, pr).startFuture()
    startedF.foreach(port => logger.info(s"Server started at port $port"))

    Await.result(startedF, config.httpStartTimeout)

  }
}

