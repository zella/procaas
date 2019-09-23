package org.zella.procaas.executor

import java.util.UUID
import java.util.concurrent.TimeUnit

import better.files.File
import monix.execution.Scheduler
import org.zella.procaas.config.ProcaasConfig
import org.zella.procaas.errors.InputException
import org.zella.procaas.executor.model.ExecParams
import play.api.libs.json.{Format, Json}

import scala.concurrent.duration.FiniteDuration
import scala.util.Try


trait BasicProcessParams extends ExecParams {
  def cmd: Seq[String]
  def envs: Map[String, String]
  def timeout: FiniteDuration
  def scheduler: Scheduler
  def workDir: File
}

trait OneWayProcessParams extends BasicProcessParams {
  def stdin: Option[String]
  def zipInputMode: Boolean
  def outPutMode: OutputMode
  def outputDir: String
}

trait TwoWayProcessParams extends BasicProcessParams


case class OneWayProcessParamsImpl(cmd: Seq[String],
                               zipInputMode: Boolean,
                               stdin: Option[String],
                               envs: Map[String, String],
                               outPutMode: OutputMode,
                               outputDir: String,
                               timeout: FiniteDuration,
                               scheduler: Scheduler,
                               workDir: File) extends OneWayProcessParams

case class TwoWayProcessParamsImpl(cmd: Seq[String],
                               zipInputMode: Boolean,
                               envs: Map[String, String],
                               outPutMode: OutputMode,
                               outputDir: String,
                               timeout: FiniteDuration,
                               scheduler: Scheduler,
                               workDir: File) extends TwoWayProcessParams


case class OneWayProcessInput(cmd: Seq[String],
                              zipInputMode: Option[Boolean] = None,
                              stdin: Option[String] = None,
                              envs: Option[Map[String, String]] = None,
                              outputMode: Option[String] = None,
                              outputDir: Option[String] = None,
                              timeoutMillis: Option[Long] = None,
                              computation: Option[String] = None
                             ){
  def fillDefaults(config: ProcaasConfig): Try[OneWayProcessParams] = Try {

    val uid = UUID.randomUUID().toString
    OneWayProcessParamsImpl(cmd,
      zipInputMode.getOrElse(false),
      stdin,
      envs.getOrElse(Map.empty),
      outputMode.getOrElse("stdout") match {
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

case class TwoWayProcessInput(cmd: Seq[String],
                              zipInputMode: Option[Boolean] = None,
                              envs: Option[Map[String, String]] = None,
                              outPutMode: Option[String] = None,
                              outputDir: Option[String] = None,
                              timeoutMillis: Option[Long] = None,
                              computation: Option[String] = None
                             ){
  def fillDefaults(config: ProcaasConfig): Try[TwoWayProcessParams] = Try {

    val uid = UUID.randomUUID().toString
    TwoWayProcessParamsImpl(cmd,
      zipInputMode.getOrElse(false),
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


object TwoWayProcessInput {
  implicit val jsonFormat: Format[TwoWayProcessInput] = Json.format[TwoWayProcessInput]
}
object OneWayProcessInput {
  implicit val jsonFormat: Format[OneWayProcessInput] = Json.format[OneWayProcessInput]
}



sealed trait OutputMode

case object Stdout extends OutputMode
case object ChunkedStdout extends OutputMode
case object SingleFile extends OutputMode
case object ZipFile extends OutputMode

sealed trait Computation

case object Io extends Computation
case object Cpu extends Computation
