package org.zella.pyass.executor.model

import monix.eval.Task
import org.zella.pyass.net.model.WriteResponse

import scala.concurrent.duration.Duration

trait ExecutorWith[T <: WriteResponse] {
  def executeBlocking(timeout: Duration): Task[T]

  def execute(timeout: Duration): Task[T]
}
