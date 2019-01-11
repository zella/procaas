package org.zella.pyaas.config.impl

import better.files.File
import com.typesafe.config.Config
import org.zella.pyaas.config.PyaasConfig

import scala.concurrent.duration.Duration

class TypesafeConfig(conf: Config) extends PyaasConfig {

  override val httpPort: Int = conf.getInt("http.port")

  override val httpStartTimeout: Duration = Duration.fromNanos(conf.getDuration("http.startTimeout").toNanos)

  override val pythonInterpreter: String = conf.getString("python.interpreter")

  override val workdir: File = File(conf.getString("workdir"))

  override val resultTextualLimitBytes: Long = conf.getLong("result.textualLimitBytes")

  override val resultBinaryLimitBytes: Long =  conf.getLong("result.binaryLimitBytes")
}
