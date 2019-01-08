package org.zella.pyass.net

import com.typesafe.scalalogging.LazyLogging
import io.vertx.core.Handler
import io.vertx.scala.core.http.HttpServerRequest
import io.vertx.scala.ext.web.RoutingContext
import monix.eval.Task
import org.apache.commons.lang3.exception.ExceptionUtils
import org.zella.pyass.executor.model.ExecutorWith
import org.zella.pyass.net.model.WriteResponse

import scala.concurrent.duration.Duration
import scala.util.Try

class TaskHandler(ex: ExecutorWith[_ <: WriteResponse]) extends Handler[RoutingContext] with LazyLogging {

  override def handle(ctx: RoutingContext): Unit = {
    Task.fromTry(ExecutionParams.fromRequest(ctx.request()))
      .flatMap(ep => if (ep.isBlocking) ex.executeBlocking(ep.timeout) else ex.execute(ep.timeout))
      .flatMap(v => Task(v.write(ctx.response())))
      .onErrorRecover {
        case e =>
          logger.error("Failure", e) //TODO ?
          ctx.response().end(s"${ExceptionUtils.getStackTrace(e)}")
      }
  }
}

case class ExecutionParams(timeout: Duration, isBlocking: Boolean)

object ExecutionParams {
  def fromRequest(req: HttpServerRequest): Try[ExecutionParams] = ???
}