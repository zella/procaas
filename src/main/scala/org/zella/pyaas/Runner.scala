package org.zella.pyaas

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import monix.execution.Scheduler.Implicits.global
import org.zella.pyaas.config.impl.TypesafeConfig
import org.zella.pyaas.net.PyaasHttpServer

object Runner extends LazyLogging {

  def main(args: Array[String]): Unit = {

    val config = new TypesafeConfig(ConfigFactory.load())

    val server = new PyaasHttpServer(config).startT()
      .runSyncUnsafe(timeout = config.httpStartTimeout)

    logger.info(s"Server started at port ${config.httpPort}")
  }
}
