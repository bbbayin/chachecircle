package com.ccq.share.auto;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

public abstract class AbstractAuto {
    protected AccessibilityNodeInfo rootNode;
    protected String LOGTAG = "[AbstractAuto]";
    protected static AutoClickFindTab INSTANCE;
    protected IAutoService iAutoService;

    protected AbstractAuto(IAutoService service, AccessibilityNodeInfo nodeInfo) {
        LOGTAG = getClass().getSimpleName();
        if (nodeInfo == null) {
            Log.e(LOGTAG, "参数nodeInfo为空！！！");
        } else {
            rootNode = nodeInfo;
            iAutoService = service;
        }
    }

    public abstract void execute();
}
