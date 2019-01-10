import java.io.*;

public class Test {

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
        String cmd = "python3";
        String line1 = "print('i'*10)";
        String line2 = "print('i'*11)";

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.start();
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

}
