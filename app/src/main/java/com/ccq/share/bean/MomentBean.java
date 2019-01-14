package com.ccq.share.bean;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/****************************************
 * 功能说明:  
 *
 * Author: Created by bayin on 2017/8/28.
 ****************************************/

public class MomentBean {
    private String information;
    private ArrayList<Uri> uriList;

    public String getInformation() {
        return information;
    }

    public void setInformation(String information) {
        this.information = information;
    }

    public ArrayList<Uri> getUriList() {
        return uriList;
    }

    public void setUriList(ArrayList<Uri> uriList) {
        this.uriList = uriList;
    }
}
