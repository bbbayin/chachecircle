package com.ccq.share.bean;

import java.util.List;

/**
 * 车辆信息bean
 *
 * Created by Administrator on 2017/8/27.
 */

public class CarInfoBean {

    /**
     * address :
     * addtime : 1503823170
     * addtime_format : 5小时前
     * cityName : 北京市
     * content :
     * count : 1
     * id : 874
     * name : 龙工855N装载机
     * phone : 18600175665
     * pic_img : [{"id":"5512","savename":"http://img11.miheyingua.cn/2017/08/27/20170827163924138.jpg"},{"id":"5513","savename":"http://img11.miheyingua.cn/2017/08/27/20170827163925352.jpg"},{"id":"5514","savename":"http://img11.miheyingua.cn/2017/08/27/20170827163926777.jpg"},{"id":"5515","savename":"http://img11.miheyingua.cn/2017/08/27/20170827163926510.jpg"},{"id":"5516","savename":"http://img11.miheyingua.cn/2017/08/27/20170827163927615.jpg"},{"id":"5517","savename":"http://img11.miheyingua.cn/2017/08/27/20170827163928451.jpg"}]
     * pic_img_count : 6
     * price : 0
     * provinceName : 北京市
     * type : 0
     * userInfo : {"userid":169,"mobile":"18600175665","dealer":0,"vip":0,"htime":0,"jxsendtime":0,"province":1,"city":1,"address":"","jingdu":"39.812614","weidu":"","nickname":"A0000000二手装载机","headimgurl":"https://wx.qlogo.cn/mmopen/vi_32/FNaOpaf1tTjUW8GI4JicG892wE7ia1vPdHsrYFpaQEFfiakJ1BcAPjld8yMGvbWeEbo1u1olW51ESqmFVzGu3iapgA/0","refcount":0,"refdate":{},"provinceName":"北京市","cityName":"北京市","isBusiness":false,"isAuthentication":false,"isMember":false,"openid":"o7-IJwda3J_Gh16bK6ecvo2PlRkU","ktime":0}
     * year : 2012
     */

    private String address;
    private int addtime;
    private String addtime_format;
    private String cityName;
    private String content;
    private int count;
    private int id;
    private String name;
    private String phone;
    private int pic_img_count;
    private String price;
    private String provinceName;
    private int type;
    private UserInfoBean userInfo;
    private int year;
    private java.util.List<PicImgBean> pic_img;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getAddtime() {
        return addtime;
    }

    public void setAddtime(int addtime) {
        this.addtime = addtime;
    }

    public String getAddtime_format() {
        return addtime_format;
    }

    public void setAddtime_format(String addtime_format) {
        this.addtime_format = addtime_format;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getPic_img_count() {
        return pic_img_count;
    }

    public void setPic_img_count(int pic_img_count) {
        this.pic_img_count = pic_img_count;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public UserInfoBean getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfoBean userInfo) {
        this.userInfo = userInfo;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public List<PicImgBean> getPic_img() {
        return pic_img;
    }

    public void setPic_img(List<PicImgBean> pic_img) {
        this.pic_img = pic_img;
    }

    public class UserInfoBean{
        /**
         * userid : 169
         * mobile : 18600175665
         * dealer : 0
         * vip : 0
         * htime : 0
         * jxsendtime : 0
         * province : 1
         * city : 1
         * address :
         * jingdu : 39.812614
         * weidu :
         * nickname : A0000000二手装载机
         * headimgurl : https://wx.qlogo.cn/mmopen/vi_32/FNaOpaf1tTjUW8GI4JicG892wE7ia1vPdHsrYFpaQEFfiakJ1BcAPjld8yMGvbWeEbo1u1olW51ESqmFVzGu3iapgA/0
         * refcount : 0
         * refdate : {}
         * provinceName : 北京市
         * cityName : 北京市
         * isBusiness : false
         * isAuthentication : false
         * isMember : false
         * openid : o7-IJwda3J_Gh16bK6ecvo2PlRkU
         * ktime : 0
         */

        private int userid;
        private String headimgurl;
        private String nickname;

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public int getUserid() {
            return userid;
        }

        public void setUserid(int userid) {
            this.userid = userid;
        }

        public String getHeadimgurl() {
            return headimgurl;
        }

        public void setHeadimgurl(String headimgurl) {
            this.headimgurl = headimgurl;
        }
    }


    public class PicImgBean{

        /**
         * id : 5488
         * savename : http://img11.miheyingua.cn/2017/08/27/20170827163733385.jpg
         */

        private String id;
        private String savename;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSavename() {
            return savename;
        }

        public void setSavename(String savename) {
            this.savename = savename;
        }
    }
}
