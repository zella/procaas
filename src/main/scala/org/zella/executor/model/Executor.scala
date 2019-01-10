package org.zella.pyass.executor.model

import better.files.File
import io.vertx.scala.ext.web.RoutingContext
import monix.eval.Task
import org.zella.pyass.net.model.WriteResponse
import play.api.libs.json.JsValue

import scala.concurrent.duration.Duration
import scala.util.Try

//TODO better class names
trait Params

trait Executor[T <: Params, V <: WriteResponse] {

  def execute(params: T, timeout: Duration): Task[V]

  def resolveInput(files: Seq[File], params: JsValue): Try[ExecutionParams[T]]
}

case class ExecutionParams[T <: Params](params: T, timeout: Duration, isBlocking: Boolean)