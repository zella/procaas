package org.zella.pyaas.net

import better.files.{File, Resource}
import dispatch._
import io.vertx.scala.core.http.HttpServer
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.asynchttpclient.request.body.multipart.{FilePart, StringPart}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, Outcome, fixture}
import org.zella.pyaas.config.PyaasConfig
import org.zella.pyaas.executor.model.impl.PyParamInput
import play.api.libs.json.Json

class HttpServerSpec extends fixture.WordSpec with Matchers with MockitoSugar {

  type FixtureParam = HttpServer

  def withFixture(test: OneArgTest): Outcome = {
    val workDir: File = File.newTemporaryDirectory()

    val conf = mock[PyaasConfig]
    when(conf.workDir).thenReturn(workDir)
    when(conf.httpPort).thenReturn(HttpServerSpec.PORT)
    //TODO test requestTimeout

    //TODO test it
    when(conf.resultBinaryLimitBytes).thenReturn(100000)
    //TODO test it
    when(conf.resultTextualLimitBytes).thenReturn(1000000)
    when(conf.pythonInterpreter).thenReturn("python3")

    val server = new PyaasHttpServer(conf).startT().runSyncUnsafe()
    try {
      test(server)
    }
    finally {
      workDir.delete()
      Task.fromFuture(server.closeFuture()).runSyncUnsafe()
    }
  }

  "HttpServer" should {

    "return file for single file output script" in { s =>

      val svc = url(s"http://localhost:${HttpServerSpec.PORT}/exec_python")
        .addBodyPart(new StringPart("data", Json.toJson(PyParamInput(Resource.getAsString("scripts/single_file_out.py"),
          args = None,
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
      resp.getResponseBody shouldBe "contentcontent"

    }


    /**
      * Should runs with docker daemon
      */
    "return file for multiple file output script with processing thru docker" ignore { s =>

      val svc = url(s"http://localhost:${HttpServerSpec.PORT}/exec_python")
        .addBodyPart(new StringPart("data", Json.toJson(PyParamInput(Resource.getAsString("scripts/with_docker.py"),
          args = None,
          zipInputMode = false,
          outPutMode = "zip",
          30000
        )).toString()))
        .addBodyPart(new FilePart("someName1", File(Resource.getUrl("input/image1.png")).toJava))
        .addBodyPart(new FilePart("someName2", File(Resource.getUrl("input/image2.png")).toJava))
        .setMethod("POST")

      val resp = Task.fromFuture(Http.default(svc)).runSyncUnsafe()

      resp.getStatusCode shouldBe 200
      resp.getContentType shouldBe "application/zip"
      resp.getHeader("content-length") shouldBe "67339"

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


    "return 400 and exception with for invalid script" in { s =>

      val svc = url(s"http://localhost:${HttpServerSpec.PORT}/exec_python")
        .addBodyPart(new StringPart("data", Json.toJson(PyParamInput(Resource.getAsString("scripts/invalid.py"))).toString()))
        .setMethod("POST")

      val resp = Task.fromFuture(Http.default(svc)).runSyncUnsafe()

      resp.getStatusCode shouldBe 400
      resp.getResponseBody.contains("ProcessException: Incorrect exit code") shouldBe true
    }

    "return 400 and timeout with for script execution time greater than timeout" in { s =>

      val svc = url(s"http://localhost:${HttpServerSpec.PORT}/exec_python")
        .addBodyPart(new StringPart("data", Json.toJson(PyParamInput(Resource.getAsString("scripts/long_time.py"),
          timeoutMillis = 3000)).toString()))
        .setMethod("POST")

      val resp = Task.fromFuture(Http.default(svc)).runSyncUnsafe()

      resp.getStatusCode shouldBe 400
      resp.getResponseBody.contains("ProcessException") shouldBe true
    }


  }
}

object HttpServerSpec {
  val PORT = 9477
}

