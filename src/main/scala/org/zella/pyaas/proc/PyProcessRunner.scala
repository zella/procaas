package org.zella.pyaas.proc

import java.io.{BufferedReader, InputStreamReader, PrintStream}
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

import better.files.File
import monix.eval.Task
import monix.reactive.Observable

import scala.concurrent.duration.Duration

class PyProcessRunner {

  def run(interpreter: String, script: String, workDir: File, timeout: Duration): Task[Done.type] = {
    Task {
      val pb = new ProcessBuilder(interpreter)
        .directory(workDir.toJava)
      val process = pb.start()
      val procOut = new PrintStream(process.getOutputStream)
      script.lines.foreach(procOut.println)
      procOut.close()
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
      Done
    }
  }

  def runStdoutAsync(interpreter: String, script: String, workDir: File, timeout: Duration): Task[Process] = {
    Task {
      val pb = new ProcessBuilder(interpreter)
        .directory(workDir.toJava)
      val process = pb.start()
      val procOut = new PrintStream(process.getOutputStream)
      script.lines.foreach(procOut.println)
      procOut.close()
      process
    }
  }

  //TODO make better

  def runStdoutSync(interpreter: String, script: String, workDir: File, timeout: Duration): Task[String] = {
    Task {
      val pb = new ProcessBuilder(interpreter)
        .directory(workDir.toJava)
      val process = pb.start()
      val procOut = new PrintStream(process.getOutputStream)
      script.lines.foreach(procOut.println)
      procOut.close()
      val reader = new BufferedReader(new InputStreamReader(process.getInputStream))
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
      reader.lines().collect(Collectors.joining())
    }
  }

}

object PyProcessRunner {
  val lineSep = System.getProperty("line.separator")
}

case object Done
