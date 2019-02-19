package org.zella.procaas.net

import com.typesafe.scalalogging.LazyLogging
import io.vertx.scala.core.{Vertx, http}
import io.vertx.scala.ext.web.Router
import io.vertx.scala.ext.web.handler.BodyHandler
import monix.eval.Task
import org.zella.procaas.config.ProcaasConfig
import org.zella.procaas.executor.model.impl._
import procaas.BuildInfo


class ProcaasHttpServer(conf: ProcaasConfig) extends LazyLogging {

  def startT(): Task[http.HttpServer] = {
    Task.fromFuture {
      val vertx = Vertx.vertx()
      val router = Router.router(vertx)
      router.route.handler(BodyHandler.create()
        .setDeleteUploadedFilesOnEnd(true))
      router.post("/process").handler(new TaskHandler(new OneWayProcessExecutor(conf)))
      router.get("/about").handler(ctx => ctx.response().end(BuildInfo.version))
      router.get("/process_interactive").handler(new TaskHandler(new TwoWayProcessExecutor(conf)))
      vertx
        .createHttpServer()
        .requestHandler(router.accept _)
        .listenFuture(conf.httpPort, "0.0.0.0")
    }
  }
}



