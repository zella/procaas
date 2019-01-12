package org.zella.pyaas.net

import com.typesafe.scalalogging.LazyLogging
import io.vertx.scala.core.{Vertx, http}
import io.vertx.scala.ext.web.Router
import io.vertx.scala.ext.web.handler.BodyHandler
import monix.eval.Task
import org.zella.pyaas.config.PyaasConfig
import org.zella.pyaas.executor.model.impl.PythonExecutor

class PyaasHttpServer(conf: PyaasConfig) extends LazyLogging {

  def startT(): Task[http.HttpServer] = {
    Task.fromFuture {
      val vertx = Vertx.vertx()
      //Create a router to answer GET-requests to "/hello" with "world"
      val router = Router.router(vertx)
      router.route.handler(BodyHandler.create())
      router.post("/exec_python").handler(new TaskHandler(new PythonExecutor(conf)))
      vertx
        .createHttpServer()
        .requestHandler(router.accept _)
        .listenFuture(conf.httpPort, "0.0.0.0")
    }
  }
}



