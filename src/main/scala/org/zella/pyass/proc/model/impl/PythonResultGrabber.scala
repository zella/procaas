package org.zella.pyass.proc.model.impl

import better.files.File
import com.fasterxml.jackson.core.JsonParseException
import org.zella.pyass.net.model.impl.{FilePyResult, JsonPyResult, PyResult}
import org.zella.pyass.proc.GrabResultException
import org.zella.pyass.proc.model.ResultGrabber
import play.api.libs.json.Json

import scala.util.{Failure, Try}

//TODO unit test
class PythonResultGrabber(out: File, asZip: Boolean, asJson: Boolean) extends ResultGrabber[PyResult] {
  override def grab: Try[PyResult] = {
    Try {
      val files = out.list
      (asZip, asJson) match {
        case _ if files.isEmpty =>
          throw new GrabResultException(s"No result files")
        case (true, false) =>
          //TODO rule should be described on higher level
          val result = out.parent / "result.zip"
          result.createIfNotExists(asDirectory = false)
          out.zipTo(result, compressionLevel = 0) //no compression
          FilePyResult(result)
        case (false, false) if files.size == 1 => FilePyResult(out)
        case (false, false) if files.size > 1 =>
          throw new GrabResultException(s"No zipping requested, but here ${files.size}")
        case (false, true) => JsonPyResult(Json.parse(out.contentAsString))
        case (true, true) => throw new GrabResultException(s"Cant grab in zip and json mode simultaneously")
      }
    }.recoverWith {
      case e: JsonParseException => Failure(new GrabResultException("Invalid result json", e))
      case e => Failure(e)
    }

  }
}
