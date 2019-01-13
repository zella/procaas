package org.zella.pyaas.net

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import io.vertx.core.Handler
import io.vertx.scala.ext.web.RoutingContext
import monix.eval.Task
import org.apache.commons.lang3.exception.ExceptionUtils
import org.zella.pyaas.errors.{InputException, PyaasException}
import org.zella.pyaas.executor.TaskSchedulers
import org.zella.pyaas.executor.model.{Executor, FileUpload, Params}
import org.zella.pyaas.net.model.Result
import play.api.libs.json.Json

class TaskHandler[T <: Params, V <: Result](exec: Executor[T, V]) extends Handler[RoutingContext] with LazyLogging {

  override def handle(ctx: RoutingContext): Unit = {
    //TODO test parse json failure
    exec.prepareInput(
      ctx.fileUploads().map(f => FileUpload(f.fileName(), File(f.uploadedFileName()))).toSeq,
      ctx.request().getFormAttribute("data").map(Json.parse)
        .getOrElse(throw new InputException("Can't get 'data' parameter"))
    ).executeOn(TaskSchedulers.io)
      .flatMap(ep => exec.execute(ep.params, ep.timeout))
      .flatMap { case (result, workDirOpt) => result.write(ctx.response())
        .doOnFinish(_ => Task(workDirOpt.foreach(_.parent.delete())))
      }
      .onErrorRecover {
        case e: PyaasException =>
          logger.error("Failure", e)
          ctx.response()
            .setStatusCode(400)
            .end(ExceptionUtils.getStackTrace(e))
      }.runAsyncAndForget(TaskSchedulers.io)
  }
}

