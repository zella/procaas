package org.zella.procaas.net.model.impl

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import io.vertx.scala.core.http.HttpServerResponse
import monix.eval.Task
import monix.reactive.Observable
import org.zella.procaas.net.model.Result


sealed trait ProcResult extends Result

case class FileProcResult(file: File) extends ProcResult with LazyLogging {

  override def write(response: HttpServerResponse): Task[Unit] = Task {
    logger.debug("Send result file...")
    response.sendFile(file.pathAsString)
  }

}

case class StdoutProcResult(out: String) extends ProcResult with LazyLogging {

  override def write(response: HttpServerResponse): Task[Unit] = Task {
    logger.debug(out)
    response.end(out)
  }

}

case class StdoutChunkedProcResult(out: Observable[String]) extends ProcResult with LazyLogging {

  override def write(response: HttpServerResponse): Task[Unit] = Task {
    logger.debug("setChunked")
    response.setChunked(true)
  }.flatMap { _ =>
    out.
      foreachL(line => {
        logger.debug(line)
        response.write(line)
      })
  }.doOnFinish(exOpt => Task {
    exOpt match {
      case None => response.end()
      case Some(e) => //do nothing, response will be written next in error handler
    }
  })
}


