package org.zella.procaas.errors

abstract class ProcaasException(msg: String, inner: Throwable = null) extends RuntimeException(msg, inner) {}

class GrabResultException(msg: String, inner: Throwable = null) extends ProcaasException(msg, inner) {}

class ProcessException(msg: String, inner: Throwable = null) extends ProcaasException(msg, inner) {}

class InputException(msg: String, inner: Throwable = null) extends ProcaasException(msg, inner) {}