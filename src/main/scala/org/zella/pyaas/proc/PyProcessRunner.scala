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

import scala.concurrent.duration.Duration

class PyProcessRunner extends LazyLogging {

  //return started process
  private def start(interpreter: String, script: String, workDir: File): Task[Process] = Task {
    logger.debug("Starting process...")
    val pb = new ProcessBuilder(interpreter)
      .redirectError(Redirect.INHERIT)
      .directory(workDir.toJava)
    val process = pb.start()
    val procOut = new PrintStream(process.getOutputStream)
    script.lines.forEach(s => procOut.println(s))
    procOut.close()
    process
  }

  //emits process output in realtime
  private def chunkedOutput(process: Process): Observable[String] = {
    Observable.fromLinesReader(Task(new BufferedReader(new InputStreamReader(process.getInputStream))))
      .doOnNext(l => Task(logger.debug(l)))
  }

  private def result(process: Process, timeout: Duration): Task[Int] = Task {
    logger.debug("Start waiting process...")
    val completed = process.waitFor(timeout.toMillis, TimeUnit.MILLISECONDS)
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
    if (exitCode != 0) throw new ProcessException(s"Incorrect exit code: $exitCode")
    exitCode
  }

  def run(interpreter: String, script: String, workDir: File, timeout: Duration)(implicit sc: Scheduler): Observable[String] = {
    val startProc: Task[Process] = start(interpreter, script, workDir).memoize
    val readByLine: Observable[String] = Observable.fromTask(startProc).flatMap(process => chunkedOutput(process))
    val waitResult: Task[Int] = startProc.flatMap(process => result(process, timeout).executeOn(sc))
    val done: Observable[String] = Observable(readByLine, Observable.fromTask(waitResult).flatMap(_ => Observable.empty[String])).concat
    done
  }


}

object PyProcessRunner {
  val lineSep = System.getProperty("line.separator")
}

case object Done
