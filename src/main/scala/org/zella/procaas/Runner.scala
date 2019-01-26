package org.zella.procaas

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.zella.procaas.config.impl.TypesafeConfig
import org.zella.procaas.net.ProcaasHttpServer
import monix.execution.Scheduler.Implicits.global

object Runner extends LazyLogging {

  def main(args: Array[String]): Unit = {
    sys.addShutdownHook({
      logger.info("Server shutdown")
    })
    val config = new TypesafeConfig(ConfigFactory.load())

    val server = new ProcaasHttpServer(config).startT()
      .runSyncUnsafe(timeout = config.httpStartTimeout)

    logger.info(s"Server started at port ${config.httpPort}")
  }
}
