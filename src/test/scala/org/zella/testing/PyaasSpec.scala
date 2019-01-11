package org.zella.testing

import better.files.File
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, Outcome, fixture}
import org.zella.pyaas.config.PyaasConfig

abstract class PyaasSpec extends fixture.WordSpec with Matchers with MockitoSugar {

  type FixtureParam = PyaasConfig

  def config(workDir: File): FixtureParam

  def withFixture(test: OneArgTest): Outcome = {
    val workDir: File = File.newTemporaryDirectory()
    try {
      test(config(workDir))
    }
    finally {
      workDir.delete()
    }
  }
}