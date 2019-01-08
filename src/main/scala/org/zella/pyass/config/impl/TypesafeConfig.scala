package org.zella.pyass.config.impl

import com.typesafe.config.Config
import org.zella.pyass.config.IConfig

import scala.concurrent.duration.Duration

class TypesafeConfig(conf: Config) extends IConfig {
  override val httpPort: Int = conf.getInt("http.port")
  override val httpStartTimeout: Duration = Duration.fromNanos(conf.getDuration("http.startTimeout").toNanos)
}
