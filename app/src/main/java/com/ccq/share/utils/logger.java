package com.ccq.share.utils;

import static android.util.Log.e;

/**
 * Created by Administrator on 2017/8/27.
 */

public class logger {
    public static void info(String format, String... strings) {
        if (strings.length != 0) {
            System.out.println(String.format(format, (Object[]) strings));
        }
    }

    public static void showLargeLog(String logContent, int showLength, String tag) {
        if (logContent.length() > showLength) {
            String show = logContent.substring(0, showLength);
            e(tag, show);
            /*剩余的字符串如果大于规定显示的长度，截取剩余字符串进行递归，否则打印结果*/
            if ((logContent.length() - showLength) > showLength) {
                String partLog = logContent.substring(showLength, logContent.length());
                showLargeLog(partLog, showLength, tag);
            } else {
                String printLog = logContent.substring(showLength, logContent.length());
                e(tag, printLog);
            }

        } else {
            e(tag, logContent);
        }
    }
}
