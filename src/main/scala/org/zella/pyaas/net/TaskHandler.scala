package org.zella.pyaas.net

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import io.vertx.core.Handler
import io.vertx.scala.ext.web.RoutingContext
import monix.eval.Task
import monix.execution.Scheduler
import org.apache.commons.lang3.exception.ExceptionUtils
import org.zella.pyaas.executor.model.{Executor, FileUpload, Params}
import org.zella.pyaas.net.model.Result
import play.api.libs.json.Json

class TaskHandler[T <: Params, V <: Result](exec: Executor[T, V]) extends Handler[RoutingContext] with LazyLogging {

  override def handle(ctx: RoutingContext): Unit = {
    //TODO test parse json failure
    import TaskSchedulers._
    exec.prepareInput(
      ctx.fileUploads().map(f => FileUpload(f.fileName(), File(f.uploadedFileName()))).toSeq,
      ctx.request().getFormAttribute("data").map(Json.parse)
        .getOrElse(throw new RuntimeException("Can't get 'data' parameter"))
    ).executeOn(io)
      .flatMap(ep => exec.execute(ep.params, ep.timeout)
        .executeOn(if (ep.isBlocking) io else cpu))
      .flatMap { case (result, workDirOpt) => result.write(ctx.response()).executeOn(io)
        .doOnFinish(_ => Task(workDirOpt.foreach(_.parent.delete()))).executeOn(io)
      }
      .onErrorRecover {
        case e =>
          logger.error("Failure", e)
          ctx.response()
            //TODO fix me
            .setStatusCode(500)
            .end(ExceptionUtils.getStackTrace(e))
      }.runAsyncAndForget(cpu)
  }
}

object TaskSchedulers {
  //TODO thread limit
  val io: Scheduler = Scheduler.io()
  val cpu: Scheduler = monix.execution.Scheduler.Implicits.global
}
