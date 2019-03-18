package com.ccq.share.auto;

import android.view.accessibility.AccessibilityNodeInfo;

import com.ccq.share.auto.base.AbstractAuto;
import com.ccq.share.auto.base.IAutoService;
import com.ccq.share.work.WorkLine;

import java.util.ArrayList;
import java.util.List;

public class AutoClickRightTopButton extends AbstractAuto {
    public AutoClickRightTopButton(IAutoService service, AccessibilityNodeInfo nodeInfo) {
        super(service, nodeInfo);
    }

    @Override
    protected void delayRun() {
        List<AccessibilityNodeInfo> list = new ArrayList<>();
        findViewsByName(rootNode, "ImageButton", list);

//        AccessibilityNodeInfo imageButton = findViewByName(rootNode, "ImageButton");
        if (list.size() > 0) {
            performClick(list);
            if (iAutoService != null) {
                iAutoService.onResult(WorkLine.NODE_CLICK_IMAGEBTN);
            }
        } else {
            if (iAutoService != null) {
                iAutoService.rootNodeNull();
            }
        }
    }
}
