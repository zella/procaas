package org.zella.procaas.proc.runner.impl

import java.util.concurrent.TimeUnit

import better.files.File
import com.github.zella.rxprocess2.{IReactiveProcessBuilder, RxProcess}
import com.zaxxer.nuprocess.NuProcessBuilder
import io.reactivex
import io.reactivex.{BackpressureStrategy, FlowableSubscriber}
import io.reactivex.disposables.Disposable
import io.reactivex.subscribers.ResourceSubscriber
import monix.eval.Task
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.observers.Subscriber
import monix.reactive.{Observable, Observer}
import org.reactivestreams
import org.zella.procaas.proc.runner.ProcessRunner

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

class NuProcessBasedRunner extends ProcessRunner {

  override def run(timeout: FiniteDuration,
                   cmd: Seq[String],
                   stdin: Option[Array[Byte]],
                   env: Map[String, String],
                   workDir: Option[File],
                   postProcess: Task[Unit])(
                    implicit sc: Scheduler): Observable[Array[Byte]] = {
    Observable.eval {
      val rb = buildProcess(cmd,env,workDir,stdin)
      val out = rb.asStdOut(timeout.toMillis, TimeUnit.MILLISECONDS)
      Observable.fromReactivePublisher(out.toFlowable(BackpressureStrategy.BUFFER))
    }.flatMap(identity)
  }

  override def runInteractive(timeout: FiniteDuration,
                              cmd: Seq[String],
                              env: Map[String, String],
                              workDir: Option[File],
                              postProcess: Task[Unit])(implicit sc: Scheduler)
  : Task[(Observer[Array[Byte]], Observable[Array[Byte]])] = {
    Task.eval {
      val bi = buildProcess(cmd, env, workDir, None).biDirectional()

      val waitDoneT = Observable.fromReactivePublisher(bi.waitDone(timeout.toMillis, TimeUnit.MILLISECONDS).toFlowable).firstL

      val inO = Observer.fromReactiveSubscriber(bi.stdIn(), () => {})

      val outO = Observable.fromReactivePublisher(bi.stdOutErr().toFlowable(BackpressureStrategy.BUFFER)).map(d => d.data)

      (waitDoneT, inO, outO)
    } flatMap { case (waitDoneT, inO, outO) => Task {
      val cancelable = waitDoneT.runToFuture
      (inO, outO.executeOn(sc).guarantee(Task {
        cancelable.cancel()
      }))
    }
    }
  }


  private def buildProcess(cmd: Seq[String],
                           env: Map[String, String],
                           workDir: Option[File],
                           stdin: Option[Array[Byte]]): IReactiveProcessBuilder[_] = {
    val b = new NuProcessBuilder(cmd.asJava)
    val envs = b.environment()
    env.foreach { case (k, v) => envs.put(k, v) }
    workDir.foreach(path => b.setCwd(path.path))
    RxProcess.reactive(b).withStdin(stdin.getOrElse(Array.empty))
  }
}
