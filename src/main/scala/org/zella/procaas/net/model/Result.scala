package org.zella.procaas.net.model

import io.vertx.scala.ext.web.RoutingContext
import monix.eval.Task

trait Result {
  def network(ctx: RoutingContext): Task[Unit]
}
