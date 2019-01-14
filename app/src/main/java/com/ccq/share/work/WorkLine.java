package com.ccq.share.work;

import java.util.ArrayList;
import java.util.List;

/**************************************************
 *
 * 作者：巴银
 * 时间：2019/1/14 21:33
 * 描述：
 * 版本：
 *
 **************************************************/

public class WorkLine {
    private static List<WorkNode> workNodeList = new ArrayList<>();
    public static int NODE_CHOOSE_FIND_ITEM = 0;// 选择发现页面
    public static int NODE_CLICK_TIMELINE = 1;// 进入朋友圈页面
    public static int NODE_CLICK_IMAGEBTN = 2;// 点击从相册选择
    public static int NODE_OPEN_ALBUM = 3;// 打开相册
    public static int NODE_SELECT_PICS = 4;// 选择图片
    public static int NODE_PASTE = 5;// 粘贴内容
    public static int NODE_SEND_WECHAT = 6;// 点击发布朋友圈

    public static int size = 0;

    public static List<WorkNode> getWorkList() {
        return workNodeList;
    }

    public static WorkNode getNextNode(){
        if (workNodeList!=null && !workNodeList.isEmpty()) {
            return workNodeList.get(0);
        }
        return null;
    }

    public static boolean forward(){
        if (workNodeList !=null && !workNodeList.isEmpty()) {
            workNodeList.remove(0);
            return true;
        }
        return false;
    }

    public static synchronized void initWorkList() {
        if (workNodeList == null) workNodeList = new ArrayList<>();
        else
            workNodeList.clear();
        size = 0;
        workNodeList.add(new WorkNode(NODE_CHOOSE_FIND_ITEM, "选择发现页面"));
        workNodeList.add(new WorkNode(NODE_CLICK_TIMELINE, "进入朋友圈页面"));
        workNodeList.add(new WorkNode(NODE_CLICK_IMAGEBTN, "点击从相册选择"));
        workNodeList.add(new WorkNode(NODE_OPEN_ALBUM, "打开相册"));
        workNodeList.add(new WorkNode(NODE_SELECT_PICS, "选择图片"));
        workNodeList.add(new WorkNode(NODE_PASTE, "粘贴内容"));
        workNodeList.add(new WorkNode(NODE_SEND_WECHAT, "点击发布朋友圈"));
    }

    public static class WorkNode {
        public int code;
        public String work;

        WorkNode(int code, String work) {
            this.code = code;
            this.code = code;
            this.work = work;
        }

        @Override
        public String toString() {
            return String.format("code=%s desc=%s",code,work);
        }
    }
}
