package org.zella.pyaas

import java.io.{BufferedReader, InputStreamReader, PrintStream}
import java.util.concurrent.TimeUnit

import better.files.File
import monix.eval.Task
import monix.reactive.Observable

import scala.concurrent.duration.Duration

object MyMain {

  def main(args: Array[String]): Unit = {

    //return started process
    def start(): Task[Process] = Task {
      val pb = new ProcessBuilder("python3", "/home/dru/git/pyaas/src/test/resources/scripts/chunked_stdout.py")
      pb.start()
    }

    //emits process output in realtime
    def chunkedOutput(process: Process): Observable[String] = {
      val reader = new BufferedReader(new InputStreamReader(process.getInputStream))
      Observable.fromLinesReader(Task(reader))
    }

    def result(process: Process): Task[Int] = Task {
      process.waitFor()
      process.exitValue()
    }

    val startProc: Task[Process] = start()
    val readByLine: Observable[String] = Observable.fromTask(startProc).flatMap(process => chunkedOutput(process))
    val waitResult: Task[Int] = startProc.flatMap(process => result(process))
    val res: Observable[String] = Observable(readByLine, Observable.fromTask(waitResult.map(_.toString))).concat


  }

  def runStdoutAsync(interpreter: String, script: String, workDir: File, timeout: Duration): Task[Process] = {
    Task {
      val pb = new ProcessBuilder(interpreter)
        .directory(workDir.toJava)
      val process = pb.start()
      val procOut = new PrintStream(process.getOutputStream)
      script.lines.foreach(procOut.println)
      procOut.close()
      //      process

      val reader = new BufferedReader(new InputStreamReader(process.getInputStream))
      Observable.fromLinesReader(Task(reader))
      null
    }
  }
}
