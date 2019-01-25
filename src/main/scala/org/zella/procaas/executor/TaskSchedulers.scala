package org.zella.procaas.executor

import monix.execution.Scheduler

object TaskSchedulers {
  val io: Scheduler = Scheduler.io()
  val cpu: Scheduler = monix.execution.Scheduler.Implicits.global
}
