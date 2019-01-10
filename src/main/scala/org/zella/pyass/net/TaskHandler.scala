package org.zella.pyass.net

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import io.vertx.core.Handler
import io.vertx.scala.ext.web.RoutingContext
import monix.eval.Task
import monix.execution.Scheduler
import org.apache.commons.lang3.exception.ExceptionUtils
import org.zella.pyass.executor.model.{Params, Executor}
import org.zella.pyass.net.model.WriteResponse
import play.api.libs.json.Json

class TaskHandler[T <: Params, V <: WriteResponse](exec: Executor[T, V]) extends Handler[RoutingContext] with LazyLogging {

  override def handle(ctx: RoutingContext): Unit = {
    //TODO test parse json failure
    import TaskSchedulers._

    //TODO make beautiful
    Task.fromTry(exec.resolveInput(
      ctx.fileUploads().map(f => File(f.uploadedFileName())).toSeq,
      ctx.request().getFormAttribute("data").map(Json.parse)
        .getOrElse(throw new RuntimeException("Can't get 'data' parameter"))
    )).executeOn(io)
      .flatMap(ep => exec.execute(ep.params, ep.timeout)
        .executeOn(if (ep.isBlocking) io else cpu))
      .flatMap(v => Task(v.write(ctx.response())))
      .onErrorRecover {
        case e =>
          logger.error("Failure", e)
          ctx.response().end(ExceptionUtils.getStackTrace(e))
      }.runAsyncAndForget(cpu)
  }
}

object TaskSchedulers {
  //TODO thread limit
  val io: Scheduler = Scheduler.io()
  val cpu: Scheduler = monix.execution.Scheduler.Implicits.global
}
