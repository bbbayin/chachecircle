package com.ccq.share;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
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
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;

import com.ccq.share.activity.MainActivity;
import com.ccq.share.core.ImageDownloadManager;
import com.ccq.share.utils.ScreenLockUtils;
import com.ccq.share.utils.ToastUtil;
import com.ccq.share.utils.WechatTempContent;
import com.ccq.share.work.WorkLine;

import java.util.List;

public class BaseService extends AccessibilityService {

    private AccessibilityManager mAccessibilityManager;
    private Context mContext;
    private static BaseService mInstance;
    protected AccessibilityNodeInfo rootNodeInfo;
    /*获取窗口结点重试次数*/
    private final int MAX_RETRY_TIMES = 3;
    private int retryTimes = 0;

    protected static final int BACK = 333;
    protected static final int OPEN_WECHAT = 654;
    protected final static int CHANGE_ACTIVITY = 456;

    @SuppressLint("HandlerLeak")
    protected Handler baseHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CHANGE_ACTIVITY:
                    Intent intent = new Intent(BaseService.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    sendEmptyMessageDelayed(OPEN_WECHAT, 1000);
                    break;
                case OPEN_WECHAT:
                    PackageManager packageManager = getPackageManager();
                    Intent it = packageManager.getLaunchIntentForPackage(Constants.WECHAT_PACKAGE_NAME);
                    startActivity(it);
                    break;
            }

        }
    };


    /**
     * 获取窗口结点，直到不为空
     */
    protected void regetNode() {
        if (retryTimes >= MAX_RETRY_TIMES) {
            abandonShare();
        } else {
            retryTimes++;
            baseHandler.sendEmptyMessageDelayed(CHANGE_ACTIVITY, 1000);
        }
    }

    /**
     * Check当前辅助服务是否启用
     *
     * @param serviceName serviceName
     * @return 是否启用
     */
    private boolean checkAccessibilityEnabled(String serviceName) {
        List<AccessibilityServiceInfo> accessibilityServices =
                mAccessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : accessibilityServices) {
            if (info.getId().equals(serviceName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 前往开启辅助服务界面
     */
    public void goAccess() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    /**
     * 模拟点击事件
     *
     * @param nodeInfo nodeInfo
     */
    public void performViewClick(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return;
        }
        while (nodeInfo != null) {
            if (nodeInfo.isClickable()) {
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
            nodeInfo = nodeInfo.getParent();
        }
    }

    /**
     * 模拟返回操作
     */
    public void performBackClick() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    /**
     * 模拟下滑操作
     */
    public void performScrollBackward() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        performGlobalAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
    }

    /**
     * 模拟上滑操作
     */
    public void performScrollForward() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        performGlobalAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
    }

    /**
     * 查找对应文本的View
     *
     * @param text text
     * @return View
     */
    public AccessibilityNodeInfo findViewByText(String text) {
        return findViewByText(text, false);
    }

    /**
     * 查找对应文本的View
     *
     * @param text      text
     * @param clickable 该View是否可以点击
     * @return View
     */
    public AccessibilityNodeInfo findViewByText(String text, boolean clickable) {
        if (rootNodeInfo == null) {
            return null;
        }
        List<AccessibilityNodeInfo> nodeInfoList = rootNodeInfo.findAccessibilityNodeInfosByText(text);
        if (nodeInfoList != null && !nodeInfoList.isEmpty()) {
            for (AccessibilityNodeInfo nodeInfo : nodeInfoList) {
                if (nodeInfo != null && (nodeInfo.isClickable() == clickable)) {
                    return nodeInfo;
                }
            }
        }
        return null;
    }

    /**
     * 查找对应ID的View
     *
     * @param id id
     * @return View
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public AccessibilityNodeInfo findViewByID(String id) {
        AccessibilityNodeInfo accessibilityNodeInfo = getRootInActiveWindow();
        if (accessibilityNodeInfo == null) {
            return null;
        }
        List<AccessibilityNodeInfo> nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(id);
        if (nodeInfoList != null && !nodeInfoList.isEmpty()) {
            for (AccessibilityNodeInfo nodeInfo : nodeInfoList) {
                if (nodeInfo != null) {
                    return nodeInfo;
                }
            }
        }
        return null;
    }

    public void clickTextViewByText(String text) {
        AccessibilityNodeInfo accessibilityNodeInfo = getRootInActiveWindow();
        if (accessibilityNodeInfo == null) {
            return;
        }
        List<AccessibilityNodeInfo> nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText(text);
        if (nodeInfoList != null && !nodeInfoList.isEmpty()) {
            for (AccessibilityNodeInfo nodeInfo : nodeInfoList) {
                if (nodeInfo != null) {
                    performViewClick(nodeInfo);
                    break;
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void clickTextViewByID(String id) {
        AccessibilityNodeInfo accessibilityNodeInfo = getRootInActiveWindow();
        if (accessibilityNodeInfo == null) {
            return;
        }
        List<AccessibilityNodeInfo> nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(id);
        if (nodeInfoList != null && !nodeInfoList.isEmpty()) {
            for (AccessibilityNodeInfo nodeInfo : nodeInfoList) {
                if (nodeInfo != null) {
                    performViewClick(nodeInfo);
                    break;
                }
            }
        }
    }

    /**
     * 模拟输入
     *
     * @param nodeInfo nodeInfo
     * @param text     text
     */
    public void inputText(AccessibilityNodeInfo nodeInfo, String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("label", text);
            clipboard.setPrimaryClip(clip);
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int action = event.getEventType();
        if (action == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || action == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            rootNodeInfo = getRootInActiveWindow();
            if (rootNodeInfo == null) {
                regetNode();
            } else {
                retryTimes = 0;
                onWindowChange(event);
            }
        }
    }

    private static String TAG = "[WeChatService]";
    private final long DELAY = 500;

    private ScreenLockUtils lockScreen;

    public void onWindowChange(AccessibilityEvent event) {

        String className = event.getClassName().toString();
        WorkLine.WorkNode action = WorkLine.getNextNode();
        if (action != null) {
            if (className.contains("LauncherUI")) {// 点击“发现”

                if (action.code == WorkLine.NODE_CHOOSE_FIND_ITEM) {
                    baseHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            selectedFind();
                        }
                    }, DELAY);
                }

            } else if (className.contains("SnsTimeLineUI")) {// 朋友圈页面，点击右上角的ImageButton

                if (action.code == WorkLine.NODE_CLICK_IMAGEBTN) {
                    baseHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            clickSharePhotoImageBtn();
                        }
                    }, DELAY);
                } else if (action.code == WorkLine.RETURN) {
                    back();
                }

            } else if (TextUtils.equals(event.getClassName(), "com.tencent.mm.ui.base.k")) {// 点击“从相册选择”

                if (action.code == WorkLine.NODE_OPEN_ALBUM) {
                    baseHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            openAlbum();
                        }
                    }, DELAY);
                }

            } else if (className.contains("AlbumPreviewUI")) {// 选择照片

                if (action.code == WorkLine.NODE_SELECT_PICS) {
                    baseHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            choosePicture();
                        }
                    }, DELAY);
                }

            } else if (className.contains("SnsUploadUI")) {// 复制内容
                if (action.code == WorkLine.NODE_PASTE) {
                    baseHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            pasteContent(rootNodeInfo);
                            clickPublish();
                        }
                    }, DELAY);

                }
            }
        }
    }

    /**
     * 点击发现tab
     */
    private synchronized void selectedFind() {
        List<AccessibilityNodeInfo> findList = rootNodeInfo.findAccessibilityNodeInfosByText("发现");
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

    /**
     * 点击"朋友圈"
     */
    private void jumpToCircleOfFriends() {
        baseHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (rootNodeInfo == null) {
                    abandonShare();
                    return;
                }
                List<AccessibilityNodeInfo> list = rootNodeInfo.findAccessibilityNodeInfosByText("朋友圈");
                if (list != null && list.size() != 0) {
                    AccessibilityNodeInfo tempInfo = list.get(0);
                    if (tempInfo != null && tempInfo.getParent() != null) {
                        tempInfo.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                    WorkLine.forward();
                }
            }
        }, DELAY);
    }

    /**
     * 点击朋友圈右上角"发送朋友圈"按钮
     */
    private void clickSharePhotoImageBtn() {
        findImageBtn(rootNodeInfo);
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
     * 打开相册
     */
    private void openAlbum() {
        List<AccessibilityNodeInfo> accessibilityNodeInfoList = rootNodeInfo.findAccessibilityNodeInfosByText("从相册选择");
        traverseNode(accessibilityNodeInfoList);
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


    private boolean pasteContent(AccessibilityNodeInfo root) {
        if (root.getChildCount() == 0) {
            Log.i(TAG, "child widget----------------------------" + root.getClassName());
            String className = root.getClassName().toString();
            if (!TextUtils.isEmpty(className) && className.contains("EditText")) {
                String remove = "详情请扫描图片中的二维码";
                if (WechatTempContent.describeList.size() > 0) {
                    remove = WechatTempContent.describeList.remove(WechatTempContent.describeList.size() - 1);
                }
                inputText(root, remove);
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

    private void clickPublish() {
        WorkLine.WorkNode nextNode = WorkLine.getNextNode();
        if (nextNode != null && nextNode.code == WorkLine.NODE_SEND_WECHAT) {
            ToastUtil.show("3秒后自动分享");
            List<AccessibilityNodeInfo> list = rootNodeInfo.findAccessibilityNodeInfosByText("发表");
            for (AccessibilityNodeInfo n : list) {
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                n.recycle();
            }
//            baseHandler.sendEmptyMessageDelayed(BACK, 3000);
            ToastUtil.show("3秒后自动返回");
            rootNodeInfo.recycle();
            WorkLine.forward();
        }
    }

    /**
     * 选择发送的图片
     */
    private void choosePicture() {
        if (rootNodeInfo != null) {
            List<AccessibilityNodeInfo> accessibilityNodeInfoList = rootNodeInfo.findAccessibilityNodeInfosByText("预览");
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

            List<AccessibilityNodeInfo> finishList = rootNodeInfo.findAccessibilityNodeInfosByText("完成(" + WorkLine.size + "/9)");//点击确定
            performClickBtn(finishList);
        }
    }

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
     * 放弃改次分享
     */
    protected void abandonShare() {
        Log.w(TAG, "放弃分享...");
        WorkLine.getWorkList().clear();
        back();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        lockScreen = ScreenLockUtils.getInstance(this);
    }


    /**
     * 在朋友圈页面，返回
     */

    private void back() {
        synchronized (BaseService.class) {
            int count = 2;
            while (count > 0) {
                performBackClick();
                count--;
                try {
                    Thread.sleep(700);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            WorkLine.forward();
            notifyNextTask();
        }
    }

    /**
     * 分享完成，通知下一个任务
     */
    private void notifyNextTask() {
        lockScreen.lockScreen();
        //发送消息，下载下一个
        ImageDownloadManager.getINSTANCE().startLooper();
    }


    @Override
    public void onInterrupt() {
    }
}
