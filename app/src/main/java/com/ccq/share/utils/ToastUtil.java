package com.ccq.share.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.widget.Toast;

import com.ccq.share.MyApp;

import java.lang.reflect.Field;

/**************************************************
 *
 * 作者：巴银
 * 时间：2018/9/23 0:38
 * 描述：
 * 版本：
 *
 **************************************************/

public class ToastUtil {
    private static Handler handler = new Handler(Looper.getMainLooper());
    private static Toast mToast;

    public static void show(final String msg) {
        showToast(msg);
    }


    private static Field sFieldTN;
    private static Field sFieldTNHandler;

    static {
        try {
            sFieldTN = Toast.class.getDeclaredField("mTN");
            sFieldTN.setAccessible(true);
            sFieldTNHandler = sFieldTN.getType().getDeclaredField("mHandler");
            sFieldTNHandler.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void hook(Toast toast) {
        try {
            Object tn = sFieldTN.get(toast);
            Handler preHandler = (Handler) sFieldTNHandler.get(tn);
            sFieldTNHandler.set(tn, new SafelyHandlerWrapper(preHandler));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class SafelyHandlerWrapper extends Handler {
        private Handler impl;

        SafelyHandlerWrapper(Handler impl) {
            this.impl = impl;
        }

        @Override
        public void dispatchMessage(Message msg) {
            try {
                super.dispatchMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void handleMessage(Message msg) {
            impl.handleMessage(msg);
        }
    }

    public static void showToast(@StringRes int id) {
        showToast(MyApp.getContext().getResources().getString(id), Toast.LENGTH_SHORT);
    }

    public static void showToast(@StringRes int id, int duration) {
        showToast(MyApp.getContext().getResources().getString(id), duration);
    }

    public static void showToast(@NonNull String msg) {
        showToast(msg, Toast.LENGTH_SHORT);
    }

    public static void showToast(@NonNull String msg, int duration) {
        Toast toast = Toast.makeText(MyApp.getContext(), msg, duration);
        // 在调用Toast.show()之前处理:
        hook(toast);
        toast.show();
    }

}
