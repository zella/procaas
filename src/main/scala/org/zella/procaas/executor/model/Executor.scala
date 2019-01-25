package org.zella.procaas.executor.model

import better.files.File
import monix.eval.Task
import org.zella.procaas.net.model.Result
import play.api.libs.json.JsValue

import scala.concurrent.duration.{Duration, FiniteDuration}

trait ExecParams {
  def timeout: FiniteDuration
}

trait Executor[T <: ExecParams, V <: Result] {

  type WorkDir = File

  def execute(params: T): Task[V]

  def prepareInput(files: Seq[FileUpload], params: JsValue): Task[(T, Option[WorkDir])]
}


case class FileUpload(originalName: String, file: File)