package org.zella.procaas.executor

import java.util.UUID
import java.util.concurrent.TimeUnit

import better.files.File
import monix.execution.Scheduler
import org.zella.procaas.config.ProcaasConfig
import org.zella.procaas.errors.InputException
import org.zella.procaas.executor.model.ExecParams
import play.api.libs.json.Json

import scala.concurrent.duration.FiniteDuration
import scala.util.Try


case class CommonProcessParams(cmd: Seq[String],
                               zipInputMode: Boolean,
                               stdin: Option[String],
                               envs: Map[String, String],
                               outPutMode: OutputMode,
                               outputDir: String,
                               timeout: FiniteDuration,
                               scheduler: Scheduler,
                               workDir: File) extends ExecParams

/**
  *
  * Http request body
  *
  * @param cmd
  * @param zipInputMode
  * @param stdin
  * @param envs process env variables
  * @param outPutMode
  * @param outputDir
  * @param timeoutMillis
  * @param computation
  */
case class CommonProcessInput(cmd: Seq[String],
                              zipInputMode: Option[Boolean] = None,
                              stdin: Option[String] = None,
                              envs: Option[Map[String, String]] = None,
                              outPutMode: Option[String] = None,
                              outputDir: Option[String] = None,
                              timeoutMillis: Option[Long] = None,
                              computation: Option[String] = None
                             ) {

  def fillDefaults(config: ProcaasConfig): Try[CommonProcessParams] = Try {

    val uid = UUID.randomUUID().toString
    CommonProcessParams(cmd,
      zipInputMode.getOrElse(false),
      stdin,
      envs.getOrElse(Map.empty),
      outPutMode.getOrElse("stdout") match {
        case "stdout" => Stdout
        case "chunkedStdout" => ChunkedStdout
        case "zip" => ZipFile
        case "file" => SingleFile
        case _ => throw new InputException("Invalid output mode")
      },
      outputDir.getOrElse(config.defaultOutputDirName),
      FiniteDuration(timeoutMillis.getOrElse(config.processTimeout.toMillis), TimeUnit.MILLISECONDS),
      computation.getOrElse("io") match {
        case "io" => TaskSchedulers.io
        case "cpu" => TaskSchedulers.cpu
        case _ => throw new InputException("Invalid computation mode")
      },
      config.workDir / uid
    )
  }
}

object CommonProcessInput {
  implicit val jsonFormat = Json.format[CommonProcessInput]
}


sealed trait OutputMode

case object Stdout extends OutputMode
case object ChunkedStdout extends OutputMode
case object SingleFile extends OutputMode
case object ZipFile extends OutputMode

sealed trait Computation

case object Io extends Computation
case object Cpu extends Computation
