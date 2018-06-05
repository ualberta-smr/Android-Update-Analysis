package ca.ualberta.mehran.androidevolution;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;


public class Utils {

    public static String runSystemCommand(String dir, boolean verbose, String... commands) {
        StringBuilder builder = new StringBuilder();
        try {
            if (verbose) {
                for (String command : commands) {
                    System.out.print(command + " ");
                }
                System.out.println();
            }
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(commands, null, new File(dir));

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            String s = null;
            while ((s = stdInput.readLine()) != null) {
                builder.append(s);
                builder.append("\n");
                if (verbose) log(s);
            }

            while ((s = stdError.readLine()) != null) {
                builder.append(s);
                builder.append("\n");
                if (verbose) log(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    public static void log(String message) {
        String timeStamp = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z").format(new Date());
        System.out.println(timeStamp + " " + message);
    }

    public static List<String> readFile(File file) {
        try {
            List<String> lines = new ArrayList<>();
            Scanner input = new Scanner(file);
            while (input.hasNextLine()) {
                lines.add(input.nextLine());
            }
            return lines;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> readFile(File file, int lineStart, int lineEnd) {
        List<String> lines = readFile(file);
        try {
            return lines.subList(lineStart - 1, lineEnd);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return lines;
        }
    }

    public static void writeToFile(String path, String content) {
        writeToFile(new File(path), content);
    }


    public static void writeToFile(File file, String content) {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            PrintWriter writer = new PrintWriter(file);
            writer.print(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
