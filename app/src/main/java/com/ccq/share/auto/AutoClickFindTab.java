package com.ccq.share.auto;


import android.view.accessibility.AccessibilityNodeInfo;

/**
 * 自动点击微信"发现"tab
 */
public class AutoClickFindTab extends AbstractAuto {

    private AutoClickFindTab(IAutoService service, AccessibilityNodeInfo nodeInfo) {
        super(service, nodeInfo);
    }

    public static AutoClickFindTab getInstance(IAutoService iAutoService, AccessibilityNodeInfo nodeInfo) {
        synchronized (AutoClickFindTab.class) {
            if (INSTANCE == null) {
                INSTANCE = new AutoClickFindTab(iAutoService, nodeInfo);
            }
        }
        return INSTANCE;
    }

    @Override
    public void execute() {
        if (rootNode == null) {
            if (iAutoService != null) {
                iAutoService.rootNodeNull();
            }
        } else {
            // todo 点击发现

        }
    }

}
