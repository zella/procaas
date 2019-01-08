package org.zella.pyass.config

import scala.concurrent.duration.Duration

trait IConfig {
    def httpPort:Int
    def httpStartTimeout:Duration
}
