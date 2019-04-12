package com.ccq.share.http;

import java.io.File;

public class FileUtils {
    public static void deleteFile(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null)
                    for (int i = 0; i < files.length; i++) {
                        deleteFile(files[i]);
                    }
            }
            file.delete();
        }
    }
}
