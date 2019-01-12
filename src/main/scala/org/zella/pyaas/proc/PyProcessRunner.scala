package org.zella.pyaas.proc

import java.io.{BufferedReader, InputStreamReader, PrintStream}
import java.util.concurrent.TimeUnit

import better.files.File
import monix.eval.Task
import monix.reactive.Observable

import scala.concurrent.duration.Duration

class PyProcessRunner {

  @deprecated
  def run(cmd: Seq[String], workDir: File, timeout: Duration): Task[Done.type] = {
    Task {
      val pb = new ProcessBuilder(cmd: _*)
        .directory(workDir.toJava)
      //        .redirectError(errors.toJava)
      //        .redirectOutput(output.toJava)

      val process = pb.start()
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


  //TODO test it
  def runStdout(interpreter: String, script: String, workDir: File, timeout: Duration): Task[BufferedReader] = {
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
      reader
    }
  }

}

object PyProcessRunner {
  val lineSep = System.getProperty("line.separator")
}

case object Done
