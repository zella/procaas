package org.zella.procaas.proc.model.impl

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.reactive.observers.Subscriber
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive.{Observable, Observer}
import org.zella.procaas.errors.GrabResultException
import org.zella.procaas.executor.{OutputMode, ZipFile}
import org.zella.procaas.net.model.impl.{FileProcResult, StdoutChunkedProcResult, StdoutProcResult, WebSocketTwoWayResult}
import org.zella.procaas.proc.model.ResultGrabber

class FileResultGrabber(out: File, outputMode: OutputMode) extends ResultGrabber[FileProcResult] with LazyLogging {

  override def grab: Task[FileProcResult] = {
    Task {
      logger.debug("Grab file result...")
      val files = out.children
      outputMode match {
        case _ if files.isEmpty =>
          throw new GrabResultException(s"No result files")

        case ZipFile =>
          val result = out.parent / (out.parent.name + ".zip")
          result.createIfNotExists(asDirectory = false)
          out.zipTo(result, 0) //TODO compression level in input
          FileProcResult(result)
        case file if files.size == 1 => FileProcResult(out.list.toSeq.head)
        case file if files.size > 1 =>
          throw new GrabResultException(s"No zipping requested, but here ${files.size} files")
      }
    }

  }
}

class StdoutChunkedResultGrabber(data: Observable[Array[Byte]]) extends ResultGrabber[StdoutChunkedProcResult] {

  override def grab: Task[StdoutChunkedProcResult] = Task(StdoutChunkedProcResult(data))

}

class StdoutResultGrabber(data: Array[Byte]) extends ResultGrabber[StdoutProcResult] {

  override def grab: Task[StdoutProcResult] = Task {
    StdoutProcResult(data)
  }

}

class WebsocketResultGrabber(in: Observer[Array[Byte]], out: Observable[Array[Byte]]) extends ResultGrabber[WebSocketTwoWayResult] {

  override def grab: Task[WebSocketTwoWayResult] = Task(WebSocketTwoWayResult(in, out))

}