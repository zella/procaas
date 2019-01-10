package org.zella.pyass.executor.model.impl

import better.files.File
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import org.zella.pyass.config.PyaasConfig
import org.zella.testing.PyaasSpec
import play.api.libs.json.Json

class PythonExecutorSpec extends PyaasSpec {


  "PythonExecutor" should {

    "return text for text based script" in { c =>

      val src = new PythonExecutor(c)

      val input = src.resolveInput(Seq.empty,
        Json.toJson(
          PyParamInput("TODO scrip body from resources",
            resultAsZip = false,
            resultAsText = true,
            10000,
            isBlocking = false))).get

//      src.execute(input.params, input.timeout).runSyncUnsafe()

      //TODO test only http server
      "return valid single file for single file based scripts" in { c => }
      "fail for multiple file for zip based script" in { c => }
      "fail for multiple file for text based script" in { c => }

    }

  }

  override def config(workDir: File) = {
    val conf = mock[PyaasConfig]
    when(conf.workdir).thenReturn(workDir)
    when(conf.pythonInterpreter).thenReturn("python3")
    conf
  }
}
