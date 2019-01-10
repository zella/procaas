package org.zella.pyass.proc.model

import scala.util.Try

/**
  * Grab result from success process run
  *
  * @tparam T result type
  */
trait ResultGrabber[T] {
  def grab: Try[T]
}
