package com.ccq.share.auto;


import android.view.accessibility.AccessibilityNodeInfo;

import com.ccq.share.auto.base.AbstractAuto;
import com.ccq.share.auto.base.IAutoService;
import com.ccq.share.work.WorkLine;

/**
 * 自动点击微信"发现"tab
 */
public class AutoClickFindTab extends AbstractAuto {

    public AutoClickFindTab(IAutoService service, AccessibilityNodeInfo nodeInfo) {
        super(service, nodeInfo);
    }

    @Override
    protected void delayRun() {
        boolean click = clickByText("发现");
        if (iAutoService != null)
            if (click) {
                iAutoService.onResult(WorkLine.NODE_CHOOSE_FIND_ITEM);
            } else {
                iAutoService.rootNodeNull();
            }
    }
}
