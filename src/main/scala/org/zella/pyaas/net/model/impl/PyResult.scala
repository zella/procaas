package org.zella.pyaas.net.model.impl

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import io.vertx.scala.core.http.HttpServerResponse
import monix.eval.Task
import monix.reactive.Observable
import org.zella.pyaas.net.model.Result


sealed trait PyResult extends Result

case class FilePyResult(file: File) extends PyResult with LazyLogging {

  override def write(response: HttpServerResponse): Task[Unit] = Task {
    logger.debug("Send result file...")
    response.sendFile(file.pathAsString)
  }

}

case class StdoutPyResult(out: String) extends PyResult {

  override def write(response: HttpServerResponse): Task[Unit] = Task(response.end(out))

}

case class StdoutChunkedPyResult(out: Observable[String]) extends PyResult with LazyLogging {

  //TODO how chunked proccess should be executed?
  //write should be executed on io
  override def write(response: HttpServerResponse): Task[Unit] = Task {
    response.setChunked(true)
  }.flatMap { _ =>
    out.foreachL(line => {
      logger.debug(line)
      response.write(line)
    })
  }.flatMap(_ => Task {
    response.end()
  })


}


