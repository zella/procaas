package org.zella.procaas.net.model

import io.vertx.scala.core.http.HttpServerResponse
import monix.eval.Task

trait Result {
  def write(response: HttpServerResponse): Task[Unit]
}
