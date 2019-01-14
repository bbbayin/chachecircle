package com.ccq.share.utils;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;

/**
 * Created by Administrator on 2017/9/5.
 */

public class ScreenLockUtils {
    private static ScreenLockUtils mInstance;
    private static PowerManager.WakeLock lock, unLock;
    private static KeyguardManager km;
    private static KeyguardManager.KeyguardLock kl;

    private ScreenLockUtils(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        unLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
        // 得到键盘锁管理器对象
        km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        kl = km.newKeyguardLock("unLock");
    }

    public static ScreenLockUtils getInstance(Context context) {
        synchronized (ScreenLockUtils.class) {
            if (mInstance == null) {
                mInstance = new ScreenLockUtils(context);
            }
        }
        return mInstance;
    }

    public void lockScreen() {
        // 锁屏
        kl.reenableKeyguard();
        // 释放wakeLock，关灯
        if (unLock.isHeld())
            unLock.release();
    }

    public void unLockScreen() {
        // 点亮屏幕
        if (!unLock.isHeld())
            unLock.acquire();
        // 解锁
        kl.disableKeyguard();
    }


}
