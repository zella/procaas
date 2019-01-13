package org.zella.pyaas.executor.model.impl

import java.util.UUID
import java.util.concurrent.TimeUnit

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.Scheduler
import org.apache.commons.text.StringSubstitutor
import org.zella.pyaas.config.PyaasConfig
import org.zella.pyaas.errors.InputException
import org.zella.pyaas.executor.TaskSchedulers
import org.zella.pyaas.executor.model.{ExecutionParams, Executor, FileUpload, Params}
import org.zella.pyaas.net.model.impl.PyResult
import org.zella.pyaas.proc.PyProcessRunner
import org.zella.pyaas.proc.model.impl.{AsFilesGrab, _}
import play.api.libs.json.{Format, JsValue, Json}

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration

class PythonExecutor(conf: PyaasConfig, pr: PyProcessRunner = new PyProcessRunner)
  extends Executor[PyScriptParam, PyResult] with LazyLogging {

  override def execute(param: PyScriptParam, timeout: Duration): Task[(PyResult, Option[WorkDir])] = {
    Task {
      logger.debug("Script normalisation...")
      val normalizator = Map(
        "input" -> param.inDir.pathAsString,
        "output" -> param.outDir.pathAsString).asJava
      StringSubstitutor.replace(param.scriptBody, normalizator, "{{", "}}")
    }.flatMap { script =>
      implicit val processScheduler = param.scheduler
      (param.howToGrab match {
        case how: AsFilesGrab =>
          pr.run(conf.pythonInterpreter, script, param.outDir.parent, timeout)
            .completedL
            .flatMap(_ =>
              new FilePyResultGrabber(
                param.outDir,
                how,
                conf.resultTextualLimitBytes,
                conf.resultBinaryLimitBytes).grab)

        case AsStdout(true) =>
          new StdoutChunkedPyResultGrabber(
            pr.run(conf.pythonInterpreter, script, param.outDir.parent, timeout)
          ).grab

        case AsStdout(false) =>
          pr.run(conf.pythonInterpreter, script, param.outDir.parent, timeout)
            .toListL.map(lines => lines.mkString)
            .flatMap(out => new StdoutPyResultGrabber(out).grab)

      }).map((_, Some(param.workDir)))
    }
  }

  //form data with files and single json field
  override def prepareInput(files: Seq[FileUpload], params: JsValue): Task[ExecutionParams[PyScriptParam]] = {
    Task {
      logger.debug("Preparing input...")
      val input = params.as[PyParamInput]
      val workDir = conf.workdir / "python" / UUID.randomUUID().toString
      val outDir = workDir / "output"
      outDir.createDirectoryIfNotExists(createParents = true)
      val inDir = workDir / "input"
      if (files.nonEmpty) {
        if (input.zipInputMode && files.size > 1 && !files.head.originalName.takeRight(3).equalsIgnoreCase("zip"))
          throw new InputException("Invalid zipInputMode, should be one zip file")

        inDir.createDirectoryIfNotExists(createParents = true)
        val moved = files.map(fu => fu.file.moveTo(inDir / fu.originalName))

        if (input.zipInputMode) {
          moved.head.unzipTo(inDir)
          moved.head.delete()
        }
      }

      val howToGrab = input.outPutMode match {
        case "stdout" => AsStdout(chunked = false)
        case "chunked_stdout" => AsStdout(chunked = true)
        case "zip" => AsZip()
        case "file" => AsSingleFile
      }


      ExecutionParams(
        PyScriptParam(input.scriptBody,
          howToGrab,
          inDir,
          outDir,
          workDir,
          if (input.isBlocking) TaskSchedulers.io else TaskSchedulers.cpu),
        Duration(input.timeoutMillis, TimeUnit.MILLISECONDS),
        input.isBlocking)
    }
  }
}

case class PyScriptParam(scriptBody: String, howToGrab: HowToGrab, inDir: File, outDir: File, workDir: File, scheduler: Scheduler) extends Params

case class PyParamInput(scriptBody: String,
                        zipInputMode: Boolean = false,
                        outPutMode: String = "stdout", //file, zip stdout_chunked //TODO enum
                        timeoutMillis: Long = 60 * 1000,
                        isBlocking: Boolean = false)

object PyParamInput {
  implicit val jsonFormat: Format[PyParamInput] = Json.using[Json.WithDefaultValues].format[PyParamInput]
}

