package org.zella.procaas.proc.runner.impl

import java.io.{BufferedReader, InputStreamReader, PrintStream}
import java.util.Collections
import java.util.stream.Collectors

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive.{Observable, Observer}
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
                  postProcess: Task[Unit])(implicit scheduler: Scheduler): Observable[Char] = Observable.defer {

    val stdout = ConcurrentSubject.replay[Char](io)

    val stderr = Collections.synchronizedCollection(new CircularFifoQueue[String](32)) //TODO conf

    val processIO = new ProcessIO(in => {
      stdIn.foreach { std =>
        val procOut = new PrintStream(in)
        procOut.print(std)
        procOut.close()
      }
    }, out => {
      val ir = new InputStreamReader(out)
      Stream.continually(ir.read()).takeWhile(_ != -1).foreach(l => {
        stdout.onNext(l.toChar)
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

  //TODO remove duplicates
  def runInternalInteractive(timeout: FiniteDuration,
                             cmd: Seq[String],
                             env: Map[String, String],
                             workDir: Option[File],
                             postProcess: Task[Unit])(implicit scheduler: Scheduler): Task[(Observer[String], Observable[Char])] = Task {

    val stdout = ConcurrentSubject.replay[Char](io)

    val stderr = Collections.synchronizedCollection(new CircularFifoQueue[String](32)) //TODO conf

    val stdin = ConcurrentSubject.replay[String](io)

    val processIO = new ProcessIO(in => {
      val procOut = new PrintStream(in)
      stdin
        .doOnNext(l => Task(logger.debug("stdin:" + l)))
        .guarantee(Task {
          logger.debug("Closing stdin...")
          procOut.close()
        })
        .foreach(s => {
          procOut.print(s)
          procOut.flush()
        })
    }, out => {
      //TODO buffer Observable[Char] to Observable[String] with window ~30-50ms (24 frames per second)
      val ir = new InputStreamReader(out)
      Stream.continually(ir.read()).takeWhile(_ != -1).foreach(l => {
        stdout.onNext(l.toChar)
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

    val out = stdout.takeUntil(Observable.evalDelayed(timeout, throw new ProcessException("Process timeout", new TimeoutException())))
      .guarantee(Task(logger.debug("postProcess...")).flatMap(_ => postProcess))

    (stdin, out)
  }


  override def runCmd(timeout: FiniteDuration,
                      cmd: Seq[String],
                      stdin: Option[String],
                      env: Map[String, String],
                      workDir: Option[File],
                      postProcess: Task[Unit])(implicit sc: Scheduler): Observable[Char] = {
    runInternal(timeout, cmd, stdin, env, workDir, postProcess) //TODO remove middle layer
  }

  override def runInteractive(timeout: FiniteDuration,
                              cmd: Seq[String],
                              env: Map[String, String],
                              workDir: Option[File],
                              postProcess: Task[Unit])(implicit sc: Scheduler): Task[(Observer[String], Observable[Char])] = {
    runInternalInteractive(timeout, cmd, env, workDir, postProcess) //TODO remove middle layer
  }


  override def runInBash(timeout: FiniteDuration,
                         cmd: String,
                         stdin: Option[String],
                         env: Map[String, String],
                         workDir: Option[File],
                         postProcess: Task[Unit])(implicit sc: Scheduler): Observable[Char] = {
    runCmd(timeout, Seq("/bin/sh", "-c", cmd), stdin, env, workDir, Task.unit)
  }

}

object SProcessRunner {
  def apply(io: Scheduler = TaskSchedulers.io): SProcessRunner = new SProcessRunner(io)
}
