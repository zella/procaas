package org.zella.pyass.proc


class ProcessException(msg: String, inner: Throwable = null) extends RuntimeException(msg, inner) {}