package org.zella.pyaas.proc


class ProcessException(msg: String, inner: Throwable = null) extends RuntimeException(msg, inner) {}