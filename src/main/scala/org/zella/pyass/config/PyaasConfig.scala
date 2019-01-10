package org.zella.pyass.config

import better.files.File

import scala.concurrent.duration.Duration

trait PyaasConfig {

  def httpPort: Int

  def httpStartTimeout: Duration

  def pythonInterpreter: String

  def workdir:File
}
