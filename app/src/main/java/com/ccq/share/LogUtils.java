package com.ccq.share;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class LogUtils {
    public static String logDir = MyApp.getContext().getFilesDir().getAbsolutePath() + File.separator + "ccq_log.txt";

    public static void write(String content) {
        File file = new File(logDir);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readlog() {
        File file = new File(logDir);
        if (file.exists()) {
            try {
                StringBuilder builder = new StringBuilder();
                FileReader fileReader = new FileReader(file);
                char[] chars = new char[2048];
                int len = 0;
                while ((len = fileReader.read(chars, 0, len)) != -1) {
                    builder.append(new String(chars, 0, len));
                }
                fileReader.close();
                return builder.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return null;
    }
}
