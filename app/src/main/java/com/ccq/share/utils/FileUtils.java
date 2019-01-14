package com.ccq.share.utils;

import android.util.Log;

import java.io.File;

/**
 * Created by Administrator on 2017/8/29.
 */

public class FileUtils {
    public static void deleteDirWithFile(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return;
        for (File file : dir.listFiles()) {
            if (file.isFile()){
                Log.w("删除文件:",file.getAbsolutePath());
                file.delete(); // 删除所有文件
            }
            else if (file.isDirectory())
                deleteDirWithFile(file); // 递规的方式删除文件夹
        }
        dir.delete();// 删除目录本身
    }
}
