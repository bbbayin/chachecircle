package com.ccq.share;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
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
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.ccq.share.activity.MainActivity;
import com.ccq.share.home.MainPresenter;
import com.ccq.share.utils.ScreenLockUtils;
import com.ccq.share.utils.ToastUtil;
import com.ccq.share.utils.WechatTempContent;
import com.ccq.share.work.SendMsgWorkLine;
import com.ccq.share.work.WorkLine;

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
                backToLauncher();
                return;
            }
        }
        if (action != null) {
            Log.d(TAG, "当前任务：" + action.work);
            ToastUtil.show(action.work);
            switch (action.code) {
                case SendMsgWorkLine.NODE_CLICK_WECHAT_TAB:
                    clickWechatTab();
                    break;
                case SendMsgWorkLine.NODE_CLICK_CHAT_ITEM:// 聊天item
                    findNodeByIdClick("com.tencent.mm:id/fzg");
                    break;
                case SendMsgWorkLine.NODE_PASTE:// 粘贴文字
                    if (isRootNodeNotNull("粘贴文字")) {
                        pasteContent();
                    }
                    break;
                case SendMsgWorkLine.NODE_CLICK_SEND:// 发送文字
                    if (isRootNodeNotNull("发送文字")) {
                        sendWeChat();
                    }
                    break;
                case SendMsgWorkLine.NODE_CLICK_ADD_BTN:// 加号
                    ToastUtil.show("2s后发送图片");
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            findNodeByIdClick("com.tencent.mm:id/au0");
                        }
                    }, 2000);
                    break;
                case SendMsgWorkLine.NODE_OPEN_ALBUM:// 相册
                    findNodeByIdClick("com.tencent.mm:id/rr");
                    break;
                case SendMsgWorkLine.NODE_SELECT_PICS:// 选择照片
                    ToastUtil.show("3秒后自动选择图片");
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
            findNodeByIdClick("com.tencent.mm:id/dtf");
//            List<AccessibilityNodeInfo> findList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("微信");
//            if (findList != null && findList.size() > 0) {
//                AccessibilityNodeInfo findBtn = findList.get(0);
//                if (findBtn != null && findBtn.getParent() != null) {
//                    findBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                    findBtn.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                    SendMsgWorkLine.forward();
//                } else {
//                    step("微信tab");
//                }
//            } else {
//                step("微信tab");
//            }
        }
    }

    /**
     * 选择发送的图片
     */
    private void choosePicture() {
        if (isRootNodeNotNull("选择图片")) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    List<AccessibilityNodeInfo> accessibilityNodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/fbe");
                    if (accessibilityNodeInfoList == null || accessibilityNodeInfoList.isEmpty()) {
                        step("选择图片");
                        return;
                    }
                    for (int i = 0; i < SendMsgWorkLine.size; i++) {
                        accessibilityNodeInfoList.get(i).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
//                    findNodeByIdClick("com.tencent.mm:id/d6");
                    List<AccessibilityNodeInfo> finishList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("发送(" + SendMsgWorkLine.size + "/9)");//点击确定
                    performClickBtn(finishList);
                }
            }, 2500);
        }
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

    private void findNodeByIdClick(final String id) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                List<AccessibilityNodeInfo> nodes = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(id);
                if (nodes != null && !nodes.isEmpty()) {
                    ToastUtil.show("找到id:" + id);
                    final AccessibilityNodeInfo node = nodes.get(0);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            if (node.getParent() != null) {
                                node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            }
                            SendMsgWorkLine.forward();
                        }
                    }, 100);
                }
            }
        }, 1000);
    }

    @Override
    public void onInterrupt() {

    }

    /**
     * 分享完成，通知下一个任务
     */
    private void notifyNextTask() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (weakReference.get() != null) {
                    instance.lockScreen();
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

    /**
     * 判断到当前不是微信首页，返回到桌面，跳过此次分享
     */
    private void backToLauncher() {
        synchronized (AutoShareService.class) {
            int count = 5;
            while (count > 0) {
                performGlobalAction(GLOBAL_ACTION_BACK);
                count--;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 粘贴文字内容
     */
    private void pasteContent() {
//        findNodeByIdClick("com.tencent.mm:id/auj");
        List<AccessibilityNodeInfo> nodeList = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/auj");
        if (nodeList != null && nodeList.size() > 1) {
            AccessibilityNodeInfo root = nodeList.get(1);
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

            sendWeChat();
        }
    }

    /**
     * 发送
     */
    private synchronized void sendWeChat() {
        if (isRootNodeNotNull("复制内容-点击发送")) {
            SendMsgWorkLine.WorkNode nextNode = SendMsgWorkLine.getNextNode();
            if (nextNode != null && nextNode.code == SendMsgWorkLine.NODE_CLICK_SEND) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        List<AccessibilityNodeInfo> list = accessibilityNodeInfo.findAccessibilityNodeInfosByText("发送");
                        if (list == null || list.isEmpty()) {
                            findNodeByIdClick("com.tencent.mm:id/ay5");
                        } else {
                            for (AccessibilityNodeInfo n : list) {
                                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                n.recycle();
                            }
                        }
                        SendMsgWorkLine.forward();
                    }
                }, 1000);
            }
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.w(TAG, "服务启动...");
        instance = ScreenLockUtils.getInstance(this);
    }
}
