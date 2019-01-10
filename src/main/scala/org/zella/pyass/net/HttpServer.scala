package org.zella.pyass.net

import com.typesafe.scalalogging.LazyLogging
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.web.Router
import io.vertx.scala.ext.web.handler.BodyHandler
import org.zella.pyass.config.PyaasConfig
import org.zella.pyass.executor.model.impl.PythonExecutor
import org.zella.pyass.proc.ProcessRunner

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HttpServer(conf: PyaasConfig, pr:ProcessRunner) extends LazyLogging {

  def startFuture(): Future[Int] = {
    val vertx = Vertx.vertx()
    //Create a router to answer GET-requests to "/hello" with "world"
    val router = Router.router(vertx)
    router.route.handler(BodyHandler.create())
    router.get("/hello").handler(_.response().end("world"))
    router.get("/test").handler(ctx => ctx.response().end("test"))
    router.post("/execPython").handler(new TaskHandler(new PythonExecutor(conf, pr)))
    vertx
      .createHttpServer()
      .requestHandler(router.accept _)
      .listenFuture(conf.httpPort, "0.0.0.0")
      .map(_ => conf.httpPort)
  }
}



