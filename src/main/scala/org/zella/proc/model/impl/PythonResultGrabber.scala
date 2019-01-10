package org.zella.pyass.proc.model.impl

import better.files.File
import com.fasterxml.jackson.core.JsonParseException
import org.zella.pyass.net.model.impl.{FilePyResult,  PyResult}
import org.zella.pyass.proc.GrabResultException
import org.zella.pyass.proc.model.ResultGrabber
import play.api.libs.json.Json

import scala.util.{Failure, Try}

//TODO unit test
class PythonResultGrabber(out: File, howToGrab: HowToGrab,
                          textualLimitBytes: Long,
                          binaryLimitBytes: Long) extends ResultGrabber[PyResult] {

  override def grab: Try[PyResult] = {
    Try {
      val files = out.children
      howToGrab match {
        case _ if files.isEmpty =>
          throw new GrabResultException(s"No result files")
        //        case AsText || AsJson if out.list.map(_.toJava.length()).sum > textualLimitBytes =>
        //          throw new GrabResultException(s"Text result limit higher that $textualLimitBytes bytes")
        case (AsZip(_) | AsSingleFile(_)) if out.list.map(_.toJava.length()).sum > binaryLimitBytes =>
          throw new GrabResultException(s"Binary result limit higher that $binaryLimitBytes bytes")
        case AsZip(compressionLevel) =>
          //TODO rule should be described on higher level
          val result = out.parent / "result.zip"
          result.createIfNotExists(asDirectory = false)
          out.zipTo(result, compressionLevel)
          FilePyResult(result, asText = false)
        case AsSingleFile(asText) if files.size == 1 => FilePyResult(out.list.toSeq.head, asText)
        case AsSingleFile(_) if files.size > 1 =>
          throw new GrabResultException(s"No zipping requested, but here ${files.size} files")
        //        case AsJson if files.size > 1 =>
        //          throw new GrabResultException(s"Json requested, but here ${files.size} files")
        //        case AsJson if files.size == 1 =>
        //          JsonPyResult(Json.parse(out.contentAsString))
        //        case AsText if files.size > 1 =>
        //          throw new GrabResultException(s"Text requested, but here ${files.size} files")
        //        case AsText if files.size == 1 =>
        //          TextPyResult(out.contentAsString)

      }
    }.recoverWith {
      case e: JsonParseException => Failure(new GrabResultException("Invalid result json", e))
      case e => Failure(e)
    }

  }
}

sealed trait HowToGrab

//no compression
case class AsZip(compressionLevel: Int = 0) extends HowToGrab

//case object AsJson extends HowToGrab

case class AsSingleFile(asText: Boolean) extends HowToGrab

//case object AsText extends HowToGrab