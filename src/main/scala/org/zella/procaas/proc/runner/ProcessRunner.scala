package org.zella.procaas.proc.runner

import better.files.File
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive.{Observable, Observer}

import scala.concurrent.duration.FiniteDuration

trait ProcessRunner {

  def runInBash(timeout: FiniteDuration,
                cmd: String,
                stdin: Option[String] = None,
                env: Map[String, String] = Map(),
                workDir: Option[File] = None,
                postProcess: Task[Unit] = Task.unit)
               (implicit sc: Scheduler): Observable[Char]


  def runCmd(timeout: FiniteDuration,
             cmd: Seq[String],
             stdin: Option[String] = None,
             env: Map[String, String] = Map(),
             workDir: Option[File] = None,
             postProcess: Task[Unit] = Task.unit)
            (implicit sc: Scheduler): Observable[Char]


  def runInteractive(timeout: FiniteDuration,
                     cmd: Seq[String],
                     env: Map[String, String],
                     workDir: Option[File],
                     postProcess: Task[Unit] = Task.unit)
                    (implicit sc: Scheduler): Task[(Observer[String], Observable[Char])]

}
