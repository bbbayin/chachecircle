package com.ccq.share.auto;

import android.view.accessibility.AccessibilityNodeInfo;

import com.ccq.share.auto.base.AbstractAuto;
import com.ccq.share.auto.base.IAutoService;
import com.ccq.share.work.WorkLine;

import java.util.List;

/**
 * 自动选中图片
 */
public class AutoSelectPicture extends AbstractAuto {
    public AutoSelectPicture(IAutoService service, AccessibilityNodeInfo nodeInfo) {
        super(service, nodeInfo);
    }

    @Override
    protected void delayRun() {
        List<AccessibilityNodeInfo> accessibilityNodeInfoList = rootNode.findAccessibilityNodeInfosByText("预览");
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

        List<AccessibilityNodeInfo> finishList = rootNode.findAccessibilityNodeInfosByText("完成(" + WorkLine.size + "/9)");//点击确定
        performClick(finishList);

        if (iAutoService!=null) {
            iAutoService.onResult(WorkLine.NODE_SELECT_PICS);
        }
    }
}
