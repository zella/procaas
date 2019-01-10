package org.zella.pyass.executor.model.impl

import java.nio.file.Files
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

import better.files.File
import monix.eval.Task
import org.apache.commons.text.{StrSubstitutor, StringSubstitutor}
import org.zella.pyass.config.PyaasConfig
import org.zella.pyass.executor.model.{ExecutionParams, Executor, Params}
import org.zella.pyass.net.model.impl.PythonResponseWriter
import org.zella.pyass.proc.ProcessRunner
import org.zella.pyass.proc.model.impl.{AsSingleFile, AsZip, HowToGrab, PythonResultGrabber}
import play.api.libs.json.{Format, JsValue, Json}

import scala.concurrent.duration.Duration
import scala.util.Try
import scala.collection.JavaConverters._

class PythonExecutor(conf: PyaasConfig, pr: ProcessRunner = new ProcessRunner)
  extends Executor[PyScriptParam, PythonResponseWriter] {

  override def execute(param: PyScriptParam, timeout: Duration): Task[PythonResponseWriter] = {
    Task {
      val normalizator = Map(
        "in" -> param.inDir.pathAsString,
        "out" -> param.outDir.pathAsString).asJava
      StringSubstitutor.replace(param.scriptBody, normalizator, "{{", "}}")
    }.flatMap { script =>
      //https://stackoverflow.com/questions/27658478/java-run-shell-command-with-eof
      Task.fromTry(pr.runPython(conf.pythonInterpreter, script, param.outDir.parent, timeout)
        .flatMap(_ =>
          new PythonResultGrabber(
            param.outDir,
            param.howToGrab,
            conf.resultTextualLimitBytes,
            conf.resultBinaryLimitBytes).grab)
        .map(result => new PythonResponseWriter(result)))
        .doOnFinish(_ => Task {
          Files.deleteIfExists(param.outDir.parent.path)
        })
    }
  }

  //form data with files and single json field
  override def resolveInput(files: Seq[File], params: JsValue): Try[ExecutionParams[PyScriptParam]] = {
    Try {
      val input = params.as[PyParamInput]
      val workDir = conf.workdir / "python" / UUID.randomUUID().toString
      val outDir = workDir / "out"
      outDir.createDirectoryIfNotExists(createParents = true)
      val inDir = workDir / "in"
      if (files.nonEmpty) {
        inDir.createDirectoryIfNotExists(createParents = true)
        files.foreach(_.moveTo(inDir))
      }

      val howToGrab = (input.resultAsZip, input.resultAsText) match {
        case (true, false) => AsZip()
        case (false, asText) => AsSingleFile(asText)
        case (true, true) => throw new RuntimeException("Can't grab result as zip and as text")
      }

      ExecutionParams(
        PyScriptParam(input.scriptBody, howToGrab, inDir, outDir),
        Duration(input.timeoutMillis, TimeUnit.MILLISECONDS),
        input.isBlocking)
    }
  }
}

case class PyScriptParam(scriptBody: String, howToGrab: HowToGrab, inDir: File, outDir: File) extends Params

case class PyParamInput(scriptBody: String, resultAsZip: Boolean, resultAsText: Boolean, timeoutMillis: Long, isBlocking: Boolean)

object PyParamInput {
  implicit val jsonFormat: Format[PyParamInput] = Json.format[PyParamInput]
}

