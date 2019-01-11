package org.zella.pyaas.proc

import java.io.{BufferedReader, IOException, InputStreamReader, PrintStream}
import java.util.concurrent.TimeUnit

import better.files.File

import scala.concurrent.duration.Duration
import scala.util.Try

class ProcessRunner {

  def run(cmd: Seq[String], workDir: File, timeout: Duration): Try[Done.type] = {
    Try {
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

  //we don't use std out, because it can be filled with rubbish(ex some forget to remove "print")
  //TODO test with security check
  def runPython(interpreter: String, script: String, workDir: File, timeout: Duration): Try[Done.type] = {
    Try {
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


}

case object Done
