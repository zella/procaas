package org.zella.pyaas.net.model.impl

import java.io.{BufferedReader, InputStreamReader}
import java.util.concurrent.TimeUnit

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import io.vertx.scala.core.http.HttpServerResponse
import monix.eval.Task
import org.zella.japi.TestJ
import org.zella.pyaas.net.model.Result
import org.zella.pyaas.proc.ProcessException


sealed trait PyResult extends Result

case class FilePyResult(file: File) extends PyResult {

  override def write(response: HttpServerResponse): Task[Unit] = Task(response.sendFile(file.pathAsString))

}

case class StdoutPyResult(out: String) extends PyResult {

  override def write(response: HttpServerResponse): Task[Unit] = Task(response.end(out))

}

case class StdoutChunkedPyResult(process: Process) extends PyResult with LazyLogging {

  //TODO make it better, i think we can use Observable
  //TODO how chunked proccess should be executed?
  //write should be executed on io
  override def write(response: HttpServerResponse): Task[Unit] = Task {
    response.setChunked(true)

    val reader = new BufferedReader(new InputStreamReader(process.getInputStream))
    Stream.continually(reader.readLine()).takeWhile(_ != null)
      .foreach(line => {
        logger.debug(line)
        response.write(line)
      })
    //FIXME process should be sync, because it can consume cpu
    val completed = process.waitFor(60, TimeUnit.MINUTES) //TODO
    if (!completed) {
      try {
        process.destroyForcibly().waitFor()
      } catch {
        case ex: Throwable =>
          throw new ProcessException("Process timeout. Can't destroy", ex)
      }
      throw new ProcessException("Process timeout")
    }
    val exitCode = process.exitValue()
    if (exitCode != 0) throw new ProcessException(s"Incorrect exit code: $exitCode")
    response.end()
  }

}


