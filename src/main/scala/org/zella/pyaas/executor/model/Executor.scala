package org.zella.pyaas.executor.model

import better.files.File
import monix.eval.Task
import org.zella.pyaas.net.model.Result
import play.api.libs.json.JsValue

import scala.concurrent.duration.Duration
import scala.util.Try

//TODO better class names
trait Params

trait Executor[T <: Params, V <: Result] {

  type WorkDir = File

  def execute(params: T, timeout: Duration): Task[(V, Option[WorkDir])]

  def prepareInput(files: Seq[FileUpload], params: JsValue): Try[ExecutionParams[T]]
}

case class ExecutionParams[T <: Params](params: T, timeout: Duration, isBlocking: Boolean)

case class FileUpload(originalName: String, file: File)