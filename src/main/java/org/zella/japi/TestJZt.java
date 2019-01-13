package org.zella.japi;

import io.vertx.scala.core.http.HttpServerResponse;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.concurrent.Future;

public class TestJZt {


    public static void mainBackup(String[] args) throws InterruptedException {
        System.out.println("Bye");
        String cmd = "python3";
        String line0 = "from time import sleep";
        String line1 = "print(0, flush=True)";
        String line2 = "sleep(1)";
        String line3 = "print(1, flush=True)";
        String line4 = "sleep(1)";
        String line5 = "print(2, flush=True)";
        String line6 = "sleep(1)";
        String line7 = "print(3, flush=True)";
        String line8 = "sleep(1)";

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.start();
            PrintStream ps = new PrintStream(p.getOutputStream());
            ps.println(line0);
            ps.println(line1);
            ps.println(line2);
            ps.println(line3);
            ps.println(line4);
            ps.println(line5);
            ps.println(line6);
            ps.println(line7);
            ps.println(line8);


//            ps.flush();
            ps.close();


            p.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            String result = builder.toString();


            System.out.println(result);
            System.out.println("Hello");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("Bye");
        String cmd = "python3";
        String line0 = "from time import sleep";
        String line1 = "print(0, flush=True)";
        String line2 = "sleep(1)";
        String line3 = "print(1, flush=True)";
        String line4 = "sleep(1)";
        String line5 = "print(2, flush=True)";
        String line6 = "sleep(1)";
        String line7 = "print(3, flush=True)";
        String line8 = "sleep(1)";

        StartedProcess proc = new ProcessExecutor()
                .command("python3", "/home/dru/git/pyaas/src/test/resources/scripts/chunked_stdout.py")
                .redirectOutput(new LogOutputStream() {
                    @Override
                    protected void processLine(String line) {
                        System.out.println(line);
                    }
                })
                .start();
        Thread.sleep(1000);

        PrintStream ps = new PrintStream(proc.getProcess().getOutputStream());
        ps.println(line0);
        ps.println(line1);
        ps.println(line2);
        ps.println(line3);
        ps.println(line4);
        ps.println(line5);
        ps.println(line6);
        ps.println(line7);
        ps.println(line8);
        ps.close();

        proc.getProcess().waitFor();

        System.out.println("Hello");


    }

}
