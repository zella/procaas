package org.zella.pyaas.proc.model

import monix.eval.Task
import org.zella.pyaas.net.model.Result

/**
  * Grab result from success process run
  *
  * @tparam T result type
  */
trait ResultGrabber[T <: Result] {
  def grab: Task[T]
}
