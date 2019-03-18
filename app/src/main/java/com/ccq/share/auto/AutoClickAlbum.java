package com.ccq.share.auto;


import android.view.accessibility.AccessibilityNodeInfo;

import com.ccq.share.auto.base.AbstractAuto;
import com.ccq.share.auto.base.IAutoService;
import com.ccq.share.work.WorkLine;

/**
 * 自动点击dialog中的"打开相册"
 */
public class AutoClickAlbum extends AbstractAuto {

    public AutoClickAlbum(IAutoService service, AccessibilityNodeInfo nodeInfo) {
        super(service, nodeInfo);
    }

    @Override
    protected void delayRun() {
        // 点击打开相册
        boolean click = clickByText("从相册选择");
        if (iAutoService != null)
            if (click) {
                iAutoService.onResult(WorkLine.NODE_OPEN_ALBUM);
            } else {
                iAutoService.rootNodeNull();
            }
    }
}
