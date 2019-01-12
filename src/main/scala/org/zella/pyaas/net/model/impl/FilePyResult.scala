package org.zella.pyaas.net.model.impl

import java.io.BufferedReader
import java.nio.file.Files

import better.files.File
import io.vertx.scala.core.http.HttpServerResponse
import monix.eval.Task
import monix.reactive.Observable
import org.zella.pyaas.net.model.Result


sealed trait PyResult extends Result

case class FilePyResult(file: File) extends PyResult {

  override def write(response: HttpServerResponse): Task[Unit] = Task(response.sendFile(file.pathAsString))

}

case class StdoutPyResult(out: String) extends PyResult {

  override def write(response: HttpServerResponse): Task[Unit] = Task(response.end(out))

}

case class StdoutChunkedPyResult(out: BufferedReader) extends PyResult {

  //TODO test it
  override def write(response: HttpServerResponse): Task[Unit] =
    Observable.fromLinesReader(Task(out))
      .doOnStart(_ => Task(response.setChunked(true)))
      .foreachL(chunk => response.write(chunk))
      .doOnFinish(_ => Task(response.end()))


}


