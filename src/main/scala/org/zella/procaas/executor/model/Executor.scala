package org.zella.procaas.executor.model

import better.files.File
import io.vertx.scala.ext.web.RoutingContext
import monix.eval.Task
import org.zella.procaas.net.model.Result

import scala.concurrent.duration.FiniteDuration

trait ExecParams {
  def timeout: FiniteDuration
}

trait Executor[T <: ExecParams, V <: Result] {

  type WorkDir = File

  def execute(params: T): Task[V]

  def prepareInput(ctx: RoutingContext): Task[(T, Option[WorkDir])]
}


case class FileUpload(originalName: String, file: File)