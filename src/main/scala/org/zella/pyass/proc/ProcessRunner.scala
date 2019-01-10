package org.zella.pyass.proc

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration
import scala.util.Try

class ProcessRunner {

  def run(cmd: Seq[String], timeout: Duration): Try[Done.type] = {
    Try {
      val pb = new ProcessBuilder(cmd: _*)
      //        .redirectError(errors.toJava)
      //        .redirectOutput(output.toJava)
      //        .directory(workDir.toJava)

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

}

case object Done
