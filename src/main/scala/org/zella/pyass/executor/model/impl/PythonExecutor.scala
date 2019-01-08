package org.zella.pyass.executor.model.impl

import monix.eval.Task
import org.zella.pyass.executor.model.ExecutorWith
import org.zella.pyass.net.model.impl.PythonResponseWriter

import scala.concurrent.duration.Duration

class PythonExecutor extends ExecutorWith[PythonResponseWriter]{
//https://monix.io/docs/3x/best-practices/blocking.html#if-blocking-use-scalas-blockcontext
  //blocking{}
  override def executeBlocking(timeout: Duration): Task[PythonResponseWriter] = ???

  override def execute(timeout: Duration): Task[PythonResponseWriter] = ???
}
