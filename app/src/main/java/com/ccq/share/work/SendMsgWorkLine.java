package com.ccq.share.work;

import android.util.Log;

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

public class SendMsgWorkLine {
    private static List<WorkNode> workNodeList = new ArrayList<>();
//    public final static int NODE_CLICK_WECHAT_TAB = 7;// 点击微信tab
    public final static int NODE_CLICK_CHAT_ITEM = 0;// 点击聊天item
    public final static int NODE_CLICK_ADD_BTN = 1;// 点击聊天加号按钮
    public final static int NODE_OPEN_ALBUM = 2;//点击相册
    public final static int NODE_SELECT_PICS = 3;// 选择图片
    public final static int NODE_PASTE = 4;// 粘贴内容
//    public final static int NODE_CLICK_SEND = 5;// 发送文字
    public final static int RETURN = 6;// 返回

    public static int size = 0;

    public static List<WorkNode> getWorkList() {
        return workNodeList;
    }

    public static WorkNode getNextNode() {
        if (workNodeList != null && !workNodeList.isEmpty()) {
            return workNodeList.get(0);
        }
        return null;
    }

    public static boolean forward() {
        if (workNodeList != null && !workNodeList.isEmpty()) {
            WorkNode remove = workNodeList.remove(0);
            Log.d("任务完成", remove.work);
            return true;
        }
        return false;
    }

    public static void clear(){
        if (workNodeList != null) workNodeList.clear();
    }

    public static synchronized void initWorkList() {
        if (workNodeList == null) workNodeList = new ArrayList<>();
        else
            workNodeList.clear();
        size = 0;
//        workNodeList.add(new WorkNode(NODE_CLICK_WECHAT_TAB, "点击微信"));
        workNodeList.add(new WorkNode(NODE_CLICK_CHAT_ITEM, "点击聊天item"));
        workNodeList.add(new WorkNode(NODE_PASTE, "粘贴内容"));
//        workNodeList.add(new WorkNode(NODE_CLICK_SEND, "发送文字"));
        workNodeList.add(new WorkNode(NODE_CLICK_ADD_BTN, "点击加号按钮"));
        workNodeList.add(new WorkNode(NODE_OPEN_ALBUM, "点击相册"));
        workNodeList.add(new WorkNode(NODE_SELECT_PICS, "选择图片"));
//        workNodeList.add(new WorkNode(RETURN, "发送成功，返回"));
//        workNodeList.add(new WorkNode(RETURN, "返回桌面"));
    }

    public static void remove(int node) {
        for (WorkNode workNode :
                workNodeList) {
            if (workNode.code == node) {
                workNodeList.remove(workNode);
                break;
            }
        }
    }

    public static class WorkNode {
        public int code;
        public String work;

        WorkNode(int code, String work) {
            this.code = code;
            this.work = work;
        }

        @Override
        public String toString() {
            return String.format("code=%s desc=%s", code, work);
        }
    }
}
