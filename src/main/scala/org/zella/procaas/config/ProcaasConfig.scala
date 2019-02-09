package org.zella.procaas.config

import better.files.File

import scala.concurrent.duration.FiniteDuration

trait ProcaasConfig {

  def httpPort: Int

  def httpStartTimeout: FiniteDuration

  def workDir: File

  def defaultOutputDirName: String

  def processTimeout: FiniteDuration

  def stdoutBufferWindow:FiniteDuration

  def stdoutBufferSize: Int

}
