package com.ccq.share;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.ccq.share.core.DownPicService;
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
    private String TAG = "AutoShareService";
    public static final int BACK = 333;
    // 微信首页名称
    private String launcherName = "com.tencent.mm.ui.LauncherUI";
    // 相册activity名称
    private String albumPageName = "com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI";// com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI

    // 是否正在执行发送朋友圈的动作
    private boolean isExecuteSendAction = false;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case BACK:
                    back();
                    break;
            }
        }
    };
    private KeyguardManager.KeyguardLock kl;
    private boolean locked;
    private ScreenLockUtils instance;
    private TaskObservable taskObservable;
    private WeakReference<AutoShareService> weakReference = new WeakReference<AutoShareService>(this);

    /**
     * 回到桌面
     */
    private void home() {
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
//        ScreenLockUtils.getInstance(this).lockScreen();
        instance.lockScreen();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.w(TAG, "服务启动...");
        instance = ScreenLockUtils.getInstance(this);
        DownPicService downPicService = new DownPicService();
        taskObservable = new TaskObservable();
        taskObservable.addObserver(downPicService);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.w(TAG, "接收事件：" + event.getEventType());
        int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                String className = event.getClassName().toString();
//                Log.w(TAG, "TYPE_WINDOW_STATE_CHANGED：" + event.getClassName());

                WorkLine.WorkNode nextNode;
                if (TextUtils.equals(className, launcherName)) {
                    nextNode = WorkLine.getNextNode();
                    if (nextNode != null && nextNode.code == WorkLine.NODE_CHOOSE_FIND_ITEM) {
                        // 点击“发现”
                        Log.w(TAG, nextNode.toString() + "96");
                        selectedFind();
                    }

                } else if (className.contains("SnsTimeLineUI")) {
                    // 朋友圈页面，点击右上角的ImageButton
                    nextNode = WorkLine.getNextNode();
                    if (nextNode != null && nextNode.code == WorkLine.NODE_CLICK_IMAGEBTN) {
                        Log.w(TAG, nextNode.toString()  + "104");
                        clickSharePhotoImageBtn();
                    }
                } else if (TextUtils.equals(event.getClassName(), "com.tencent.mm.ui.base.k")) {
                    // 点击“从相册选择”
                    nextNode = WorkLine.getNextNode();
                    if (nextNode != null && nextNode.code == WorkLine.NODE_OPEN_ALBUM) {
                        Log.w(TAG, nextNode.toString() + "111");
                        openAlbum();
                    }
                } else if (TextUtils.equals(className, albumPageName)) {
                    // 选择照片
                    nextNode = WorkLine.getNextNode();
                    if (nextNode != null && nextNode.code == WorkLine.NODE_SELECT_PICS) {
                        Log.w(TAG, nextNode.toString()  + "118");
                        choosePicture(WorkLine.size);
                    }
                } else if (className.contains("SnsUploadUI")) {
                    //发送朋友圈
                    if (!isExecuteSendAction) {
                        sendWeChat();
                    }
                }

                // 相册 AlbumPreviewUI 选择角标图片
                // 朋友圈 SnsTimeLineUI 点击右上角的按钮
                /**
                 * com.tencent.mm:id/jr
                 * android.widget.ImageButton
                 * 拍照分享
                 */

                break;
//            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
//                Log.w(TAG, "TYPE_WINDOWS_CHANGED：" + event.getClassName());
//                break;

        }
    }


    /**
     * 点击发现tab
     */
    private void selectedFind() {
        List<AccessibilityNodeInfo> tvFind = getRootInActiveWindow().findAccessibilityNodeInfosByText("发现");
        if (tvFind != null && tvFind.size() > 0) {
            for (int i = 0; i < tvFind.size(); i++) {
                tvFind.get(i).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            WorkLine.forward();
            WorkLine.WorkNode nextNode = WorkLine.getNextNode();
            if (nextNode != null && nextNode.code == WorkLine.NODE_CLICK_TIMELINE) {
                Log.w(TAG, nextNode.toString() + "160");
                jumpToCircleOfFriends();
            }
        }
    }


    private void clickSharePhotoImageBtn() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            findImageBtn(nodeInfo);
        } else {
            Log.w(TAG, "找不到ImageButton");
        }

    }


    private boolean findImageBtn(AccessibilityNodeInfo root) {
        if (root.getChildCount() == 0) {
            Log.i(TAG, "child widget----------------------------" + root.getClassName());
            String className = root.getClassName().toString();
            if (!TextUtils.isEmpty(className) && className.contains("ImageButton")) {
                root.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                WorkLine.forward();
                return true;
            }
        } else {
            for (int i = 0; i < root.getChildCount(); i++) {
                if (root.getChild(i) != null) {
                    if (findImageBtn(root.getChild(i))) {
                        break;
                    }
                }
            }
        }
        return false;
    }


    private void choosePicture(final int picCount) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getRootInActiveWindow() == null) {
                    return;
                }
                List<AccessibilityNodeInfo> accessibilityNodeInfoList = getRootInActiveWindow().findAccessibilityNodeInfosByText("预览");
                if (accessibilityNodeInfoList == null ||
                        accessibilityNodeInfoList.size() == 0 ||
                        accessibilityNodeInfoList.get(0).getParent() == null ||
                        accessibilityNodeInfoList.get(0).getParent().getChildCount() == 0) {
                    return;
                }
                AccessibilityNodeInfo tempInfo = accessibilityNodeInfoList.get(0).getParent().getChild(3);

                for (int j = 0; j < picCount; j++) {
                    AccessibilityNodeInfo childNodeInfo = tempInfo.getChild(j);
                    if (childNodeInfo != null) {
                        for (int k = 0; k < childNodeInfo.getChildCount(); k++) {
                            if (childNodeInfo.getChild(k).isEnabled() && childNodeInfo.getChild(k).isClickable()) {
                                childNodeInfo.getChild(k).performAction(AccessibilityNodeInfo.ACTION_CLICK);//选中图片
                            }
                        }
                    }
                }

                List<AccessibilityNodeInfo> finishList = getRootInActiveWindow().findAccessibilityNodeInfosByText("完成(" + picCount + "/9)");//点击确定
                if (performClickBtn(finishList)) {
                    WorkLine.forward();
                }
            }
        }, 1000);
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
                    if (accessibilityNodeInfo.isClickable() && accessibilityNodeInfo.isEnabled()) {
                        accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private void jumpToCircleOfFriends() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                List<AccessibilityNodeInfo> list = getRootInActiveWindow().findAccessibilityNodeInfosByText("朋友圈");
                if (list != null && list.size() != 0) {
                    AccessibilityNodeInfo tempInfo = list.get(0);
                    if (tempInfo != null && tempInfo.getParent() != null) {
                        tempInfo.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                    WorkLine.forward();
                }
            }
        }, 1000);
    }

    /**
     * 打开相册
     */
    private void openAlbum() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getRootInActiveWindow() == null) {
                    return;
                }

                List<AccessibilityNodeInfo> accessibilityNodeInfoList = getRootInActiveWindow().findAccessibilityNodeInfosByText("从相册选择");
                traverseNode(accessibilityNodeInfoList);
            }
        }, 200);
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
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (weakReference.get() != null) {
                        instance.lockScreen();
                        //发送消息，下载下一个
                        taskObservable.notifyShareFinish();
                    }
                }
            });
        }
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

    private synchronized void sendWeChat() {

        final AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        if (nodeInfo != null) {
            isExecuteSendAction = true;
            // 粘贴文字内容
            WorkLine.WorkNode nextNode = WorkLine.getNextNode();
            if (nextNode!=null && nextNode.code == WorkLine.NODE_PASTE) {
                pasteContent(nodeInfo);
            }

            nextNode = WorkLine.getNextNode();
            if (nextNode!=null && nextNode.code == WorkLine.NODE_SEND_WECHAT) {
                ToastUtil.show("3秒后自动分享");
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("发表");

                        for (AccessibilityNodeInfo n : list) {
                            n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                        Log.w(TAG, "------分享成功------");
                            n.recycle();
                        }
                        handler.sendEmptyMessageDelayed(BACK, 4000);
                        ToastUtil.show("4秒后自动返回");
                        nodeInfo.recycle();
                        isExecuteSendAction = false;
                    }
                }, 3000);
            }
        }
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
