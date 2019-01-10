package org.zella.pyass.executor.model.impl

import java.nio.file.Files
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

import better.files.File
import monix.eval.Task
import org.zella.pyass.config.PyaasConfig
import org.zella.pyass.executor.model.{ExecutionParams, Executor, Params}
import org.zella.pyass.net.model.impl.PythonResponseWriter
import org.zella.pyass.proc.ProcessRunner
import org.zella.pyass.proc.model.impl.PythonResultGrabber
import play.api.libs.json.{Format, JsValue, Json}

import scala.concurrent.duration.Duration
import scala.util.Try

class PythonExecutor(conf: PyaasConfig, pr: ProcessRunner) extends Executor[PyScriptParam, PythonResponseWriter] {

  override def execute(param: PyScriptParam, timeout: Duration): Task[PythonResponseWriter] = {
    //TODO async(Task) trasnformate script to make it executable
    //    param.scriptBody
    //    val map = Map("inpu")
    //https://stackoverflow.com/questions/2043453/executing-python-multi-line-statements-in-the-one-line-command-line
    //https://stackoverflow.com/questions/27658478/java-run-shell-command-with-eof
    val cmd = Seq(conf.pythonInterpreter, "-c", param.scriptBody)
    Task.fromTry(pr.run(cmd, timeout)
      .flatMap(_ => new PythonResultGrabber(param.outDir, param.resultAsZip, param.resultAsJson).grab)
      .map(result => new PythonResponseWriter(result)))
      .doOnFinish(_ => Task {
        Files.deleteIfExists(param.outDir.parent.path)
      })
  }

  //FIXME files should be in multipart form data. If file is big, OOM occurs, so we need use standart vertx file upload
  //form data with files and single json field
  override def resolveInput(files: Seq[File], params: JsValue): Try[ExecutionParams[PyScriptParam]] = {
    Try {
      val input = params.as[PyParamInput]
      val workDir = conf.workdir / "python" / UUID.randomUUID().toString
      val outDir = workDir / "out"
      outDir.createDirectoryIfNotExists(createParents = true)
      val inDir = if (files.nonEmpty) {
        val inDir = workDir / "in"
        inDir.createDirectoryIfNotExists(createParents = true)
        files.foreach(_.moveTo(inDir))
        Some(inDir)
      } else None
      ExecutionParams(
        PyScriptParam(input.scriptBody, input.resultAsZip, input.resultAsJson, inDir, outDir),
        Duration(input.timeoutMillis, TimeUnit.MILLISECONDS),
        input.isBlocking)
    }
  }
}

case class PyScriptParam(scriptBody: String, resultAsZip: Boolean, resultAsJson: Boolean, inDir: Option[File], outDir: File) extends Params

case class PyParamInput(scriptBody: String, resultAsZip: Boolean, resultAsJson: Boolean, timeoutMillis: Long, isBlocking: Boolean)

object PyParamInput {
  implicit val jsonFormat: Format[PyParamInput] = Json.format[PyParamInput]
}

