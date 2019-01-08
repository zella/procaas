package org.zella.pyass.net.model

import io.vertx.scala.core.http.HttpServerResponse

trait WriteResponse {
  def write(response: HttpServerResponse)
}
