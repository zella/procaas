package org.zella.pyaas.config

import better.files.File

import scala.concurrent.duration.Duration

trait PyaasConfig {

  def httpPort: Int

  def httpStartTimeout: Duration

  def pythonInterpreter: String

  def workdir: File

  /**
    *
    * @return Max bytes result file
    */
  def resultTextualLimitBytes: Long

  /**
    *
    * @return Max bytes result text (Can cause OOM)
    */
  def resultBinaryLimitBytes: Long
}
