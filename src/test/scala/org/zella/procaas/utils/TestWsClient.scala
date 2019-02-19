package org.zella.procaas.utils

import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive.{Observable, Observer}
import org.asynchttpclient.BoundRequestBuilder
import org.asynchttpclient.ws.{WebSocket, WebSocketListener, WebSocketUpgradeHandler}

object TestWsClient extends LazyLogging {

  def connect(buildedReq: => BoundRequestBuilder): (ConcurrentSubject[String, String], Observable[String]) = {
    val out = ConcurrentSubject.replay[String]
    val in: ConcurrentSubject[String, String] = ConcurrentSubject.replay[String]

    val b = new WebSocketUpgradeHandler.Builder
    val h = b.addWebSocketListener(new WebSocketListener {
      override def onOpen(websocket: WebSocket): Unit = {
        logger.debug("Handshake opened")
        in
          .doOnComplete(Task({
            logger.debug("Send close frame...")
            websocket.sendCloseFrame()
          }))
          .doOnError(e => Task(websocket.sendCloseFrame(500, e.getMessage.takeRight(64))))
          .foreach(l => {
            logger.debug("Send to server:" + l)
            websocket.sendTextFrame(l)
          })

      }

      override def onClose(websocket: WebSocket, code: Int, reason: String): Unit = {
        code match {
          case c if c == 1000 => out.onComplete() //TODO codes
          case c => out.onError(new RuntimeException(s"Code: $c Reason: $reason"))
        }
      }

      override def onError(e: Throwable): Unit = out.onError(e)

      override def onTextFrame(payload: String, finalFragment: Boolean, rsv: Int): Unit = {
        logger.debug("Receive on ws client:" + payload)
        out.onNext(payload)
      }
    }).build()

    buildedReq.execute(h)

    (in, out)

  }

}
