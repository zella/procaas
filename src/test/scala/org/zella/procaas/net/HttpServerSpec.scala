package org.zella.procaas.net

import java.io.{BufferedReader, InputStreamReader}

import better.files.{File, Resource}
import com.typesafe.scalalogging.LazyLogging
import dispatch._
import io.vertx.scala.core.http.HttpServer
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import monix.reactive.subjects.ConcurrentSubject
import org.apache.commons.lang3.exception.ExceptionUtils
import org.asynchttpclient.Dsl
import org.asynchttpclient.request.body.multipart.{FilePart, StringPart}
import org.asynchttpclient.ws.{WebSocket, WebSocketListener, WebSocketUpgradeHandler}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, Outcome, fixture}
import org.zella.procaas.config.ProcaasConfig
import org.zella.procaas.executor._
import org.zella.procaas.utils.TestWsClient
import play.api.libs.json.Json

import scala.concurrent.duration._

class HttpServerSpec extends fixture.WordSpec with Matchers with MockitoSugar with LazyLogging {

  import HttpServerSpec._

  type FixtureParam = HttpServer

  //TODO random port
  def withFixture(test: OneArgTest): Outcome = {
    val workDir: File = File.newTemporaryDirectory()

    val conf = mock[ProcaasConfig]
    when(conf.workDir).thenReturn(workDir)
    when(conf.defaultOutputDirName).thenReturn("output")
    when(conf.httpPort).thenReturn(Port)
    when(conf.processTimeout).thenReturn(5 minutes)
    when(conf.stdoutBufferSize).thenReturn(128)
    when(conf.stdoutBufferWindow).thenReturn(10 millis)
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
        .addBodyPart(new StringPart("data", Json.toJson(OneWayProcessInput(
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
        .addBodyPart(new StringPart("data", Json.toJson(OneWayProcessInput(
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
        .addBodyPart(new StringPart("data", Json.toJson(OneWayProcessInput(
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
        .addBodyPart(new StringPart("data", Json.toJson(OneWayProcessInput(
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
        .addBodyPart(new StringPart("data", Json.toJson(OneWayProcessInput(
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
        .addBodyPart(new StringPart("data", Json.toJson(OneWayProcessInput(
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
        .addBodyPart(new StringPart("data", Json.toJson(OneWayProcessInput(
          Seq(Python, "-c", Resource.getAsString("scripts/chunked_stdout.py")),
          outPutMode = Some("chunkedStdout"),
          timeoutMillis = Some(2000)
        )).toString()))
        .setMethod("POST")

      val resp = Task.fromFuture(Http.default(svc)).runSyncUnsafe()
      resp.getStatusCode shouldBe 200 //because chunked mode
      resp.getResponseBody.contains("ProcessException:") shouldBe true
    }


    //TODO No timeout?
    "ws test basic TODO" in { s =>

      val bReq = Dsl.asyncHttpClient()
        .prepareGet(s"ws://localhost:${s.actualPort()}/process_interactive")
        .addQueryParam("data",Json.toJson(TwoWayProcessInput(Seq(Python, "-c", Resource.getAsString("scripts/ws.py"))
        )).toString() )

      val (in, out) = TestWsClient.connect(bReq)

      in.onNext("ONE\n")
      in.onNext("TWO\n")
      in.onNext("THREE\n")
      in.onNext("FOUR")

      val result = out
          .doOnNext(l => Task(logger.info(l)))
        .toListL.runSyncUnsafe().mkString
      println(result)

      result shouldBe "onetwothree"
    }

    "experimetns" ignore {s =>

      val bReq = Dsl.asyncHttpClient()
        .prepareGet(s"ws://localhost:${s.actualPort()}/process_interactive")
        .addQueryParam("data",Json.toJson(TwoWayProcessInput(Seq("/bin/bash")
        )).toString() )

      val (in, out) = TestWsClient.connect(bReq)

      in.onNext("echo test" + System.lineSeparator())
      Thread.sleep(1000)
      in.onNext("uname -r" + System.lineSeparator())
//      in.onNext("python3" + "\n")
//      Thread.sleep(1000)
//      in.onNext("print('hello from python, flush = True')" + System.lineSeparator())
//      in.onNext("echo AAAAAAAAAAAAA" + System.lineSeparator())

      val result = out
        .doOnNext(l => Task(logger.info(l)))
        .toListL.runSyncUnsafe().mkString
      println(result)

      result shouldBe "onetwothree"
    }

    "ws test with closing TODO" in { s =>

    }

    "ws test with py exception TODO" in { s =>

    }

  }
}

//TODO test workdir removed after failure or success

//TODO use ptree htop etc to test processes killing

object HttpServerSpec {
  val Python = "python3"
  val Port = 8888
}

