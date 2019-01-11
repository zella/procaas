package org.zella.pyaas.proc.model

import org.zella.pyaas.net.model.Result

import scala.util.Try

/**
  * Grab result from success process run
  *
  * @tparam T result type
  */
trait ResultGrabber[T <: Result] {
  def grab: Try[T]
}
