package org.zella.pyaas.proc

import java.io.{BufferedReader, InputStreamReader, PrintStream}
import java.lang.ProcessBuilder.Redirect
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import org.zella.pyaas.errors.ProcessException

import scala.concurrent.duration.{Duration, FiniteDuration}

class PyProcessRunner extends LazyLogging {

  //return started process
  private def start(interpreter: String, scriptContent: String, workDir: File, timeout: Duration): Task[Process] = Task {
    logger.debug("Starting process...")
    val pb = new ProcessBuilder("timeout", timeout.toSeconds.toString, interpreter)
      .redirectError(Redirect.INHERIT)
      .directory(workDir.toJava)
    val process = pb.start()
    val procOut = new PrintStream(process.getOutputStream)
    scriptContent.lines.forEach(s => procOut.println(s))
    procOut.close()
    process
  }

  private def start(interpreter: String, script: File, args: Seq[String], workDir: File, timeout: Duration): Task[Process] = Task {
    logger.debug("Starting process...")
    val pb = new ProcessBuilder(Seq(interpreter, timeout.toSeconds.toString, script.pathAsString) ++ args: _*)
      .redirectError(Redirect.INHERIT)
      .directory(workDir.toJava)
    val process = pb.start()
    val procOut = new PrintStream(process.getOutputStream)
    procOut.close()
    process
  }

  //emits process output in realtime
  private def chunkedOutput(process: Process): Observable[String] = {
    Observable.fromLinesReader(Task(new BufferedReader(new InputStreamReader(process.getInputStream))))
      .doOnError(_ => Task {
        forceStop(process)
      })
      .doOnSubscriptionCancel(Task {
        forceStop(process)
      })
  }

  private def forceStop(process: Process): Unit = {
    try {
      process.destroyForcibly().waitFor()
    } catch {
      case ex: Throwable =>
        throw new ProcessException("Process timeout. Can't destroy", ex)
    }
  }

  private def result(process: Process, timeout: Duration): Task[Int] =
    Task {
      logger.debug("Start waiting process...")
      val completed = process.waitFor(timeout.toMillis, TimeUnit.MILLISECONDS)
      logger.debug(s"Process completed: $completed")
      if (!completed) {
        try {
          process.destroyForcibly().waitFor()
        } catch {
          case ex: Throwable =>
            throw new ProcessException("Process timeout. Can't destroy", ex)
        }
        throw new ProcessException("Process timeout")
      }
      val exitCode = process.exitValue()
      logger.debug(s"Process completed with $exitCode code")
      if (exitCode != 0 && exitCode != 124) throw new ProcessException(s"Incorrect exit code: $exitCode")
      if (exitCode == 124) throw new ProcessException("Process timeout")
      exitCode
    }


  def run(interpreter: String, script: String, workDir: File, timeout: Duration)
         (implicit sc: Scheduler): Observable[String] = {
    runInternal(timeout, None, start(interpreter, script, workDir, timeout))
  }

  def run(interpreter: String, script: File, args: Seq[String], workDir: File, timeout: Duration)
         (implicit sc: Scheduler): Observable[String] = {
    runInternal(timeout, Some(script), start(interpreter, script, args, workDir, timeout))
  }

  private def runInternal(timeout: Duration, script: Option[File], startT: Task[Process])(implicit sc: Scheduler) = {
    val startProc: Task[Process] = startT.memoize
    val readByLine: Observable[String] = Observable.fromTask(startProc).flatMap(process => chunkedOutput(process))
    val waitResult: Task[Int] = startProc.flatMap(process => result(process, timeout).executeOn(sc))
      .doOnCancel(Task(script.foreach(_.delete(true))))
      .doOnFinish(_ => Task(script.foreach(_.delete(true))))

    val done: Observable[String] = Observable(readByLine, Observable.fromTask(waitResult).flatMap(_ => Observable.empty[String])).concat
    done
  }


}

object PyProcessRunner {
  val lineSep = System.getProperty("line.separator")
}

case object Done
