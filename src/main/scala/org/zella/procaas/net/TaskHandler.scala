package org.zella.procaas.net

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import io.vertx.core.Handler
import io.vertx.scala.ext.web.RoutingContext
import monix.eval.Task
import org.apache.commons.lang3.exception.ExceptionUtils
import org.zella.procaas.errors.{InputException, ProcaasException}
import org.zella.procaas.executor.TaskSchedulers
import org.zella.procaas.executor.model.{ExecParams, Executor, FileUpload}
import org.zella.procaas.net.model.Result
import play.api.libs.json.Json

class TaskHandler[T <: ExecParams, V <: Result](exec: Executor[T, V]) extends Handler[RoutingContext] with LazyLogging {

  override def handle(ctx: RoutingContext): Unit = {
    implicit val sc = TaskSchedulers.io
    exec.prepareInput(
      ctx.fileUploads().map(f => FileUpload(f.fileName(), File(f.uploadedFileName()))).toSeq,
      ctx.request().getFormAttribute("data").map(Json.parse)
        .getOrElse(throw new InputException("Can't get 'data' parameter"))
    ).executeOn(TaskSchedulers.io)
      .flatMap { case (param, workDirOpt) => exec.execute(param)
        .flatMap(result => result.write(ctx.response()))
        .doOnFinish(_ => Task {
          logger.debug("Deleting workdir...")
          workDirOpt.foreach(_.delete())
        })
      }
      .onErrorRecover {
        case e: ProcaasException =>
          logger.error("Failure", e)
          ctx.response()
            .setStatusCode(400)
            .end(ExceptionUtils.getStackTrace(e))
        case e => ctx.fail(e)
      }.runAsyncAndForget
  }
}

