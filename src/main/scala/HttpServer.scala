import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.web.Router
import io.vertx.scala.ext.web.handler.BodyHandler

import scala.concurrent.Future

class HttpServer   {


  def startFuture(): Future[_] = {
    val vertx = Vertx.vertx()
    //Create a router to answer GET-requests to "/hello" with "world"
    val router = Router.router(vertx)
    router.route.handler(BodyHandler.create())
    router
      .get("/hello")
      .handler(_.response().end("world"))
    router.get("/test").handler(ctx => ctx.response().end("test"))

    vertx
      .createHttpServer()
      .requestHandler(router.accept _)
      .listenFuture(8666, "0.0.0.0")
  }
}