package org.zella.procaas.executor.model.impl

import better.files.File
import com.typesafe.scalalogging.LazyLogging
import io.vertx.scala.ext.web.RoutingContext
import monix.eval.Task
import monix.execution.Scheduler
import org.zella.procaas.config.ProcaasConfig
import org.zella.procaas.errors.InputException
import org.zella.procaas.executor._
import org.zella.procaas.executor.model.{Executor, FileUpload}
import org.zella.procaas.net.model.impl.OneWayProcResult
import org.zella.procaas.proc.model.impl._
import org.zella.procaas.proc.runner.ProcessRunner
import org.zella.procaas.proc.runner.impl.SProcessRunner
import play.api.libs.json.Json

class OneWayProcessExecutor(conf: ProcaasConfig, pr: ProcessRunner = SProcessRunner())
  extends Executor[OneWayProcessParams, OneWayProcResult] with LazyLogging {

  override def execute(p: OneWayProcessParams): Task[OneWayProcResult] = {
    def runInternal() = {
      implicit val processScheduler: Scheduler = p.scheduler
      pr.runCmd(p.timeout, p.cmd, p.stdin, p.envs, Some(p.workDir))
        .bufferTimedAndCounted(conf.stdoutBufferWindow, conf.stdoutBufferSize)
        .filter(_.nonEmpty)
        .map(_.mkString)
    }

    p.outPutMode match {
      case ChunkedStdout => new StdoutChunkedResultGrabber(runInternal()).grab
      case Stdout => runInternal()
        .toListL.map(lines => lines.mkString)
        .flatMap(out => new StdoutResultGrabber(out).grab)
      case mode@(ZipFile | SingleFile) =>
        runInternal().completedL.flatMap(_ => new FileResultGrabber(p.workDir / p.outputDir, mode).grab)
    }
  }

  override def prepareInput(ctx: RoutingContext): Task[(OneWayProcessParams, Option[WorkDir])] =
    Task {
      (ctx.fileUploads().map(f => FileUpload(f.fileName(), File(f.uploadedFileName()))).toSeq,
        ctx.request().getFormAttribute("data").map(Json.parse)
          .getOrElse(throw new InputException("Can't get 'data' parameter")))
    }.flatMap { case (files, params) =>
      Task.fromTry(params.as[OneWayProcessInput].fillDefaults(conf)).flatMap { procDef =>
        Task {
          procDef.workDir.createIfNotExists(asDirectory = true, createParents = true)

          if (files.nonEmpty) {
            if (procDef.zipInputMode && files.size > 1 && !files.head.originalName.takeRight(3).equalsIgnoreCase("zip"))
              throw new InputException("Invalid zipInputMode, should be one zip file")

            val moved = files.map(fu => fu.file.moveTo(procDef.workDir / fu.originalName))

            if (procDef.zipInputMode) {
              moved.head.unzipTo(procDef.workDir)
              moved.head.delete()
            }
          }
          (procDef, Some(procDef.workDir))
        }
      }
    }
}