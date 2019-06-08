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
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.ccq.share.activity.MainActivity;
import com.ccq.share.home.MainPresenter;
import com.ccq.share.utils.ScreenLockUtils;
import com.ccq.share.utils.ToastUtil;
import com.ccq.share.utils.WechatTempContent;
import com.ccq.share.work.WorkLine;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.List;


/****************************************
 * 功能说明:  
 *
 * Author: Created by bayin on 2017/8/31.
 *
 * "发现"：cdh
 * ""
 ****************************************/

public class AutoShareService extends AccessibilityService {
    private String TAG = "AutoShareService";

    private static final int OPEN_WECHAT = 654;
    public static final int BACK = 333;
    private final static int CHANGE_ACTIVITY = 456;

    // 相册activity名称
    private String albumPageName = "com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI";

    // 是否正在执行发送朋友圈的动作

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

    private ScreenLockUtils instance;
    private WeakReference<AutoShareService> weakReference = new WeakReference<AutoShareService>(this);
    private AccessibilityNodeInfo accessibilityNodeInfo;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.w(TAG, "服务启动...");
        instance = ScreenLockUtils.getInstance(this);
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

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        WorkLine.WorkNode action = WorkLine.getNextNode();// 当前执行的操作

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
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
                notifyNextTask();
                return;
            }

            // 获取到节点不为空，才开始执行模拟点击逻辑
            if (action != null) {
                if (action.code == WorkLine.NODE_CHOOSE_FIND_ITEM && !className.contains("LauncherUI")) {
                    // 分享的第一步判断，如果不是微信首页直接退出此次分享
                    backToLauncher();
                    notifyNextTask();
                    return;
                }
                // 正常情况
                if (className.contains("LauncherUI")) {// 点击“发现”
                    if (action.code == WorkLine.NODE_CHOOSE_FIND_ITEM) {
                        ToastUtil.show(action.work);
                        selectedFind();
                    } else if (action.code == WorkLine.RETURN) {
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        WorkLine.forward();
                        notifyNextTask();
                    }
                } else if (className.contains("SnsTimeLineUI")) {// 朋友圈页面，点击右上角的ImageButton
                    if (action.code == WorkLine.NODE_CLICK_IMAGEBTN) {
                        ToastUtil.show(action.work);
                        clickSharePhotoImageBtn();
                    } else if (action.code == WorkLine.RETURN) {
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        WorkLine.forward();
                    }
                } else if (TextUtils.equals(event.getClassName(), "com.tencent.mm.ui.base.k")) {// 点击“从相册选择”
                    if (action.code == WorkLine.NODE_OPEN_ALBUM) {
                        ToastUtil.show(action.work);
                        openAlbum();
                    }
                } else if (TextUtils.equals(className, albumPageName)) {// 选择照片
                    if (action.code == WorkLine.NODE_SELECT_PICS) {
                        ToastUtil.show("3秒后自动选择图片");
                        choosePicture();
                    }
                } else if (className.contains("SnsUploadUI")) {// 发送朋友圈
                    if (action.code == WorkLine.NODE_PASTE) {
                        ToastUtil.show(action.work);
                        if (isRootNodeNotNull("复制内容-点击发送")) {
                            pasteContent(accessibilityNodeInfo);
                        }

                        WorkLine.WorkNode nextNode = WorkLine.getNextNode();
                        if (nextNode.code == WorkLine.NODE_SEND_WECHAT) {
                            sendWeChat();
                        }
                    }
                }
            }
        }
    }

    private void step(String desc) {
        ToastUtil.show("异常，跳过本次分享" + "[" + desc + "]");
        int time = 3;
        WorkLine.getWorkList().clear();
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


    /**
     * 点击发现tab
     */
    private synchronized void selectedFind() {
        if (isRootNodeNotNull("点击发现")) {
            List<AccessibilityNodeInfo> findList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("发现");
            if (findList != null && findList.size() > 0) {
                AccessibilityNodeInfo findBtn = findList.get(0);
                if (findBtn != null && findBtn.getParent() != null) {
                    findBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    findBtn.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    WorkLine.forward();
                } else {
                    step("发现tab");
                }
                WorkLine.WorkNode nextNode = WorkLine.getNextNode();
                if (nextNode != null && nextNode.code == WorkLine.NODE_CLICK_TIMELINE) {
                    jumpToCircleOfFriends();
                }
            } else {
                step("发现tab");
            }
        }
    }

    /**
     * 点击"朋友圈"
     */
    private void jumpToCircleOfFriends() {
        if (isRootNodeNotNull("点击朋友圈")) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    List<AccessibilityNodeInfo> list = accessibilityNodeInfo.findAccessibilityNodeInfosByText("朋友圈");
                    if (list != null && list.size() != 0) {
                        AccessibilityNodeInfo tempInfo = list.get(0);
                        if (tempInfo != null && tempInfo.getParent() != null) {
                            tempInfo.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        } else {
                            step("朋友圈");
                        }
                        WorkLine.forward();
                    } else {
                        step("朋友圈");
                    }
                }
            }, 1000);
        }
    }

    /**
     * 点击朋友圈右上角"发送朋友圈"按钮
     */
    private void clickSharePhotoImageBtn() {
        if (isRootNodeNotNull("点击右上角")) {
            findImageBtn(accessibilityNodeInfo);
        }
    }


    private boolean findImageBtn(AccessibilityNodeInfo node) {
        if (node.getChildCount() == 0) {
            String className = node.getClassName().toString();
            if (!TextUtils.isEmpty(className) && className.contains("ImageButton")) {
                Log.w(TAG, "查找ImageButton：" + node.getViewIdResourceName() + "  desc:[" + node.getContentDescription().toString());
                if (TextUtils.equals("拍照分享", node.getContentDescription().toString())) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    WorkLine.forward();
                    return true;
                }
            }
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                if (node.getChild(i) != null) {
                    findImageBtn(node.getChild(i));
                }
            }
        }
        return false;
    }

    /**
     * 选择发送的图片
     */
    private void choosePicture() {
        if (isRootNodeNotNull("选择图片")) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    List<AccessibilityNodeInfo> accessibilityNodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("预览");
                    if (accessibilityNodeInfoList == null ||
                            accessibilityNodeInfoList.size() == 0 ||
                            accessibilityNodeInfoList.get(0).getParent() == null ||
                            accessibilityNodeInfoList.get(0).getParent().getChildCount() == 0) {
                        step("选择图片");
                        return;
                    }
                    AccessibilityNodeInfo tempInfo = accessibilityNodeInfoList.get(0).getParent().getChild(3);
                    if (tempInfo != null) {
                        for (int j = 0; j < WorkLine.size; j++) {
                            AccessibilityNodeInfo childNodeInfo = tempInfo.getChild(j);
                            if (childNodeInfo != null) {
                                for (int k = 0; k < childNodeInfo.getChildCount(); k++) {
                                    childNodeInfo.getChild(k).performAction(AccessibilityNodeInfo.ACTION_CLICK);//选中图片
                                }
                            }
                        }
                    }

                    List<AccessibilityNodeInfo> finishList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("完成(" + WorkLine.size + "/9)");//点击确定
                    performClickBtn(finishList);
                }
            }, 2500);
        }
    }

    /**
     * @param accessibilityNodeInfoList
     * @return
     */
    private boolean performClickBtn(List<AccessibilityNodeInfo> accessibilityNodeInfoList) {
        if (accessibilityNodeInfoList != null && accessibilityNodeInfoList.size() != 0) {
            for (int i = 0; i < accessibilityNodeInfoList.size(); i++) {
                AccessibilityNodeInfo accessibilityNodeInfo = accessibilityNodeInfoList.get(i);
                if (accessibilityNodeInfo != null) {
                    accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    WorkLine.forward();
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * 打开相册
     */
    private void openAlbum() {
        if (isRootNodeNotNull("打开相册")) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    List<AccessibilityNodeInfo> accessibilityNodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("从相册选择");
                    boolean b = traverseNode(accessibilityNodeInfoList);
                    if (b) {
                        WorkLine.forward();
                    } else {
                        step("打开相册");
                    }
                }
            }, 1000);
        }
    }

    private boolean traverseNode(List<AccessibilityNodeInfo> accessibilityNodeInfoList) {
        if (accessibilityNodeInfoList != null && accessibilityNodeInfoList.size() != 0) {
            AccessibilityNodeInfo accessibilityNodeInfo = accessibilityNodeInfoList.get(0).getParent();
            if (accessibilityNodeInfo != null && accessibilityNodeInfo.getChildCount() != 0) {
                accessibilityNodeInfo = accessibilityNodeInfo.getChild(0);
                if (accessibilityNodeInfo != null) {
                    accessibilityNodeInfo = accessibilityNodeInfo.getParent();
                    if (accessibilityNodeInfo != null) {
                        accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);//点击从相册中选择
                        return true;
                    }
                }
            }
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
            WorkLine.forward();
            Log.w(TAG, "-------返回成功---------");
            notifyNextTask();
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
                WorkLine.forward();
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

    private synchronized void sendWeChat() {
        if (isRootNodeNotNull("复制内容-点击发送")) {
            clickPublish();
        }
    }

    private void clickPublish() {
        WorkLine.WorkNode nextNode = WorkLine.getNextNode();
        if (nextNode != null && nextNode.code == WorkLine.NODE_SEND_WECHAT) {
            ToastUtil.show("自动分享");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    List<AccessibilityNodeInfo> list = accessibilityNodeInfo.findAccessibilityNodeInfosByText("发表");
                    for (AccessibilityNodeInfo n : list) {
                        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        n.recycle();
                    }

//                    handler.sendEmptyMessageDelayed(BACK, 3000);
                    ToastUtil.show("自动返回");
                    accessibilityNodeInfo.recycle();
                    WorkLine.forward();
                }
            }, 1000);
        }
    }

    private AccessibilityNodeInfo getNodeByClassName(AccessibilityNodeInfo node, String name) {
        // TODO: 2019/1/28
        if (node != null && !TextUtils.isEmpty(name)) {
            if (TextUtils.equals(node.getClassName(), name)) {
                return node;
            } else {
                if (node.getChildCount() > 0) {
                    for (int i = 0; i < node.getChildCount(); i++) {
                        AccessibilityNodeInfo nodeByClassName = getNodeByClassName(node.getChild(i), name);
                        if (nodeByClassName != null && TextUtils.equals(nodeByClassName.getClassName(), name))
                            return nodeByClassName;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void onInterrupt() {
        Log.e("onAccessibilityEvent", "---onInterrupt----");
    }
}
