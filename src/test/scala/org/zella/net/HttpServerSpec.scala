package org.zella.pyass.net

import better.files.File
import org.mockito.Mockito._
import org.zella.pyass.config.PyaasConfig
import org.zella.testing.PyaasSpec
import play.api.libs.json.Json

class HttpServerSpec extends PyaasSpec {


  "HttpServer" should {

    "return text for text based script" in { c =>

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
