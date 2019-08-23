package org.zella.procaas.proc.runner

import better.files.File
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.observers.Subscriber
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive.{Observable, Observer}

import scala.concurrent.duration.FiniteDuration

trait ProcessRunner {

  def run(timeout: FiniteDuration,
          cmd: Seq[String],
          stdin: Option[Array[Byte]] = None,
          env: Map[String, String] = Map(),
          workDir: Option[File] = None,
          postProcess: Task[Unit] = Task.unit)
         (implicit sc: Scheduler): Observable[Array[Byte]]


  def runInteractive(timeout: FiniteDuration,
                     cmd: Seq[String],
                     env: Map[String, String],
                     workDir: Option[File],
                     postProcess: Task[Unit] = Task.unit)
                    (implicit sc: Scheduler): Task[(Observer[Array[Byte]], Observable[Array[Byte]])]

}
