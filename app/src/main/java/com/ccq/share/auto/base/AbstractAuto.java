package com.ccq.share.auto.base;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;


import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAuto {
    protected AccessibilityNodeInfo rootNode;
    protected String LOGTAG = "[AbstractAuto]";
    protected IAutoService iAutoService;
    protected Handler handler = new Handler(Looper.getMainLooper());
    protected final int DELAY = 500;

    public AbstractAuto(IAutoService service, AccessibilityNodeInfo nodeInfo) {
        LOGTAG = getClass().getSimpleName();
        if (nodeInfo == null) {
            Log.e(LOGTAG, "参数nodeInfo为空！！！");
        }
        rootNode = nodeInfo;
        iAutoService = service;
    }


    public void executeDelay() {
        if (checkRootNodeNotNull()) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    delayRun();
                }
            }, DELAY);

        } else {
            if (iAutoService != null) {
                iAutoService.rootNodeNull();
            }
        }
    }

    protected abstract void delayRun();

    protected boolean checkRootNodeNotNull() {
        if (rootNode == null) {
            if (iAutoService != null) {
                iAutoService.rootNodeNull();
            }
            return false;
        }
        return true;
    }

    /**
     * 根据文字查找当前页面的控件，执行点击事件
     *
     * @param text
     * @return
     */
    protected boolean clickByText(String text) {
        if (TextUtils.isEmpty(text)) {
            Log.e(LOGTAG, "参数不能为空！");
            return false;
        } else {
            if (rootNode == null) {
                Log.e(LOGTAG, "rootNode为空！！");
                return false;
            } else {
                List<AccessibilityNodeInfo> infosByText = rootNode.findAccessibilityNodeInfosByText(text);
                if (infosByText == null || infosByText.size() == 0) {
                    Log.e(LOGTAG, String.format("未找到[%s]结点", text));
                    return false;
                } else {
                    for (int i = 0; i < infosByText.size(); i++) {
                        performClickNode(infosByText.get(i));
                    }
                    return true;
                }
            }
        }
    }

    /**
     * 根据类名查找node
     *
     * @param node
     * @param viewName
     * @return
     */
    protected AccessibilityNodeInfo findViewByName(AccessibilityNodeInfo node, String viewName) {
        if (node == null) {
            return null;
        } else {
            if (node.getChildCount() == 0) {
                String className = node.getClassName().toString();
                if (!TextUtils.isEmpty(className) && className.contains(viewName)) {
                    return node;
                }
            } else {
                for (int i = 0; i < node.getChildCount(); i++) {
                    if (node.getChild(i) != null) {
                        if (findViewByName(node.getChild(i), viewName) != null) {
                            return node.getChild(i);
                        }
                    }
                }
            }
        }
        return null;
    }

    protected void findViewsByName(AccessibilityNodeInfo node, String viewName, List<AccessibilityNodeInfo> list) {
        if (list == null) {
            list = new ArrayList<>();
        }
        if (node == null) {
            return;
        } else {
            if (node.getChildCount() == 0) {
                String className = node.getClassName().toString();
                if (!TextUtils.isEmpty(className) && className.contains(viewName)) {
                    Log.d(LOGTAG, "findViewsByName() : id = " + node.getViewIdResourceName());
                    list.add(node);
                }
            } else {
                for (int i = 0; i < node.getChildCount(); i++) {
                    if (node.getChild(i) != null) {
                        if (findViewByName(node.getChild(i), viewName) != null) {
                            list.add(node.getChild(i));
                        }
                    }
                }
            }
        }
    }

    protected boolean performClick(List<AccessibilityNodeInfo> accessibilityNodeInfoList) {
        if (accessibilityNodeInfoList != null && accessibilityNodeInfoList.size() != 0) {
            for (int i = 0; i < accessibilityNodeInfoList.size(); i++) {
                performClickNode(accessibilityNodeInfoList.get(i));
            }
        }
        return false;
    }

    protected void performClickNode(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            if (nodeInfo.getParent() != null) {
                nodeInfo.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }
}
