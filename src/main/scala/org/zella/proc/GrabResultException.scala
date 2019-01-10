package org.zella.pyass.proc

class GrabResultException(msg: String, inner: Throwable = null) extends RuntimeException(msg, inner) {}