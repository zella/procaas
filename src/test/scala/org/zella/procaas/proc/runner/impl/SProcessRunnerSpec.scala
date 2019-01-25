package org.zella.procaas.proc.runner.impl

import java.util.concurrent.TimeoutException

import better.files.Resource
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import org.zella.procaas.errors.ProcessException

import scala.concurrent.duration._

class SProcessRunnerSpec extends WordSpec with Matchers with MockitoSugar {

  import monix.execution.Scheduler.Implicits.global


  val python = "python3"

  "SProcessRunner" should {

    "return simple stdout on execution" in {

      val source = SProcessRunner().runCmd(
        10 seconds,
        Seq(python, "-c", Resource.getAsString("scripts/simple_stdout.py"))
      )

      val result = source.toListL.runSyncUnsafe()
      result.mkString shouldBe "2019\n"
    }

    "return chunked stdout on execution" in {

      val source = SProcessRunner().runCmd(
        10 seconds,
        Seq(python, "-c", Resource.getAsString("scripts/chunked_stdout.py"))
      )

      val result = source.toListL.runSyncUnsafe()
      result.mkString shouldBe "1\n2\n3\n4\n"
      Thread.sleep(1000)
    }

    "throw ProcessException with TimeoutException on execution on long time process" in {


      val source = SProcessRunner().runCmd(
        2 seconds,
        Seq(python, "-c", Resource.getAsString("scripts/chunked_stdout.py"))
      )

      val thrown = intercept[ProcessException] {
        val result = source.toListL.runSyncUnsafe()
        println(result)
      }
      thrown.getCause shouldBe a[TimeoutException]
    }


    "throw ProcessException with non zero exit code on execution broken process" in {

      val source = SProcessRunner().runCmd(
        10 seconds,
        Seq(python, "-c", Resource.getAsString("scripts/invalid.py"))
      )

      val thrown = intercept[ProcessException] {
        val result = source.toListL.runSyncUnsafe()
        println(result)
      }
      thrown.getMessage.contains("has exited with 1") shouldBe true
    }

    "return empty out on execution noout process" in {

      val source = SProcessRunner().runCmd(
        10 seconds,
        Seq(python, "-c", Resource.getAsString("scripts/valid_noout.py"))
      )

      val result = source.toListL.runSyncUnsafe()
      result.mkString shouldBe ""
    }

    "return argument on execution py with args" in {

      val source = SProcessRunner().runCmd(
        10 seconds,
        Seq(python, "-c", Resource.getAsString("scripts/with_args.py"), "THREE", "FO UR"
        ))


      val result = source.toListL.runSyncUnsafe()
      result.mkString shouldBe "1\n2\nTHREE\nFO UR\n4\n5\n"
    }

    //
    //
    //    "return cmd with stdin" in {
    //      val script: String = Resource.getAsString("scripts/simple_stdout.py")
    //      val source = SProcessRunner().runCmd(10 seconds,
    //        s"""python3 -c "${script}"""", None)
    //
    //      val result = source.toListL.runSyncUnsafe()
    //      result.mkString shouldBe ""
    //      val code =
    //        """
    //          |import sys
    //          |
    //          |print('1')
    //          |print("2")
    //          |print(sys.argv[1])
    //        """.stripMargin
    //      val arg = 3
    //      val cmd = "python3 -c \"$(cat <<'EOF'"
    //      val stdIn: Seq[String] = code.split("\n").toSeq ++ Seq("EOF", ")", "\" " + arg)
    //
    //      val cmd2=s"""python3 -c $code """
    //
    //      import sys.process._
    //      val cmdBash ="""python3 -c $'import sys\nprint(\'1\')\nprint("2")\nprint(sys.argv[1])' 3"""
    //
    //      val cmd3 = Array("/bin/sh", "-c", cmdBash)
    //
    //      val p = Runtime.getRuntime.exec(cmd3)
    //
    //      import java.io.BufferedReader
    //      import java.io.InputStreamReader
    //      p.waitFor()
    //      val lineReader = new BufferedReader(new InputStreamReader(p.getInputStream))
    //      lineReader.lines.forEach(line => println(line))
    //
    //

    //      val source = SProcessRunner().runInBash(10 seconds,
    //        cmd2)
    //
    //      val result = source.toListL.runSyncUnsafe()
    //      println(result)


    //            import sys.process._
    //            def runCommand(cmd: String): (Int, String, String) = {
    //              val stdoutStream = new ByteArrayOutputStream
    //              val stderrStream = new ByteArrayOutputStream
    //              val stdoutWriter = new PrintWriter(stdoutStream)
    //              val stderrWriter = new PrintWriter(stderrStream)
    //              val exitValue = cmd.!(ProcessLogger(stdoutWriter.println, stderrWriter.println))
    //              stdoutWriter.close()
    //              stderrWriter.close()
    //              (exitValue, stdoutStream.toString, stderrStream.toString)
    //            }
    //
    //
    //            val code =
    //              """
    //                |import sys
    //                |
    //                |print('1')
    //                |print("2")
    //                |print(sys.argv[1])
    //              """.stripMargin
    //
    //            val arg = 3
    //
    //            val cmd = ???
    //
    //            val (exitCode, std, err) = runCommand(cmd)

    //      import sys.process._
    //
    //      def runCommand(cmd: String*): (Int, String, String) = {
    //        val stdoutStream = new java.io.ByteArrayOutputStream
    //        val stderrStream = new java.io.ByteArrayOutputStream
    //        val stdoutWriter = new java.io.PrintWriter(stdoutStream)
    //        val stderrWriter = new java.io.PrintWriter(stderrStream)
    //        val exitValue =
    //          cmd.!(ProcessLogger(stdoutWriter.println, stderrWriter.println))
    //        stdoutWriter.close()
    //        stderrWriter.close()
    //        (exitValue, stdoutStream.toString, stderrStream.toString)
    //      }
    //
    //
    //      val code =
    //        """
    //          |import sys
    //          |
    //          |print('1')
    //          |print("2")
    //          |print(sys.argv[1])
    //        """.stripMargin
    //
    //      val arg = 3
    //      println("hello")
    //      println(runCommand("python3", "-c", code, arg.toString))
    //
    //
    //
    //      //
    //      //      println()
    //      //      val j = Json.parse("{}").as[Parsed]
    //      //      val j2 = Json.parse("{\"list\":[]}").as[Parsed]
    //      //
    //      //      println()
    //      //      //      val v= Json.fromJson(j2)
    //      //      //      val v2= Json.fromJson(j2)
    //
    //    }


  }
}
