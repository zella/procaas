package org.zella.pyaas.proc

class GrabResultException(msg: String, inner: Throwable = null) extends RuntimeException(msg, inner) {}