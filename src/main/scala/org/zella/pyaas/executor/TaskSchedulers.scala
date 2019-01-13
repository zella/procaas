package org.zella.pyaas.executor

import monix.execution.Scheduler

object TaskSchedulers {
  //TODO thread limit
  val io: Scheduler = Scheduler.io()
  val cpu: Scheduler = monix.execution.Scheduler.Implicits.global
}
