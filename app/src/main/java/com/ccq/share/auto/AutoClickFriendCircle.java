package com.ccq.share.auto;


import android.view.accessibility.AccessibilityNodeInfo;

import com.ccq.share.auto.base.AbstractAuto;
import com.ccq.share.auto.base.IAutoService;
import com.ccq.share.work.WorkLine;

/**
 * 自动点击微信"朋友圈"按钮
 */
public class AutoClickFriendCircle extends AbstractAuto {

    public AutoClickFriendCircle(IAutoService service, AccessibilityNodeInfo nodeInfo) {
        super(service, nodeInfo);
    }

    @Override
    protected void delayRun() {
        boolean click = clickByText("朋友圈");
        if (iAutoService != null)
            if (click) {
                iAutoService.onResult(WorkLine.NODE_CLICK_TIMELINE);
            } else {
                iAutoService.rootNodeNull();
            }
    }

}
