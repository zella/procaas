package org.zella.pyaas.errors

abstract class PyaasException(msg: String, inner: Throwable = null) extends RuntimeException(msg, inner) {}

class GrabResultException(msg: String, inner: Throwable = null) extends PyaasException(msg, inner) {}

class ProcessException(msg: String, inner: Throwable = null) extends PyaasException(msg, inner) {}

class InputException(msg: String, inner: Throwable = null) extends PyaasException(msg, inner) {}