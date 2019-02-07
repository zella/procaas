package org.zella.procaas.executor.model.impl

import com.typesafe.scalalogging.LazyLogging
import io.vertx.scala.ext.web.RoutingContext
import monix.eval.Task
import monix.execution.Scheduler
import org.zella.procaas.config.ProcaasConfig
import org.zella.procaas.errors.InputException
import org.zella.procaas.executor._
import org.zella.procaas.executor.model.{Executor, FileUpload}
import org.zella.procaas.net.model.impl.TwoWayProcResult
import org.zella.procaas.proc.model.impl._
import org.zella.procaas.proc.runner.ProcessRunner
import org.zella.procaas.proc.runner.impl.SProcessRunner
import play.api.libs.json.{JsValue, Json}

class TwoWayProcessExecutor(conf: ProcaasConfig, pr: ProcessRunner = SProcessRunner())
  extends Executor[TwoWayProcessParams, TwoWayProcResult] with LazyLogging {

  override def execute(p: TwoWayProcessParams): Task[TwoWayProcResult] = {

    implicit val processScheduler: Scheduler = p.scheduler

    pr.runInteractive(p.timeout, p.cmd, p.envs, Some(p.workDir))
      .flatMap { case (in, out) => new WebsocketResultGrabber(in, out).grab }
  }

  override def prepareInput(ctx: RoutingContext): Task[(TwoWayProcessParams, Option[WorkDir])] =
    Task {
      ctx.request().getParam("data").map(Json.parse)
        .getOrElse(throw new InputException("Can't get 'data' parameter"))
    }.flatMap(params =>
      Task.fromTry(params.as[TwoWayProcessInput].fillDefaults(conf)).flatMap { procDef =>
        Task {
          procDef.workDir.createIfNotExists(asDirectory = true, createParents = true)
          (procDef, Some(procDef.workDir))
        }
      })
}
