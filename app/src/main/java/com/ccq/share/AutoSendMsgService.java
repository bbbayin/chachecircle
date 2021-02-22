package com.ccq.share;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.ccq.share.activity.MainActivity;
import com.ccq.share.home.MainPresenter;
import com.ccq.share.utils.ScreenLockUtils;
import com.ccq.share.utils.ToastUtil;
import com.ccq.share.utils.WechatTempContent;
import com.ccq.share.work.SendMsgWorkLine;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * 自动发送群消息
 * bayin
 * 2021/1/31
 */
public class AutoSendMsgService extends AccessibilityService {
    private String TAG = "[AutoSendMsgService]";
    private ScreenLockUtils instance;
    private WeakReference<AutoSendMsgService> weakReference = new WeakReference<AutoSendMsgService>(this);
    private AccessibilityNodeInfo accessibilityNodeInfo;

    private static final int OPEN_WECHAT = 654;
    public static final int BACK = 333;
    private final static int CHANGE_ACTIVITY = 456;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case BACK:
                    back();
                    break;
                case CHANGE_ACTIVITY:
                    Intent intent = new Intent(weakReference.get(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    handler.sendEmptyMessageDelayed(OPEN_WECHAT, 500);
                    break;
                case OPEN_WECHAT:
                    PackageManager packageManager = getBaseContext().getPackageManager();
                    Intent it = packageManager.getLaunchIntentForPackage(Constants.WECHAT_PACKAGE_NAME);
                    startActivity(it);
                    break;
            }
        }
    };

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        Log.d(TAG, "接收事件===className: " + className);
        SendMsgWorkLine.WorkNode action = SendMsgWorkLine.getNextNode();

        // 窗口事件

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            try {
                Thread.sleep(1000);
                accessibilityNodeInfo = getRootInActiveWindow();
                int time = 3;
                while (time > 0 && accessibilityNodeInfo == null) {
                    time--;
                    Thread.sleep(1000);
                    accessibilityNodeInfo = getRootInActiveWindow();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 判断节点是否为空
            if (accessibilityNodeInfo == null) {
                handler.sendEmptyMessageDelayed(CHANGE_ACTIVITY, 600);
                return;
            }
        }

        if (action != null) {
            Log.d(TAG, "当前任务：" + action.work);
            ToastUtil.show(action.work);
            switch (action.code) {
//                case SendMsgWorkLine.NODE_CLICK_WECHAT_TAB:
//                    clickWechatTab();
//                    break;
                case SendMsgWorkLine.NODE_CLICK_CHAT_ITEM:// 聊天item
                    findNodeByIdClick(ViewIds.ITEM_ID_1, 500);
                    break;
                case SendMsgWorkLine.NODE_PASTE:// 粘贴文字
                    if (isRootNodeNotNull("粘贴文字")) {
                        pasteContent(accessibilityNodeInfo);
                        sendWeChat();
                    }
                    break;
                case SendMsgWorkLine.NODE_CLICK_ADD_BTN:// 加号
                    findNodeByIdClick("com.tencent.mm:id/au0", 500);
                    break;
                case SendMsgWorkLine.NODE_OPEN_ALBUM:// 相册
                    findNodeByIdClick("com.tencent.mm:id/rr", 500);
                    break;
                case SendMsgWorkLine.NODE_SELECT_PICS:// 选择照片
                    if (!isChoosing)
                        choosePicture();
                    break;
                case SendMsgWorkLine.RETURN:// 返回
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    SendMsgWorkLine.forward();
                    notifyNextTask();
                    break;
            }
        }
    }

    private void clickWechatTab() {
        if (isRootNodeNotNull("点击微信")) {
//            findNodeByIdClick("com.tencent.mm:id/dtf",500);
            List<AccessibilityNodeInfo> findList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("微信");
            if (findList != null && findList.size() > 0) {
                AccessibilityNodeInfo findBtn = findList.get(0);
                if (findBtn != null && findBtn.getParent() != null) {
                    findBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    findBtn.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    SendMsgWorkLine.forward();
                } else {
                    step("微信tab");
                }
            } else {
                step("微信tab");
            }
        }
    }


    volatile boolean isChoosing = false;

    /**
     * 选择发送的图片
     */
    private synchronized void choosePicture() {
        isChoosing = true;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                List<AccessibilityNodeInfo> accessibilityNodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/fbe");
                if (accessibilityNodeInfoList == null || accessibilityNodeInfoList.isEmpty()) {
                    step("选择图片");
                    return;
                }
                Log.d(TAG, "图片数量"+SendMsgWorkLine.size);
                Log.d(TAG, "View数量"+accessibilityNodeInfoList.size());
                for (int i = 0; i < SendMsgWorkLine.size; i++) {
                    // 点击下载好的图片
                    accessibilityNodeInfoList.get(i).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }

                // 发送图片
                findNodeByIdClick(ViewIds.BTN_SEND_PICS, 1500);
//                List<AccessibilityNodeInfo> finishList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("发送(" + SendMsgWorkLine.size + "/9)");//点击确定
//                performClickBtn(finishList);
                // 返回
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        notifyNextTask();
                    }
                }, 2000);
            }
        }, 500);
    }


    private void performClickBtn(List<AccessibilityNodeInfo> infos) {
        if (infos != null && infos.size() != 0) {
            for (int i = 0; i < infos.size(); i++) {
                AccessibilityNodeInfo accessibilityNodeInfo = infos.get(i);
                if (accessibilityNodeInfo != null) {
                    accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    SendMsgWorkLine.forward();
                    return;
                }
            }
        }
    }

    private void step(String desc) {
        ToastUtil.show("异常，跳过本次分享" + "[" + desc + "]");
        Log.d(TAG, "异常，跳过本次分享" + "[" + desc + "]");
        int time = 3;
        SendMsgWorkLine.getWorkList().clear();
        while (time >= 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            performGlobalAction(GLOBAL_ACTION_BACK);
            time--;
        }
        notifyNextTask();
    }

    private void findNodeByIdClick(final String id, long delay) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                List<AccessibilityNodeInfo> nodes = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(id);
                if (nodes != null && !nodes.isEmpty()) {
                    final AccessibilityNodeInfo node = nodes.get(0);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            if (node.getParent() != null) {
                                node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            }
                            SendMsgWorkLine.forward();
                            isChoosing = false;
                        }
                    }, 100);
                }
            }
        }, delay);
    }

    /**
     * 分享完成，通知下一个任务
     */
    private void notifyNextTask() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (weakReference.get() != null) {
//                    instance.lockScreen();
                    //发送消息，下载下一个
                    EventBus.getDefault().post(MainPresenter.FINISH);
                }
            }
        });
    }

    /**
     * 获取不为空的node
     *
     * @param tag
     * @return
     */
    private boolean isRootNodeNotNull(String tag) {
        accessibilityNodeInfo = getRootInActiveWindow();
        if (accessibilityNodeInfo == null) {
            Log.i(TAG, String.format("动作：[%s]，RootNode为空！", tag));
            handler.sendEmptyMessageDelayed(CHANGE_ACTIVITY, 600);
        }
        return accessibilityNodeInfo != null;
    }

    /**
     * 在朋友圈页面，返回
     */

    private void back() {
        synchronized (AutoShareService.class) {
            int count = 2;
            while (count > 0) {
                performGlobalAction(GLOBAL_ACTION_BACK);
                Log.w(TAG, "-------执行返回--" + count);
                count--;
                try {
                    Thread.sleep(700);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            SendMsgWorkLine.forward();
            Log.w(TAG, "-------返回成功---------");
        }
    }

    private boolean pasteContent(AccessibilityNodeInfo root) {
        if (root.getChildCount() == 0) {
//            Log.i(TAG, "child widget----------------------------" + root.getClassName());
            String className = root.getClassName().toString();
            if (!TextUtils.isEmpty(className) && className.contains("EditText")) {
                Bundle arguments = new Bundle();
                String remove = "详情请扫描图片中的二维码";
                if (WechatTempContent.describeList.size() > 0) {
                    remove = WechatTempContent.describeList.remove(WechatTempContent.describeList.size() - 1);
                }
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, remove);
                    root.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                } else {
                    ClipboardManager clipService = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData text = ClipData.newPlainText("text", remove);
                    clipService.setPrimaryClip(text);
                    root.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                    root.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                }
                SendMsgWorkLine.forward();
                return true;
            }
        } else {
            for (int i = 0; i < root.getChildCount(); i++) {
                if (root.getChild(i) != null) {
                    if (pasteContent(root.getChild(i))) {
                        break;
                    }
                }
            }
        }
        return false;
    }


    /**
     * 发送
     */
    private synchronized void sendWeChat() {
        if (isRootNodeNotNull("复制内容-点击发送")) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    List<AccessibilityNodeInfo> list = accessibilityNodeInfo.findAccessibilityNodeInfosByText("发送");
                    if (list == null || list.isEmpty()) {
                        findNodeByIdClick("com.tencent.mm:id/ay5", 500);
                    } else {
                        for (AccessibilityNodeInfo n : list) {
                            n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            n.recycle();
                        }
                    }
                }
            }, 1000);
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.w(TAG, "服务启动...");
        instance = ScreenLockUtils.getInstance(this);
    }


    @Override
    public void onInterrupt() {

    }
}
