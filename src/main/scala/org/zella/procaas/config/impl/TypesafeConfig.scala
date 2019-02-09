package org.zella.procaas.config.impl

import java.util.concurrent.TimeUnit

import better.files.File
import com.typesafe.config.Config
import org.zella.procaas.config.ProcaasConfig

import scala.concurrent.duration.{Duration, FiniteDuration}

class TypesafeConfig(conf: Config) extends ProcaasConfig {

  override val httpPort: Int = conf.getInt("http.port")

  override val httpStartTimeout: FiniteDuration = FiniteDuration(conf.getDuration("http.startTimeout").toMillis, TimeUnit.MILLISECONDS)

  override val workDir: File = File(conf.getString("process.workdir"))

  override val defaultOutputDirName: String = conf.getString("process.defaultOutputDirName")

  override val processTimeout: FiniteDuration = FiniteDuration(conf.getDuration("process.timeout").toMillis, TimeUnit.MILLISECONDS)

  override val stdoutBufferWindow: FiniteDuration =  FiniteDuration(conf.getDuration("process.stdout.bufferWindow").toMillis, TimeUnit.MILLISECONDS)

  override val stdoutBufferSize: Int = conf.getInt("process.stdout.buffer")
}
