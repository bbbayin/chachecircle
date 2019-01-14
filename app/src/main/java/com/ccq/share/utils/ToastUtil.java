package com.ccq.share.utils;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.ccq.share.MyApp;

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
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mToast == null) {
                    mToast = Toast.makeText(MyApp.getContext(), msg, Toast.LENGTH_SHORT);
                } else {
                    mToast.setText(msg);
                }
                mToast.show();
            }
        });
    }

}
