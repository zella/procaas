package org.zella.procaas.executor.model.impl

import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import org.zella.procaas.config.ProcaasConfig
import org.zella.procaas.errors.InputException
import org.zella.procaas.executor._
import org.zella.procaas.executor.model.{Executor, FileUpload}
import org.zella.procaas.net.model.impl.ProcResult
import org.zella.procaas.proc.model.impl._
import org.zella.procaas.proc.runner.ProcessRunner
import org.zella.procaas.proc.runner.impl.SProcessRunner
import play.api.libs.json.JsValue

class CommonProcessExecutor(conf: ProcaasConfig, pr: ProcessRunner = SProcessRunner())
  extends Executor[CommonProcessParams, ProcResult] with LazyLogging {

  override def execute(p: CommonProcessParams): Task[ProcResult] = {
    def runInternal() = {
      implicit val processScheduler = p.scheduler
      pr.runCmd(p.timeout, p.cmd, p.stdin, p.envs, Some(p.workDir))
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

  override def prepareInput(files: Seq[FileUpload], params: JsValue): Task[(CommonProcessParams, Option[WorkDir])] = {
    Task.fromTry(params.as[CommonProcessInput].fillDefaults(conf)).flatMap { procDef =>
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
