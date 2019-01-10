package org.zella.pyass.net.model.impl

import java.nio.file.Files

import better.files.File
import io.vertx.scala.core.http.HttpServerResponse
import org.zella.pyass.net.model.WriteResponse

class PythonResponseWriter(result: PyResult) extends WriteResponse {

  override def write(response: HttpServerResponse): Unit = {
    result match {
      case FilePyResult(file, true) =>
        response.sendFile(file.pathAsString)
      case FilePyResult(file, false) =>
        val contentType = Files.probeContentType(file.path)
        response.putHeader("content-type", contentType)
        //TODO potential OOM
        response.end(file.contentAsString)
    }
  }
}

sealed trait PyResult

case class FilePyResult(file: File, asText:Boolean) extends PyResult


