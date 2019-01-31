package org.zella.procaas.proc.runner.impl

import java.io.{BufferedReader, InputStreamReader, PrintStream}
import java.util.Collections
import java.util.stream.Collectors

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import monix.reactive.subjects.ConcurrentSubject
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.zella.procaas.errors.ProcessException
import org.zella.procaas.executor.TaskSchedulers
import org.zella.procaas.proc.runner.ProcessRunner

import scala.concurrent.TimeoutException
import scala.concurrent.duration.FiniteDuration
import scala.sys.process.{Process, ProcessIO}

class SProcessRunner(io: Scheduler) extends ProcessRunner with LazyLogging {


  def runInternal(timeout: FiniteDuration,
                  cmd: Seq[String],
                  stdIn: Option[String],
                  env: Map[String, String],
                  workDir: Option[File],
                  postProcess: Task[Unit])(implicit scheduler: Scheduler): Observable[String] = Observable.defer {

    val stdout = ConcurrentSubject.replay[String](io)

    val stderr = Collections.synchronizedCollection(new CircularFifoQueue[String](32)) //TODO conf

    //processio spawn threads for out, err, but forget for now
    val processIO = new ProcessIO(in => {
      stdIn.foreach { std =>
        val procOut = new PrintStream(in)
        std.lines.forEach(s => procOut.println(s))
        procOut.close()
      }
    }, out => {
      val br = new BufferedReader(new InputStreamReader(out))
      Stream.continually(br.readLine()).takeWhile(_ != null).foreach(l => {
        stdout.onNext(l + System.lineSeparator())
      })
    }, err => {
      val br = new BufferedReader(new InputStreamReader(err))
      Stream.continually(br.readLine()).takeWhile(_ != null).foreach(l => stderr.add(l))
    })

    Task {
      logger.debug("Starting process...")
      Process(cmd, workDir.map(_.toJava), env.toSeq: _*)
        .run(processIO)
    }.executeOn(scheduler)
      .flatMap(proc =>
        Task {
          logger.debug("Start waiting process...")
          val exitCode = proc.exitValue()
          logger.debug(s"Process completed with $exitCode code")
          stdout.synchronized {
            if (exitCode != 0) {
              stdout.onError(new ProcessException(s"Process '$cmd' has exited with $exitCode. " +
                s"stderr(32 lines): ${stderr.stream().collect(Collectors.joining(System.lineSeparator()))}"))
            }
            else {
              stdout.onComplete()
            }
          }
        }
          .doOnFinish(_ => Task(ensureKilled(proc)))
          .doOnCancel(Task(ensureKilled(proc))))
      .runAsyncAndForget

    def ensureKilled(process: Process): Unit = stdout.synchronized {
      logger.debug("Try killing process...")
      if (process.isAlive) {
        logger.debug("Destroying process...")
        process.destroy()
      }
    }

    stdout.takeUntil(Observable.evalDelayed(timeout, throw new ProcessException("Process timeout", new TimeoutException())))
      .guarantee(Task(logger.debug("postProcess...")).flatMap(_ => postProcess))
  }


  override def runCmd(timeout: FiniteDuration,
                      cmd: Seq[String],
                      stdin: Option[String],
                      env: Map[String, String],
                      workDir: Option[File],
                      postProcess: Task[Unit])(implicit sc: Scheduler): Observable[String] = {
    runInternal(timeout, cmd, stdin, env, workDir, postProcess) //TODO remove middle layer
  }


  override def runInBash(timeout: FiniteDuration,
                         cmd: String,
                         stdin: Option[String],
                         env: Map[String, String],
                         workDir: Option[File],
                         postProcess: Task[Unit])(implicit sc: Scheduler): Observable[String] = {
    runCmd(timeout, Seq("/bin/sh", "-c", cmd), stdin, env, workDir, Task.unit)
  }

}

object SProcessRunner {
  def apply(io: Scheduler = TaskSchedulers.io): SProcessRunner = new SProcessRunner(io)
}
