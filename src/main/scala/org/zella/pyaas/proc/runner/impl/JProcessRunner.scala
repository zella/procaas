package org.zella.pyaas.proc.runner.impl

import java.io.{BufferedReader, InputStreamReader, PrintStream}
import java.lang.ProcessBuilder.Redirect
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import scala.concurrent.duration._
import better.files.File
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.{Observable, OverflowStrategy}
import monix.reactive.subjects.ConcurrentSubject
import org.zella.pyaas.errors.ProcessException
import org.zella.pyaas.executor.TaskSchedulers
import org.zella.pyaas.proc.runner.ProcessRunner

import scala.concurrent.TimeoutException
import scala.concurrent.duration.{Duration, FiniteDuration}

class JProcessRunner(io: Scheduler) extends ProcessRunner with LazyLogging {

  private def runInternal(timeout: FiniteDuration,
                          cmd: Seq[String],
                          stdIn: Option[String],
                          workDir: Option[File],
                          postProcess: Task[Unit])(implicit sc: Scheduler) = Observable.defer {

    val processFinished = ConcurrentSubject.async[Nothing]

    def start(timeout: Duration, cmd: Seq[String], stdIn: Option[String], workDir: Option[File]): Task[Process] = Task {
      logger.debug("Starting process...")
      val pb =
        workDir.fold(new ProcessBuilder(cmd: _*)) { w => new ProcessBuilder(cmd: _*).directory(w.toJava) }
          .redirectError(Redirect.INHERIT)
      val process = pb.start()
      stdIn.foreach { std =>
        val procOut = new PrintStream(process.getOutputStream)
        std.lines.forEach(s => procOut.println(s))
        procOut.close()
      }
      process
    }

    def readStdout(process: Process): Observable[String] = {
      Observable.fromLinesReader(Task(new BufferedReader(new InputStreamReader(process.getInputStream))))
        .doOnNext(l => Task(logger.debug(s"stdout: $l`")))
    }

    def waitResult(process: Process): Task[Int] =
      Task {
        logger.debug("Start waiting process...")
        val exitCode = process.waitFor()
        logger.debug(s"Process completed with $exitCode code")
        if (exitCode != 0) throw new ProcessException(s"Incorrect exit code: $exitCode")
        exitCode
      }.timeout(timeout)
        .onErrorHandleWith {
          case e: TimeoutException => Task.raiseError(new ProcessException("Process timeout", e))
          case others => Task.raiseError(others)
        }
        .doOnFinish(eOpt => Task {
          ensureKilled(process)
          eOpt match {
            case None => processFinished.onComplete()
            case Some(e) => processFinished.onError(e)
          }
        })
        .doOnCancel(Task({
          ensureKilled(process)
          processFinished.onError(new ProcessException("Process timeout", new TimeoutException()))
        }))

    def ensureKilled(process: Process): Unit = {
      logger.debug("Try killing process...")
      try {
        if (process.isAlive) {
          logger.debug("Destroying process on timeout...")
          process.destroy()
        }
      }
      catch {
        case e: Throwable =>
          process.destroyForcibly()
          logger.error("Can't destroy process", e)
      }
    }


    val startT = start(timeout, cmd, stdIn, workDir).executeOn(sc).memoize
    val readByLineO = Observable.fromTask(startT).flatMap(process => readStdout(process)
      //https://github.com/monix/monix/issues/164
      .executeOn(io))
      .asyncBoundary(OverflowStrategy.BackPressure(256))
    val waitResultT = startT.flatMap(p => waitResult(p)).memoize

    val subj = readByLineO
      .guarantee(Task(logger.debug("postProcess...")).flatMap(_ => postProcess))
      .takeUntil(Observable.fromTask(waitResultT.flatMap(_ => Task.never)))

    Observable(subj, processFinished).concat

  }

  override def runCmd(timeout: FiniteDuration,
                      cmd: Seq[String],
                      stdin: Option[String],
                      workDir: Option[File],
                      postProcess: Task[Unit])(implicit sc: Scheduler): Observable[String] = {
    runInternal(timeout, cmd, stdin, workDir, postProcess)
  }


  override def runPyInteractive(timeout: FiniteDuration,
                                interpreter: String,
                                script: String,
                                workDir: Option[File])(implicit sc: Scheduler): Observable[String] = {
    runInternal(timeout, Seq(interpreter), Some(script), workDir, Task.unit)
    //    runInBash(timeout, interpreter, Some(script), workDir, Task.unit)
  }

  override def runPy(timeout: FiniteDuration,
                     interpreter: String,
                     script: File,
                     args: Seq[String],
                     workDir: Option[File],
                     postProcess: Task[Unit])(implicit sc: Scheduler): Observable[String] = {
    runInternal(timeout, Seq(interpreter, script.pathAsString), None, workDir, postProcess)
    //    runInBash(timeout, s"$interpreter ${script.pathAsString}", None, workDir, Task.unit)
  }

  override def runInBash(timeout: FiniteDuration,
                         cmd: String,
                         stdin: Option[String],
                         workDir: Option[File],
                         postProcess: Task[Unit])(implicit sc: Scheduler): Observable[String] = {

    runInternal(timeout, Seq("/bin/sh", "-c", cmd), stdin, workDir, Task.unit)
  }
}

object JProcessRunner {
  def apply(io: Scheduler = TaskSchedulers.io): JProcessRunner = new JProcessRunner(io)
}
