package org.zella.procaas.net.model.impl

import java.util.concurrent.atomic.AtomicBoolean

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.scala.core.http.HttpServerResponse
import io.vertx.scala.ext.web.RoutingContext
import monix.eval.Task
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive.{Observable, Observer}
import org.apache.commons.lang3.exception.ExceptionUtils
import org.zella.procaas.net.model.Result


sealed trait OneWayProcResult extends Result

sealed trait TwoWayProcResult extends Result

case class FileProcResult(file: File) extends OneWayProcResult with LazyLogging {

  override def network(ctx: RoutingContext): Task[Unit] = Task {
    logger.debug("Send result file...")
    ctx.response.sendFile(file.pathAsString)
  }

}

case class StdoutProcResult(out: String) extends OneWayProcResult with LazyLogging {

  override def network(ctx: RoutingContext): Task[Unit] = Task {
    logger.debug(out)
    ctx.response.end(out)
  }

}

case class StdoutChunkedProcResult(out: Observable[String]) extends OneWayProcResult with LazyLogging {

  override def network(ctx: RoutingContext): Task[Unit] = Task {
    logger.debug("setChunked")
    ctx.response.setChunked(true)
  }.flatMap { _ =>
    out.
      foreachL(line => {
        logger.debug(line)
        ctx.response.write(line)
      })
  }.doOnFinish(exOpt => Task {
    exOpt match {
      case None => ctx.response.end()
      case Some(e) => //do nothing, response will be written next in error handler
    }
  })
}

case class WebSocketTwoWayResult(in: ConcurrentSubject[String, String], out: Observable[String]) extends TwoWayProcResult with LazyLogging {

  override def network(ctx: RoutingContext): Task[Unit] = Task {
    val isClosed = new AtomicBoolean(false)
    logger.debug("Upgrading to websocket...")
    ctx.request().upgrade()
      .textMessageHandler((s: String) => {
        logger.debug("Receive on ws server:" + s)
        in.onNext(s)
      })
      .closeHandler(_ => {
        isClosed.set(true)
        in.onComplete()
      })
  }.flatMap { socket =>
    out.
      foreachL(l => {
        logger.debug("Sent to client:" + l)
        socket.writeFinalTextFrame(l)
      })
      .doOnFinish(exOpt => Task {
        logger.debug("Closing websocket...")
        exOpt match {
          case None => socket.close()
          case Some(e) => socket.close(500, Some(ExceptionUtils.getStackTrace(e)))
        }
      })
  }
}


