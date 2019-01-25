package org.zella.procaas.proc.model.impl

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.reactive.Observable
import org.zella.procaas.errors.{GrabResultException, ProcaasException}
import org.zella.procaas.executor.{OutputMode, ZipFile}
import org.zella.procaas.net.model.impl.{FileProcResult, StdoutChunkedProcResult, StdoutProcResult}
import org.zella.procaas.proc.model.ResultGrabber

//TODO unit test
class FileResultGrabber(out: File, outputMode: OutputMode) extends ResultGrabber[FileProcResult] with LazyLogging {

  override def grab: Task[FileProcResult] = {
    Task {
      logger.debug("Grab file result...")
      val files = out.children
      outputMode match {
        case _ if files.isEmpty =>
          throw new GrabResultException(s"No result files")
        //        case zip(_) | file if out.list.map(_.toJava.length()).sum > binaryLimitBytes =>
        //          throw new GrabResultException(s"Binary result limit higher that $binaryLimitBytes bytes")
        case ZipFile =>
          //TODO rule should be described on higher level
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

class StdoutChunkedResultGrabber(out: Observable[String]) extends ResultGrabber[StdoutChunkedProcResult] {

  override def grab: Task[StdoutChunkedProcResult] = Task(StdoutChunkedProcResult(out))

}

class StdoutResultGrabber(out: String) extends ResultGrabber[StdoutProcResult] {

  override def grab: Task[StdoutProcResult] = Task {
    StdoutProcResult(out)
  }


}

//trait HowToGrab
//
//sealed trait AsFilesGrab extends HowToGrab
//
//sealed trait AsStdoutGrab extends HowToGrab
//
////no compression
//case class AsZip(compressionLevel: Int = 0) extends AsFilesGrab
//case object AsSingleFile extends AsFilesGrab
//case class AsStdout(chunked: Boolean) extends AsStdoutGrab
