package org.zella.pyaas.executor.model.impl

import java.nio.file.Files
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

import better.files.File
import monix.eval.Task
import org.apache.commons.text.StringSubstitutor
import org.zella.pyaas.config.PyaasConfig
import org.zella.pyaas.executor.model.{ExecutionParams, Executor, FileUpload, Params}
import org.zella.pyaas.net.model.impl.{FilePyResult, PyResult}
import org.zella.pyaas.proc.PyProcessRunner
import org.zella.pyaas.proc.model.impl.{AsFilesGrab, _}
import play.api.libs.json.{Format, JsValue, Json}

import scala.concurrent.duration.Duration
import scala.util.Try
import scala.collection.JavaConverters._

class PythonExecutor(conf: PyaasConfig, pr: PyProcessRunner = new PyProcessRunner)
  extends Executor[PyScriptParam, PyResult] {

  override def execute(param: PyScriptParam, timeout: Duration): Task[(PyResult, Option[WorkDir])] = {
    Task {
      val normalizator = Map(
        "input" -> param.inDir.pathAsString,
        "output" -> param.outDir.pathAsString).asJava
      StringSubstitutor.replace(param.scriptBody, normalizator, "{{", "}}")
    }.flatMap { script =>
      (param.howToGrab match {
        case how: AsFilesGrab =>
          pr.run(conf.pythonInterpreter, script, param.outDir.parent, timeout)
            .flatMap(_ =>
              new FilePyResultGrabber(
                param.outDir,
                how,
                conf.resultTextualLimitBytes,
                conf.resultBinaryLimitBytes).grab)

        case AsStdout(true) =>
          pr.runStdout(conf.pythonInterpreter, script, param.outDir.parent, timeout)
            .flatMap(reader => new StdoutChunkedPyResultGrabber(reader).grab)

        case AsStdout(false) =>
          pr.runStdout(conf.pythonInterpreter, script, param.outDir.parent, timeout)
            .flatMap(reader => new StdoutPyResultGrabber(reader).grab)

      }).map((_, Some(param.workDir)))

    }
  }

  //form data with files and single json field
  override def prepareInput(files: Seq[FileUpload], params: JsValue): Task[ExecutionParams[PyScriptParam]] = {
    Task {
      val input = params.as[PyParamInput]
      val workDir = conf.workdir / "python" / UUID.randomUUID().toString
      val outDir = workDir / "output"
      outDir.createDirectoryIfNotExists(createParents = true)
      val inDir = workDir / "input"
      if (files.nonEmpty) {
        inDir.createDirectoryIfNotExists(createParents = true)
        files.foreach(fu => fu.file.moveTo(inDir / fu.originalName))
      }

      val howToGrab = if (input.resultAsZip) AsZip() else AsSingleFile

      ExecutionParams(
        PyScriptParam(input.scriptBody, howToGrab, inDir, outDir, workDir),
        Duration(input.timeoutMillis, TimeUnit.MILLISECONDS),
        input.isBlocking)
    }
  }
}

case class PyScriptParam(scriptBody: String, howToGrab: HowToGrab, inDir: File, outDir: File, workDir: File) extends Params

case class PyParamInput(scriptBody: String, stdoutMode: Boolean, resultAsZip: Boolean, timeoutMillis: Long, isBlocking: Boolean)

object PyParamInput {
  implicit val jsonFormat: Format[PyParamInput] = Json.format[PyParamInput]
}

