package org.zella.pyaas.proc.runner

import better.files.File
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable

import scala.concurrent.duration.FiniteDuration

trait ProcessRunner {

  def runInBash(timeout: FiniteDuration,
                cmd: String,
                stdin: Option[String],
                workDir: Option[File] = None,
                postProcess: Task[Unit] = Task.unit)
               (implicit sc: Scheduler): Observable[String]


  def runCmd(timeout: FiniteDuration,
             cmd: Seq[String],
             stdin: Option[String],
             workDir: Option[File] = None,
             postProcess: Task[Unit] = Task.unit)
            (implicit sc: Scheduler): Observable[String]

  def runPyInteractive(timeout: FiniteDuration,
                       interpreter: String,
                       script: String,
                       workDir: Option[File] = None)
                      (implicit sc: Scheduler): Observable[String]

  def runPy(timeout: FiniteDuration,
            interpreter: String,
            script: File,
            args: Seq[String],
            workDir: Option[File] = None,
            postProcess: Task[Unit] = Task.unit)
           (implicit sc: Scheduler): Observable[String]
}
