package org.zella.pyaas.net

import better.files.{File, Resource}
import org.mockito.Mockito._
import org.zella.pyaas.config.PyaasConfig
import org.zella.testing.PyaasSpec
import play.api.libs.json.Json
import monix.execution.Scheduler.Implicits.global
import dispatch._
import Defaults._
import io.vertx.scala.core.http.HttpServer
import monix.eval.Task
import org.asynchttpclient.request.body.multipart.{FilePart, StringPart}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, Outcome, fixture}
import org.zella.pyaas.executor.model.impl.PyParamInput

class HttpServerSpec extends fixture.WordSpec with Matchers with MockitoSugar {

  type FixtureParam = HttpServer

  def withFixture(test: OneArgTest): Outcome = {
    val workDir: File = File.newTemporaryDirectory()

    val conf = mock[PyaasConfig]
    when(conf.workdir).thenReturn(workDir)
    when(conf.httpPort).thenReturn(HttpServerSpec.PORT)
    //TODO test requestTimeout

    //TODO test it
    when(conf.resultBinaryLimitBytes).thenReturn(1000)
    //TODO test it
    when(conf.resultTextualLimitBytes).thenReturn(10000)
    when(conf.pythonInterpreter).thenReturn("python3")

    val server = new PyaasHttpServer(conf).startT().runSyncUnsafe()
    try {
      test(server)
    }
    finally {
    //  workDir.delete()
      Task.fromFuture(server.closeFuture()).runSyncUnsafe()
    }
  }

  "HttpServer" should {

    "return file for single file output script" in { s =>

      val svc = url(s"http://localhost:${HttpServerSpec.PORT}/exec_python")
        .addBodyPart(new StringPart("data", Json.toJson(PyParamInput(Resource.getAsString("scripts/single_file_out.py"),
          zipInputMode = false,
          outPutMode = "file",
          5000
        )).toString()))
        .addBodyPart(new FilePart("someName1", File(Resource.getUrl("input/1.txt")).toJava))
        .addBodyPart(new FilePart("someName2", File(Resource.getUrl("input/2.txt")).toJava))
        .setMethod("POST")

      val resp = Task.fromFuture(Http.default(svc)).runSyncUnsafe()

      resp.getStatusCode shouldBe 200
      resp.getContentType shouldBe "text/plain"
      resp.getResponseBody shouldBe "12"

    }


    "return file for multiple file output script with processing thru docker" ignore  { s =>

      val svc = url(s"http://localhost:${HttpServerSpec.PORT}/exec_python")
        .addBodyPart(new StringPart("data", Json.toJson(PyParamInput(Resource.getAsString("scripts/with_docker.py"),
          zipInputMode = false,
          outPutMode = "zip",
          30000
        )).toString()))
        .addBodyPart(new FilePart("someName1", File(Resource.getUrl("input/image1.png")).toJava))
        .addBodyPart(new FilePart("someName2", File(Resource.getUrl("input/image2.png")).toJava))
        .setMethod("POST")

      val resp = Task.fromFuture(Http.default(svc)).runSyncUnsafe()

      resp.getStatusCode shouldBe 200
      resp.getContentType shouldBe "text/plain"
      resp.getResponseBody shouldBe "12"

    }

    "return stdout for stdout script" in { s =>

      val svc = url(s"http://localhost:${HttpServerSpec.PORT}/exec_python")
        .addBodyPart(new StringPart("data", Json.toJson(PyParamInput(Resource.getAsString("scripts/simple_stdout.py"))).toString()))
        .setMethod("POST")

      val resp = Task.fromFuture(Http.default(svc)).runSyncUnsafe()

      resp.getStatusCode shouldBe 200
      resp.getResponseBody shouldBe "2019"
    }

    "return chunked stdout for stdout script in chunked mode" in { s =>

      val svc = url(s"http://localhost:${HttpServerSpec.PORT}/exec_python")
        .addBodyPart(new StringPart("data", Json.toJson(PyParamInput(Resource.getAsString("scripts/chunked_stdout.py"),
          outPutMode = "chunked_stdout")).toString()))
        .setMethod("POST")

      val resp = Task.fromFuture(Http.default(svc)).runSyncUnsafe()

      resp.getStatusCode shouldBe 200
      resp.getResponseBody shouldBe "0123"
    }


    "return valid single file for single file based scripts" in { s => }
    "fail for multiple file for zip based script" in { s => }
    "fail for multiple file for text based script" in { s => }


  }
}

object HttpServerSpec {
  val PORT = 9477
}

