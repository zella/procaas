package org.zella.pyass.net.model.impl

import io.vertx.scala.core.http.HttpServerResponse
import org.zella.pyass.net.model.WriteResponse

//TODO here some result in constructor
class PythonResponseWriter extends WriteResponse {

  override def write(response: HttpServerResponse): Unit = ???
}

