package com.ccq.share.auto;

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
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.ccq.share.Constants;
import com.ccq.share.activity.MainActivity;
import com.ccq.share.utils.ScreenLockUtils;
import com.ccq.share.utils.ToastUtil;
import com.ccq.share.utils.WechatTempContent;
import com.ccq.share.work.WorkLine;

import java.lang.ref.WeakReference;
import java.util.List;

/****************************************
 * 功能说明:  
 *
 * Author: Created by bayin on 2017/8/31.
 ****************************************/

public class AutoShareService extends AccessibilityService {
    private static final int OPEN_WECHAT = 654;
    private String TAG = "AutoShareService";
    public static final int BACK = 333;
    private final static int CHANGE_ACTIVITY = 456;

    // 微信首页名称
    private String launcherName = "com.tencent.mm.ui.LauncherUI";
    // 相册activity名称
    private String albumPageName = "com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI";

    // 是否正在执行发送朋友圈的动作
    private boolean isExecuteSendAction = false;

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
    //    private TaskObservable taskObservable;
    private WeakReference<AutoShareService> weakReference = new WeakReference<AutoShareService>(this);
    private AccessibilityNodeInfo accessibilityNodeInfo;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.w(TAG, "服务启动...");
        instance = ScreenLockUtils.getInstance(this);
//        taskObservable = new TaskObservable();
//        taskObservable.addObserver(DownPicService.getINSTANCE());
    }

    private boolean checkRootNodeNotNull(String tag) {
        accessibilityNodeInfo = getRootInActiveWindow();
        if (accessibilityNodeInfo == null) {
            Log.i(TAG, String.format("动作：[%s]，RootNode为空！", tag));
            handler.sendEmptyMessageDelayed(CHANGE_ACTIVITY, 400);
        }
        return accessibilityNodeInfo != null;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        Log.w(TAG, "EventType = " + event.getEventType() + "action = " + event.getAction() + "className = " + className);
        WorkLine.WorkNode nextNode = WorkLine.getNextNode();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (nextNode != null) {
                if (TextUtils.equals(className, launcherName)) {
                    if (nextNode.code == WorkLine.NODE_CHOOSE_FIND_ITEM) {
                        ToastUtil.show(nextNode.work);
                        // 点击“发现”
                        selectedFind();
                    }

                } else if (className.contains("SnsTimeLineUI")) {
                    // 朋友圈页面，点击右上角的ImageButton
                    if (nextNode.code == WorkLine.NODE_CLICK_IMAGEBTN) {
                        ToastUtil.show(nextNode.work);
                        clickSharePhotoImageBtn();
                    }
                } else if (TextUtils.equals(event.getClassName(), "com.tencent.mm.ui.base.k")) {
                    // 点击“从相册选择”
                    if (nextNode.code == WorkLine.NODE_OPEN_ALBUM) {
                        ToastUtil.show(nextNode.work);
                        openAlbum();
                    }
                } else if (TextUtils.equals(className, albumPageName)) {
                    // 选择照片
                    if (nextNode.code == WorkLine.NODE_SELECT_PICS) {
                        ToastUtil.show(nextNode.work);
                        choosePicture();
                    }
                } else if (className.contains("SnsUploadUI")) {
                    //发送朋友圈
                    ToastUtil.show(nextNode.work);
                    sendWeChat();
                }
            }
        }
    }


    /**
     * 点击发现tab
     */
    private synchronized void selectedFind() {
        if (checkRootNodeNotNull("点击发现")) {
            List<AccessibilityNodeInfo> findList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("发现");
            if (findList != null && findList.size() > 0) {
                AccessibilityNodeInfo findBtn = findList.get(0);
                if (findBtn != null && findBtn.getParent() != null) {
                    findBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    findBtn.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    WorkLine.forward();
                }
                WorkLine.WorkNode nextNode = WorkLine.getNextNode();
                if (nextNode != null && nextNode.code == WorkLine.NODE_CLICK_TIMELINE) {
                    jumpToCircleOfFriends();
                }
            }
        }
    }


    /**
     * 点击朋友圈右上角"发送朋友圈"按钮
     */
    private void clickSharePhotoImageBtn() {
        if (checkRootNodeNotNull("点击右上角")) {
            findImageBtn(accessibilityNodeInfo);
        }
    }


    private boolean findImageBtn(AccessibilityNodeInfo node) {
        if (node.getChildCount() == 0) {
            String className = node.getClassName().toString();
            if (!TextUtils.isEmpty(className) && className.contains("ImageButton")) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                WorkLine.forward();
                return true;
            }
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                if (node.getChild(i) != null) {
                    if (findImageBtn(node.getChild(i))) {
                        break;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 选择发送的图片
     */
    private void choosePicture() {
        if (checkRootNodeNotNull("选择图片")) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    List<AccessibilityNodeInfo> accessibilityNodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("预览");
                    if (accessibilityNodeInfoList == null ||
                            accessibilityNodeInfoList.size() == 0 ||
                            accessibilityNodeInfoList.get(0).getParent() == null ||
                            accessibilityNodeInfoList.get(0).getParent().getChildCount() == 0) {
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
            }, 300);
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


    private void jumpToCircleOfFriends() {
        if (checkRootNodeNotNull("点击朋友圈")) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    List<AccessibilityNodeInfo> list = accessibilityNodeInfo.findAccessibilityNodeInfosByText("朋友圈");
                    if (list != null && list.size() != 0) {
                        AccessibilityNodeInfo tempInfo = list.get(0);
                        if (tempInfo != null && tempInfo.getParent() != null) {
                            tempInfo.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                        WorkLine.forward();
                    }
                }
            }, 600);
        }
    }

    /**
     * 打开相册
     */
    private void openAlbum() {
        if (checkRootNodeNotNull("打开相册")) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    List<AccessibilityNodeInfo> accessibilityNodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("从相册选择");
                    traverseNode(accessibilityNodeInfoList);
                }
            }, 200);
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
                        WorkLine.forward();
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
     * 分享完成，通知下一个任务
     */
    private void notifyNextTask() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (weakReference.get() != null) {
                    instance.lockScreen();
                    //发送消息，下载下一个
//                    taskObservable.notifyShareFinish();
                }
            }
        });
    }

    private boolean pasteContent(AccessibilityNodeInfo root) {
        if (root.getChildCount() == 0) {
            Log.i(TAG, "child widget----------------------------" + root.getClassName());
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
        if (checkRootNodeNotNull("复制内容-点击发送")) {
            // 粘贴文字内容
            WorkLine.WorkNode nextNode = WorkLine.getNextNode();
            if (nextNode != null && nextNode.code == WorkLine.NODE_PASTE) {
                pasteContent(accessibilityNodeInfo);
                clickPublish();
            }

        }
    }

    private void clickPublish() {
        WorkLine.WorkNode nextNode = WorkLine.getNextNode();
        if (nextNode != null && nextNode.code == WorkLine.NODE_SEND_WECHAT) {
            ToastUtil.show("3秒后自动分享");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    List<AccessibilityNodeInfo> list = accessibilityNodeInfo.findAccessibilityNodeInfosByText("发表");
                    for (AccessibilityNodeInfo n : list) {
                        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        n.recycle();
                    }
                    handler.sendEmptyMessageDelayed(BACK, 3000);
                    ToastUtil.show("3秒后自动返回");
                    accessibilityNodeInfo.recycle();
                    isExecuteSendAction = false;
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("警告！");
        builder.setMessage("onInterrupt");
        builder.show();
        Log.e("onAccessibilityEvent", "---onInterrupt----");
    }
}