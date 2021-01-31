package com.ccq.share.utils;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.ccq.share.AutoSendMsgService;

import java.util.List;

/**
 * Created by Administrator on 2017/8/28.
 */

public class PermissionUtils {

    private static final String TAG = "PermissionUtils";

    public static void checkPerssion(Activity activity, int checkCode) {
        if (!hasPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                || !hasPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                || !hasPermission(activity, Manifest.permission.READ_PHONE_STATE)) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_PHONE_STATE},
                    checkCode);
        }
    }

    static boolean hasPermission(Activity activity, String permission) {
        return (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED);
    }


    public static boolean serviceIsRunning(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(Short.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo info : services) {
            String name = AutoSendMsgService.class.getName();
            Log.w(TAG, "服务名称："+name);
            if (info.service.getClassName().equals(name)) {
                Log.w("utils", "WeChatService is running");
                return true;
            }
        }
        Log.w("utils", "WeChatService not running !!!!!");
        return false;
    }
}
