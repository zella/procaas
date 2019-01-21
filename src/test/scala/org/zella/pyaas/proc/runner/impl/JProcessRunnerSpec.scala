package org.zella.pyaas.proc.runner.impl

import java.util.concurrent.TimeoutException

import better.files.{File, Resource}
import monix.eval.Task
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import org.zella.pyaas.errors.ProcessException
import org.zella.pyaas.executor.TaskSchedulers

import scala.concurrent.duration._

class JProcessRunnerSpec extends WordSpec with Matchers with MockitoSugar {

  import monix.execution.Scheduler.Implicits.global


  val python = "python3"

  "JProcessRunner" should {

    "return simple stdout on execution" in {

      val source = JProcessRunner().runPy(10 seconds,
        python,
        File(Resource.getUrl("scripts/simple_stdout.py")),
        Seq(),
        None)

      val result = source.toListL.runSyncUnsafe()
      result.mkString shouldBe "2019"
    }

    "return simple stdout on execution in interactive mode" in {

      val source = JProcessRunner().runPyInteractive(10 seconds,
        python,
        Resource.getAsString("scripts/simple_stdout.py"),
        None)

      val result = source.toListL.runSyncUnsafe()
      result.mkString shouldBe "2019"
    }

    "return chunked stdout on execution" in {

      val source = JProcessRunner().runPy(10 seconds,
        python,
        File(Resource.getUrl("scripts/chunked_stdout.py")),
        Seq(),
        None)

      val result = source.toListL.runSyncUnsafe()
      result.mkString shouldBe "1234"
      Thread.sleep(1000)
    }

    "throw ProcessException with TimeoutException on execution on long time process" in {

      val source = JProcessRunner().runPy(2 seconds,
        python,
        File(Resource.getUrl("scripts/chunked_stdout.py")),
        Seq(),
        None)

      val thrown = intercept[ProcessException] {
        val result = source.toListL.runSyncUnsafe()
        println(result)
      }
      thrown.getCause shouldBe a[TimeoutException]
    }


    "throw ProcessException with non zero exit code on execution broken process" in {

      val source = JProcessRunner().runPy(10 seconds,
        python,
        File(Resource.getUrl("scripts/invalid.py")),
        Seq(),
        None)

      val thrown = intercept[ProcessException] {
        val result = source.toListL.runSyncUnsafe()
        println(result)
      }
      thrown.getMessage shouldBe "Incorrect exit code: 1"
    }

    "return empty out on execution noout process" in {

      val source = JProcessRunner().runPy(10 seconds,
        python,
        File(Resource.getUrl("scripts/valid_noout.py")),
        Seq(),
        None)

      val result = source.toListL.runSyncUnsafe()
      result.mkString shouldBe ""
    }

  }
}