package com.ccq.share.work;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.ccq.share.Constants;
import com.ccq.share.MyApp;
import com.ccq.share.home.MainPresenter;
import com.ccq.share.utils.SpUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class ChatTaskManager {
    private static final ChatTaskManager ourInstance = new ChatTaskManager();

    public static ChatTaskManager getInstance() {
        return ourInstance;
    }

    private ChatTaskManager() {
        EventBus.getDefault().register(this);
    }

    private static int taskIndex = 0;
    private static int totalTask = 1;

    public int getCurrentTaskIndex() {
        return taskIndex;
    }

    /**
     * 单个发送到群聊任务完成回调
     */
    @Subscribe
    public synchronized void onSingleTaskFinish(Task task) {
        taskIndex++;
        if (taskIndex >= totalTask) {
            // 彻底完成
            System.out.println("任务全部完成");
            EventBus.getDefault().post(MainPresenter.FINISH);
        } else {
            System.out.println(String.format("当前任务进度：%s/%s", taskIndex, totalTask));
            // 通知下一轮
            SendMsgWorkLine.initWorkList();
            PackageManager packageManager = MyApp.getContext().getPackageManager();
            Intent it = packageManager.getLaunchIntentForPackage(Constants.WECHAT_PACKAGE_NAME);
            MyApp.getContext().startActivity(it);
        }
    }

    public synchronized void initChatTask() {
        String num = (String) SpUtils.get(MyApp.getContext(), Constants.KEY_CHAT_NUMBER, "1");
        if (!TextUtils.isEmpty(num)) {
            try {
                taskIndex = 0;
                totalTask = Integer.parseInt(num);
            } catch (Exception e) {
                taskIndex = 0;
                totalTask = 1;
            }
        }

        System.out.println("群聊数量: "+totalTask);
    }

    public static class Task {

    }
}
