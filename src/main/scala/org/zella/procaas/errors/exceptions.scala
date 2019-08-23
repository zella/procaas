package org.zella.procaas.errors

abstract class MakakusException(msg: String, inner: Throwable = null) extends RuntimeException(msg, inner) {}

class GrabResultException(msg: String, inner: Throwable = null) extends MakakusException(msg, inner) {}

class InputException(msg: String, inner: Throwable = null) extends MakakusException(msg, inner) {}