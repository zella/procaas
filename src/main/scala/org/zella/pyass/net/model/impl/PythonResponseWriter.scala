package org.zella.pyass.net.model.impl

import better.files.File
import io.vertx.scala.core.http.HttpServerResponse
import org.zella.pyass.net.model.WriteResponse
import play.api.libs.json.{JsValue, Json}

class PythonResponseWriter(result: PyResult) extends WriteResponse {

  override def write(response: HttpServerResponse): Unit = {
    result match {
      case FilePyResult(file)=>
        response.sendFile(file.pathAsString)
      case JsonPyResult(js)=>
        response.putHeader("content-type", "application/json")
        response.end(Json.stringify(js))
    }
  }
}

sealed trait PyResult

case class JsonPyResult(jsValue: JsValue) extends PyResult

case class FilePyResult(file: File) extends PyResult

