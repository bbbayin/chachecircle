package com.ccq.share;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
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
    private String PUBLISH_MOMENT = "com.tencent.mm.plugin.sns.ui.SnsUploadUI";

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
        System.out.println("当前class --- " + className);
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
                // 分享的第一步判断，如果不是微信首页直接退出此次分享
//                if (action.code == WorkLine.NODE_CHOOSE_FIND_ITEM) {
//                    // 1. 聊天页面(com.tencent.mm.ui.LauncherUI)，判断是否含有edittext
//                    // 2.
//                    if (isPageInChat(accessibilityNodeInfo)) {
//                        Log.i(TAG, "当前是聊天页面，返回");
//                        performGlobalAction(GLOBAL_ACTION_BACK);
//                        createWindowChangeEvent();
//                        return;
//                    }
//                }

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
                } else if (TextUtils.equals(event.getClassName(), "com.tencent.mm.ui.widget.dialog.l")) {// 点击“从相册选择”
                    if (action.code == WorkLine.NODE_OPEN_ALBUM) {
                        ToastUtil.show(action.work);
                        openAlbum();
                    }
                } else if (TextUtils.equals(className, albumPageName)) {// 选择照片
                    if (action.code == WorkLine.NODE_SELECT_PICS) {
                        ToastUtil.show("2秒后自动选择图片");
                        choosePic2();
                    }
                } else if (PUBLISH_MOMENT.equals(className)) {// 发送朋友圈
                    if (isPasting) return;
                    System.out.println("发布朋友圈，粘贴");
                    ToastUtil.show(action.work);
                    if (action.code == WorkLine.NODE_PASTE) {
                        isPasting = true;
                        ToastUtil.show(action.work);
                        if (isRootNodeNotNull("复制内容-点击发表")) {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    // 复制 + 发布
                                    pasteById(accessibilityNodeInfo);
                                }
                            }, 2000);
                        }
                    }
                }
            }
        }
    }


    private void launchWeChat() {
        Intent intent = new Intent();
        ComponentName componentName = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI");
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(componentName);
        startActivity(intent);
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


    private boolean isPageInChat(AccessibilityNodeInfo root) {
        if (root == null) return false;
        if (root.getChildCount() == 0) {
            CharSequence widgetName = root.getClassName();
            if (widgetName != null) {
                if (!TextUtils.isEmpty(widgetName.toString()) && widgetName.toString().contains("EditText")) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < root.getChildCount(); i++) {
                if (root.getChild(i) != null) {
                    if (isPageInChat(root.getChild(i))) {
                        return true;
                    }
                }
            }
        }
        return false;
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
            if (!TextUtils.isEmpty(className) && className.contains("ImageView")) {
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

    private void choosePic2() {
        // com.tencent.mm:id/gpy
        if (isRootNodeNotNull("选择图片2")) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    List<AccessibilityNodeInfo> checklist = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/gpy");
                    if (checklist != null && checklist.size() > 0) {
                        for (int i = 0; i < WorkLine.size; i++) {
                            AccessibilityNodeInfo node = checklist.get(i);
                            if (node != null && node.getClassName().equals("android.widget.CheckBox")) {
                                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);//选中图片
                            }
                        }
                        final List<AccessibilityNodeInfo> finishList = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/en");//点击确定
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                performClickBtn(finishList);
                            }
                        }, 2000);
                    } else {
                        step("选择图片");
                    }
                }
            }, 2000);
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
     * 从相册选择
     */
    private void openAlbum() {
        if (isRootNodeNotNull("从相册选择")) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    List<AccessibilityNodeInfo> accessibilityNodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("从相册选择");
                    boolean b = traverseNode(accessibilityNodeInfoList);
                    if (b) {
                        WorkLine.forward();
                    } else {
                        step("从相册选择");
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

    volatile boolean isPasting = false;

    private synchronized void pasteById(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> editTextNodes = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/jsy");
        if (editTextNodes != null && editTextNodes.size() > 0) {
            for (AccessibilityNodeInfo node : editTextNodes) {
                if (node.getClassName().equals("android.widget.EditText")) {
                    Bundle arguments = new Bundle();
                    String remove = "详情请扫描图片中的二维码";
                    if (WechatTempContent.describeList.size() > 0) {
                        remove = WechatTempContent.describeList.remove(WechatTempContent.describeList.size() - 1);
                    }
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, remove);
                        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                    } else {
                        ClipboardManager clipService = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData text = ClipData.newPlainText("text", remove);
                        clipService.setPrimaryClip(text);
                        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                        node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                    }

                    // 发布
                    sendWeChat();
                    isPasting = false;
                    break;
                }
            }
        }
    }

    private synchronized void sendWeChat() {
        if (isRootNodeNotNull("复制内容-点击发送")) {
            clickPublish();
        }
    }

    private void clickPublish() {
        ToastUtil.show("自动分享");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                List<AccessibilityNodeInfo> publishList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("发表");
                if (publishList != null && publishList.size() > 0) {
                    publishList.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }else {
                    List<AccessibilityNodeInfo> list = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/en");
                    for (AccessibilityNodeInfo n : list) {
                        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        n.recycle();
                    }
                }
                ToastUtil.show("自动返回");
                accessibilityNodeInfo.recycle();
                WorkLine.forward();
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
        }, 1000);
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
