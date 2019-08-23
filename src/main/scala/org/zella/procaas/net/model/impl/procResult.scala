package org.zella.procaas.net.model.impl

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.scala.core.http.HttpServerResponse
import io.vertx.scala.core.streams.Pump
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

case class StdoutProcResult(data: Array[Byte]) extends OneWayProcResult with LazyLogging {

  override def network(ctx: RoutingContext): Task[Unit] = Task {
    logger.debug(new String(data))
    ctx.response.end(Buffer.buffer(data))
  }

}

case class StdoutChunkedProcResult(out: Observable[Array[Byte]]) extends OneWayProcResult with LazyLogging {

  override def network(ctx: RoutingContext): Task[Unit] = Task {
    logger.debug("setChunked")
    ctx.response.setChunked(true)
  }.flatMap { _ =>
    out.
      foreachL(data => {
        logger.debug(new String(data))
        ctx.response.write(Buffer.buffer(data))
      })
  }.doOnFinish(exOpt => Task {
    exOpt match {
      case None => ctx.response.end()
      case Some(e) => //do nothing, response will be written next in error handler
    }
  })
}

case class WebSocketTwoWayResult(in: Observer[Array[Byte]], out: Observable[Array[Byte]]) extends TwoWayProcResult with LazyLogging {

  override def network(ctx: RoutingContext): Task[Unit] = Task {
    val isClosed = new AtomicBoolean(false)
    logger.debug("Upgrading to websocket...")
    ctx.request().upgrade()
      .handler(data => {
        logger.debug("From client:" + data.toString())
        in.onNext(data.getBytes)
      })
      .closeHandler(_ => {
        isClosed.set(true)
        in.onComplete()
      })
  }.flatMap { socket =>
    out.
      foreachL(l => {
        logger.debug("Sent to client:" + l)
        //TODO writeQueueFull?
        socket.write(Buffer.buffer(l))
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


