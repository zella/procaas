import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.concurrent.TimeoutException;

public class Test2 {

    public static void main(String[] args) throws InterruptedException, IOException, TimeoutException {
        new ProcessExecutor().command("python3", "/home/dru/git/pyaas/src/test/resources/scripts/chunked_stdout.py")
                .redirectOutput(new LogOutputStream() {
                    @Override
                    protected void processLine(String line) {
                        System.out.println(line);
                    }
                })
                .execute();
    }

}