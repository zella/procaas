package org.zella.pyaas.net.model.impl

import java.nio.file.Files

import better.files.File
import io.vertx.scala.core.http.HttpServerResponse
import monix.eval.Task
import org.zella.pyaas.net.model.Result


case class FilePyResult(file: File) extends Result {

  override def write(response: HttpServerResponse): Task[Unit] = Task(response.sendFile(file.pathAsString))

}


