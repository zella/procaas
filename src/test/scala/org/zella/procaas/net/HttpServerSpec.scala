package org.zella.procaas.net

import better.files.{File, Resource}
import dispatch._
import io.vertx.scala.core.http.HttpServer
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.asynchttpclient.request.body.multipart.{FilePart, StringPart}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, Outcome, fixture}
import org.zella.procaas.config.ProcaasConfig
import org.zella.procaas.executor._
import play.api.libs.json.Json

class HttpServerSpec extends fixture.WordSpec with Matchers with MockitoSugar {

  import HttpServerSpec._

  type FixtureParam = HttpServer

  //TODO random port
  def withFixture(test: OneArgTest): Outcome = {
    val workDir: File = File.newTemporaryDirectory()

    val conf = mock[ProcaasConfig]
    when(conf.workDir).thenReturn(workDir)
    when(conf.defaultOutputDirName).thenReturn("output")
    when(conf.httpPort).thenReturn(Port)
    //TODO test requestTimeout

    val server = new ProcaasHttpServer(conf).startT().runSyncUnsafe()
    try {
      test(server)
    }
    finally {
      workDir.delete(swallowIOExceptions = true)
      Task.fromFuture(server.closeFuture()).runSyncUnsafe()
    }
  }


  "HttpServer" should {

    "return file for single file output script" in { s =>

      val svc = url(s"http://localhost:${s.actualPort()}/process")
        .addBodyPart(new StringPart("data", Json.toJson(CommonProcessInput(
          Seq(Python, "-c", Resource.getAsString("scripts/single_file_out.py")),
          outPutMode = Option("file"),
          timeoutMillis = Some(5000)
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

      val svc = url(s"http://localhost:${s.actualPort()}/process")
        .addBodyPart(new StringPart("data", Json.toJson(CommonProcessInput(
          Seq(Python, "-c", Resource.getAsString("scripts/with_docker.py")),
          outPutMode = Some("zip"),
          timeoutMillis = Some(5000)
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

      val svc = url(s"http://localhost:${s.actualPort()}/process")
        .addBodyPart(new StringPart("data", Json.toJson(CommonProcessInput(
          Seq(Python, "-c", Resource.getAsString("scripts/simple_stdout.py")),
          timeoutMillis = Some(5000)
        )).toString()))
        .setMethod("POST")

      val resp = Task.fromFuture(Http.default(svc)).runSyncUnsafe()

      resp.getStatusCode shouldBe 200
      resp.getResponseBody shouldBe "2019\n"
    }


    "return chunked stdout for stdout script in chunked mode" in { s =>

      val svc = url(s"http://localhost:${s.actualPort()}/process")
        .addBodyPart(new StringPart("data", Json.toJson(CommonProcessInput(
          Seq(Python, "-c", Resource.getAsString("scripts/chunked_stdout.py")),
          outPutMode = Some("chunkedStdout"),
          timeoutMillis = Some(5000)
        )).toString()))
        .setMethod("POST")

      val resp = Task.fromFuture(Http.default(svc)).runSyncUnsafe()

      resp.getStatusCode shouldBe 200
      resp.getResponseBody shouldBe "1\n2\n3\n4\n"
    } //TODO test with curl infitity


    "return 400 and exception with for invalid script" in { s =>

      val svc = url(s"http://localhost:${s.actualPort()}/process")
        .addBodyPart(new StringPart("data", Json.toJson(CommonProcessInput(
          Seq(Python, "-c", Resource.getAsString("scripts/invalid.py")),
          timeoutMillis = Some(5000)
        )).toString()))
        .setMethod("POST")


      val resp = Task.fromFuture(Http.default(svc)).runSyncUnsafe()

      resp.getStatusCode shouldBe 400
      resp.getResponseBody.contains("has exited with 1") shouldBe true
    }

    "return 400 and timeout with for script execution time greater than timeout" in { s =>

      val svc = url(s"http://localhost:${s.actualPort()}/process")
        .addBodyPart(new StringPart("data", Json.toJson(CommonProcessInput(
          Seq(Python, "-c", Resource.getAsString("scripts/chunked_stdout.py")),
          timeoutMillis = Some(2000)
        )).toString()))
        .setMethod("POST")

      val resp = Task.fromFuture(Http.default(svc)).runSyncUnsafe()

      resp.getStatusCode shouldBe 400
      resp.getResponseBody.contains("TimeoutException") shouldBe true
    }

    "return 400 and timeout in chunked mode with for script execution time greater than timeout" in { s =>

      val svc = url(s"http://localhost:${s.actualPort()}/process")
        .addBodyPart(new StringPart("data", Json.toJson(CommonProcessInput(
          Seq(Python, "-c", Resource.getAsString("scripts/chunked_stdout.py")),
          outPutMode = Some("chunkedStdout"),
          timeoutMillis = Some(2000)
        )).toString()))
        .setMethod("POST")

      val resp = Task.fromFuture(Http.default(svc)).runSyncUnsafe()
      resp.getStatusCode shouldBe 200 //because chunked mode
      resp.getResponseBody.contains("ProcessException:") shouldBe true
    }


  }
}

//TODO test workdir removed after failure or success

//TODO use ptree htop etc to test processes killing

object HttpServerSpec {
  val Python = "python3"
  val Port = 8888
}

