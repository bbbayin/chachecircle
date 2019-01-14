package com.ccq.share.bean;

/****************************************
 * 功能说明:  
 *
 * Author: Created by bayin on 2017/9/11.
 ****************************************/

public class PushBean {
    public static final int TYPE_SELL = 0;//出售
    public static final  int TYPE_BUY = 1;//求购
    private String userid;
    private String carid;
    private int type = 0;//0：出售，1：求购
    private String id;
    public PushBean(int type, String id) {
        this.type = type;
        this.id = id;
    }

    public PushBean(int type, String userid, String carid) {
        this.type = type;
        this.userid = userid;
        this.carid = carid;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getCarid() {
        return carid;
    }

    public void setCarid(String carid) {
        this.carid = carid;
    }
}
