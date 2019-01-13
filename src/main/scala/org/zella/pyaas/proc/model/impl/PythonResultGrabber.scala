package org.zella.pyaas.proc.model.impl

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.reactive.Observable
import org.zella.pyaas.net.model.impl.{FilePyResult, StdoutChunkedPyResult, StdoutPyResult}
import org.zella.pyaas.proc.GrabResultException
import org.zella.pyaas.proc.model.ResultGrabber

//TODO unit test
class FilePyResultGrabber(out: File, howToGrab: AsFilesGrab,
                          textualLimitBytes: Long,
                          binaryLimitBytes: Long) extends ResultGrabber[FilePyResult] with LazyLogging{

  override def grab: Task[FilePyResult] = {
    Task {
      logger.debug("Grab file result...")
      val files = out.children
      howToGrab match {
        case _ if files.isEmpty =>
          throw new GrabResultException(s"No result files")
        case AsZip(_) | AsSingleFile if out.list.map(_.toJava.length()).sum > binaryLimitBytes =>
          throw new GrabResultException(s"Binary result limit higher that $binaryLimitBytes bytes")
        case AsZip(compressionLevel) =>
          //TODO rule should be described on higher level
          val result = out.parent / "result.zip"
          result.createIfNotExists(asDirectory = false)
          out.zipTo(result, compressionLevel)
          FilePyResult(result)
        case AsSingleFile if files.size == 1 => FilePyResult(out.list.toSeq.head)
        case AsSingleFile if files.size > 1 =>
          throw new GrabResultException(s"No zipping requested, but here ${files.size} files")
      }
    }

  }
}

class StdoutChunkedPyResultGrabber(out: Observable[String]) extends ResultGrabber[StdoutChunkedPyResult] {

  override def grab: Task[StdoutChunkedPyResult] = Task(StdoutChunkedPyResult(out))

}

class StdoutPyResultGrabber(out: String) extends ResultGrabber[StdoutPyResult] {

  override def grab: Task[StdoutPyResult] = Task {
    StdoutPyResult(out)
  }


}

trait HowToGrab

sealed trait AsFilesGrab extends HowToGrab

sealed trait AsStdoutGrab extends HowToGrab

//no compression
case class AsZip(compressionLevel: Int = 0) extends AsFilesGrab

case object AsSingleFile extends AsFilesGrab

case class AsStdout(chunked: Boolean) extends AsStdoutGrab
