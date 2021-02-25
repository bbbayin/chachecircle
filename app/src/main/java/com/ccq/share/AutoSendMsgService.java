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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private int index = 0;

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
    private String remove = "铲车圈分享";

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();

        if (index == 0) {
            if (WechatTempContent.describeList.size() > 0) {
                remove = WechatTempContent.describeList.remove(WechatTempContent.describeList.size() - 1);
            }
        }
        if (index >= WechatTempContent.chatNumber) {
            // 结束了
            notifyNextTask();
            return;
        }

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
        List<AccessibilityNodeInfo> idNodes = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(ViewIds.ITEM_ID_1);
        for (int i = 0; i < idNodes.size(); i++) {
            AccessibilityNodeInfo item = idNodes.get(i);
            Log.i(TAG, "hash = " + item.toString());
        }


        SendMsgWorkLine.WorkNode action = SendMsgWorkLine.getNextNode();
        if (action != null) {
            Log.d(TAG, "当前任务：" + action.work);
            switch (action.code) {
//                case SendMsgWorkLine.NODE_CLICK_WECHAT_TAB:6777777777787777
//                    clickWechatTab();
//                    break;
                case SendMsgWorkLine.NODE_CLICK_CHAT_ITEM:// 聊天item
                    Log.d(TAG, "------点击对话-------" + index);
                    clickChatItem();
                    break;

                case SendMsgWorkLine.NODE_PASTE:// 粘贴文字
                    Log.d(TAG, "------粘贴文字-------" + index);
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    pasteContent(accessibilityNodeInfo);
                    sendWeChat();
                    break;

                case SendMsgWorkLine.NODE_CLICK_ADD_BTN:// 加号
                    Log.d(TAG, "------点加号-------" + index);
                    findNodeByIdClick(ViewIds.IMAGE_PLUS, 500);
                    SendMsgWorkLine.forward();
                    break;

                case SendMsgWorkLine.NODE_OPEN_ALBUM:// 相册
                    Log.d(TAG, "------点相册-------" + index);
                    findNodeByIdClick(ViewIds.BTN_ALBUM, 500);
                    SendMsgWorkLine.forward();
                    break;

                case SendMsgWorkLine.NODE_SELECT_PICS:// 选择照片
                    if (!isChoosing) {
                        Log.d(TAG, "------选图片-------" + index);
                        choosePicture();
                        SendMsgWorkLine.forward();
                    }
                    break;

                case SendMsgWorkLine.RETURN:// 返回
                    Log.d(TAG, "------返回桌面-------" + index);
                    back();
                    SendMsgWorkLine.forward();
                    nextChat();
                    break;
            }
        }
    }

    private final Set<AccessibilityNodeInfo> clickedItems = new HashSet<>();

    private synchronized void clickChatItem() {
        try {
            Thread.sleep(500);
            List<AccessibilityNodeInfo> idNodes = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(ViewIds.ITEM_ID_1);
            if (idNodes != null && !idNodes.isEmpty()) {
                for (int i = WechatTempContent.chatNumber - 1; i >= 0; i--) {
                    AccessibilityNodeInfo item = idNodes.get(i);
                    for (AccessibilityNodeInfo info :
                            clickedItems) {
                        Log.d(TAG, "当前容齐："+info.hashCode());
                    }
                    if (clickedItems.contains(item)) {
                        Log.d(TAG, "已经点过了：" + item.hashCode());
                    } else {
                        boolean b = item.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        if (b) {
                            Log.d(TAG, "点击聊天成功"+item.hashCode());
                            clickedItems.add(item);
                            SendMsgWorkLine.forward();
                            break;
                        } else {
                            handler.sendEmptyMessageDelayed(CHANGE_ACTIVITY, 100);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    volatile boolean isChoosing = false;

    /**
     * 选择发送的图片
     */
    private synchronized void choosePicture() {
        isChoosing = true;
        try {
            Thread.sleep(500);
            List<AccessibilityNodeInfo> accessibilityNodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/fbe");
            if (accessibilityNodeInfoList == null || accessibilityNodeInfoList.isEmpty()) {
                step("选择图片");
                return;
            }
            for (int i = 0; i < SendMsgWorkLine.size; i++) {
                // 点击下载好的图片
                accessibilityNodeInfoList.get(i).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            // 发送图片
            clickPicture();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送图片
     */
    private void clickPicture() {
        try {
            Thread.sleep(500);
            List<AccessibilityNodeInfo> nodes = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(ViewIds.BTN_SEND_PICS);
            if (nodes != null && !nodes.isEmpty()) {
                final AccessibilityNodeInfo node = nodes.get(0);
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (node.getParent() != null) {
                    node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
                isChoosing = false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void findNodeByIdClick(final String id, long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<AccessibilityNodeInfo> nodes = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(id);
        if (nodes != null && !nodes.isEmpty()) {
            final AccessibilityNodeInfo node = nodes.get(0);
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            if (node.getParent() != null) {
                node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }


    /**
     * 粘贴文字
     *
     * @param root
     * @return
     */
    private boolean pasteContent(AccessibilityNodeInfo root) {
        if (root.getChildCount() == 0) {
            String className = root.getClassName().toString();
            if (!TextUtils.isEmpty(className) && className.contains("EditText")) {
                Bundle arguments = new Bundle();

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
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                List<AccessibilityNodeInfo> sendNodes = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(ViewIds.BTN_SEND_MSG);
                if (sendNodes != null && !sendNodes.isEmpty()) {
                    boolean b = sendNodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    if (b) {
                        SendMsgWorkLine.forward();
                    } else {
                        if (clickTextSend()) {
                            SendMsgWorkLine.forward();
                            Log.d(TAG, "发送成功...");
                        } else {
                            Log.d(TAG, "发送失败！！！");
                        }
                    }
                } else {
                    if (clickTextSend()) {
                        SendMsgWorkLine.forward();
                        Log.d(TAG, "发送成功...");
                    } else {
                        Log.d(TAG, "发送失败！！！");
                    }
                }
            }
        }, 1000);
    }

    private boolean clickTextSend() {
        List<AccessibilityNodeInfo> list = accessibilityNodeInfo.findAccessibilityNodeInfosByText("发送");
        for (AccessibilityNodeInfo n : list) {
            boolean b = n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            if (b) return true;
            n.recycle();
        }
        return false;
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
            Log.w(TAG, "-------返回成功---------");
        }
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

    private void nextChat() {
        index++;
        SendMsgWorkLine.reInit();
        try {
            Thread.sleep(1000);
            handler.sendEmptyMessage(OPEN_WECHAT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 分享完成，通知下一个任务
     */
    private void notifyNextTask() {
        clickedItems.clear();
        index = 0;
        SendMsgWorkLine.clear();
        EventBus.getDefault().post(MainPresenter.FINISH);
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
