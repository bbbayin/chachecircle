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
import com.ccq.share.utils.ToastUtil;
import com.ccq.share.utils.WechatTempContent;
import com.ccq.share.work.SendMsgWorkLine;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
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
    private WeakReference<AutoSendMsgService> weakReference = new WeakReference<>(this);
    private AccessibilityNodeInfo accessibilityNodeInfo;

    //    private static final int OPEN_WECHAT = 654;
    public static final int BACK = 333;
    private final static int TO_MAIN_PAGE = 456;
    private final static int BACK_AFTER_SEND_PICS = 2876;

    private int index = 0;
    private int selectedImageTaskID = 0;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case BACK:
                    back();
                    break;
                case TO_MAIN_PAGE:
                    changeActivity("back");
                    break;
                case BACK_AFTER_SEND_PICS:
                    SendMsgWorkLine.forward();
                    boolean b = performGlobalAction(GLOBAL_ACTION_BACK);
                    while (!b) {
                        b = performGlobalAction(GLOBAL_ACTION_BACK);
                    }
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            next();
                        }
                    }, 3000);
                    break;
            }
        }
    };

    private void openWeChat() {
        PackageManager packageManager = getBaseContext().getPackageManager();
        Intent it = packageManager.getLaunchIntentForPackage(Constants.WECHAT_PACKAGE_NAME);
        startActivity(it);
    }

    private String sendContent = "铲车圈分享";
    private List<String> returnTagList = new ArrayList<>();

    /**
     * 切换页面
     */
    private void changeActivity(String type) {
        Intent intent = new Intent(weakReference.get(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("extra", type);
        getApplicationContext().startActivity(intent);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();

        if (index == 0) {
            // 分享任务到达，获取文字
            if (WechatTempContent.describeList.size() > 0) {
                sendContent = WechatTempContent.describeList.remove(WechatTempContent.describeList.size() - 1);
            }
        }

        // 窗口事件
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            if (isChoosing) return;
            try {
                Thread.sleep(100);
                accessibilityNodeInfo = getRootInActiveWindow();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 判断节点是否为空
            if (accessibilityNodeInfo == null) {
                changeActivity("back");
                return;
            }
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                System.out.println("当前页面：" + event.getClassName());
            }
            handleEvent(event.getClassName(), eventType);
        }
    }

    private void handleEvent(CharSequence pageName, int eventType) {
        SendMsgWorkLine.WorkNode action = SendMsgWorkLine.getNextNode();
        if (action != null) {
            Log.d(TAG, "当前任务：" + action.work);
            if (action.code == SendMsgWorkLine.NODE_SELECT_PICS && eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                Log.w(TAG, "无效事件" + action);
                return;
            }
            switch (action.code) {
                case SendMsgWorkLine.NODE_CLICK_CHAT_ITEM:// 聊天item
                    clickChatItemWithForward();
                    break;
                case SendMsgWorkLine.NODE_PASTE:// 粘贴文字
                    if (!"android.widget.LinearLayout".contentEquals(pageName)) {
                        clickChatItemWithForward();
                    } else {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        pasteContent(accessibilityNodeInfo);
                        sendWeChatWithForward();
                    }
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
                    if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                            && pageName.equals("com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI") &&
                            !isChoosing && selectedImageTaskID == index) {
                        // 只有页面改变时才触发选图片动作
                        Log.d(TAG, "------选图片-------" + index);
                        // 选择图片+ 发送
                        isChoosing = true;
                        choosePicture();
                        isChoosing = false;
                    }
                    break;
                case SendMsgWorkLine.RETURN:// 返回
                    back();
                    SendMsgWorkLine.forward();
//                    String tag = "return" + index;
//                    if (returnTagList.contains(tag)) {
//                        return;
//                    } else {
//
//                    }
//                    returnTagList.add(tag);
                    break;
                case SendMsgWorkLine.NEXT:
                    if (pageName.equals("com.tencent.mm.ui.LauncherUI")) {
                        next();
                    }
                    break;
            }
        }
    }

    private void next() {
        index++;
        if (index >= WechatTempContent.chatNumber) {
            notifyNextTask();
            changeActivity("");
        } else {
            nextChat();
            changeActivity("back");
        }
    }

    private void nextChat() {
        Log.w(TAG, "分享下个聊天...");
        SendMsgWorkLine.reInit();
//        try {
//            Thread.sleep(100);
//            openWeChat();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    private final Set<Integer> clickedItems = new HashSet<>();

    private void printNodes(List<AccessibilityNodeInfo> nodes) {
        Log.w(TAG, ">>>>>>>>>>>>>>>>>>");
        for (int i = 0; i < nodes.size(); i++) {
            Log.w(TAG, nodes.get(i).hashCode() + "-----" + i);
        }
        Log.w(TAG, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
    }

    private synchronized void clickChatItemWithForward() {
        try {
            Thread.sleep(100);
            String title = String.format("%s-%s", WechatTempContent.chatName, index + 1);
            Log.d(TAG, "目标文字：" + title);
            List<AccessibilityNodeInfo> titleNodes = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(ViewIds.ITEM_CHAT_TITLE);
            int cur = 0;
            for (int i = 0; i < titleNodes.size(); i++) {
                CharSequence text = titleNodes.get(i).getText();
                Log.d(TAG, "当前item文字：" + title);
                if (title.equals(text.toString())) {
                    cur = i;
                    break;
                }
            }
            List<AccessibilityNodeInfo> idNodes = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(ViewIds.ITEM_ID_1);
            if (!idNodes.isEmpty()) {
                boolean b = idNodes.get(cur).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.d(TAG, "点击聊天item结果：" + b);
                SendMsgWorkLine.forward();
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
        try {
            Thread.sleep(500);
            List<AccessibilityNodeInfo> accessibilityNodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/fbe");
            if (accessibilityNodeInfoList == null || accessibilityNodeInfoList.isEmpty()) {
                step("选择图片");
                isChoosing = false;
                return;
            }
            for (int i = 0; i < SendMsgWorkLine.size; i++) {
                // 点击下载好的图片
                accessibilityNodeInfoList.get(i).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            selectedImageTaskID++;
            // 发送图片
            sendPicture();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送图片
     */
    private void sendPicture() {
        try {
            Thread.sleep(500);
            List<AccessibilityNodeInfo> nodes = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(ViewIds.BTN_SEND_PICS);
            if (nodes != null && !nodes.isEmpty()) {
                final AccessibilityNodeInfo node = nodes.get(0);
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (node.getParent() != null) {
                    node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
            handler.sendEmptyMessageDelayed(BACK_AFTER_SEND_PICS, 4000);
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
        accessibilityNodeInfo = getRootInActiveWindow();
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
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, sendContent);
                    root.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                } else {
                    ClipboardManager clipService = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData text = ClipData.newPlainText("text", sendContent);
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
    private synchronized void sendWeChatWithForward() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        accessibilityNodeInfo = getRootInActiveWindow();
        List<AccessibilityNodeInfo> sendNodes = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(ViewIds.BTN_SEND_MSG);
        if (sendNodes != null && !sendNodes.isEmpty()) {
            boolean b = sendNodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            if (b) {
                Log.d(TAG, "发送成功...1");
                SendMsgWorkLine.forward();
            } else {
                sendText2();
            }
        } else {
            sendText2();
        }
    }

    private void sendText2() {
        if (clickTextSend()) {
            SendMsgWorkLine.forward();
            Log.d(TAG, "发送成功...2");
        } else {
            handler.sendEmptyMessage(TO_MAIN_PAGE);
        }
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
        synchronized (AutoSendMsgService.class) {
            int count = 5;
            while (count > 0) {
                boolean b = performGlobalAction(GLOBAL_ACTION_BACK);
                Log.w(TAG, "-------执行返回--" + b);
                count--;
                try {
                    Thread.sleep(80);
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
            handler.sendEmptyMessageDelayed(TO_MAIN_PAGE, 600);
        }
        return accessibilityNodeInfo != null;
    }

    /**
     * 分享完成，通知下一个任务
     */
    private void notifyNextTask() {
        Log.w(TAG, "通知下个任务...");
        clickedItems.clear();
        returnTagList.clear();
        selectedImageTaskID = 0;
        index = 0;
        SendMsgWorkLine.clear();
        back();
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
    }


    @Override
    public void onInterrupt() {

    }
}
