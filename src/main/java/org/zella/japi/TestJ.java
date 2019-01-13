package org.zella.japi;

import io.vertx.scala.core.http.HttpServerResponse;

import java.io.*;

public class TestJ {

    public static void main2(String[] args) throws InterruptedException {
        String cmd = "python3";
        String line1 = "print('i'*10)";
        String line2 = "print('i'*11)";

        try {
            Process p = Runtime.getRuntime().exec(cmd);
            PrintStream ps = new PrintStream(p.getOutputStream());
            ps.println(line1);
            ps.println(line2);
//            ps.flush();
            ps.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            String result = builder.toString();

            p.waitFor();

            System.out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
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




            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            String result = builder.toString();

            p.waitFor();

            System.out.println(result);
            System.out.println("Hello");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void ttt(HttpServerResponse response, BufferedReader reader) throws IOException {
        String line = null;
        while ((line = reader.readLine()) != null) {
            System.out.println(System.currentTimeMillis());
            System.out.println(line);
            response.write(line);
        }
    }

}
