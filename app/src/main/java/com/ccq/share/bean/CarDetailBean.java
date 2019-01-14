package com.ccq.share.bean;

import java.util.List;

/****************************************
 * 功能说明:  
 *
 * Author: Created by bayin on 2017/8/31.
 ****************************************/

public class CarDetailBean {

    /**
     * code : 0
     * message : 获取成功
     * data : {"BrandName":"其他","CImages":[{"id":"103213","savename":"http://img10.chanchequan.com/upload/2017/08/30/1504102333895677949.jpg"},{"id":"103214","savename":"http://img10.chanchequan.com/upload/2017/08/30/1504102333898548053.jpg"},{"id":"103215","savename":"http://img10.chanchequan.com/upload/2017/08/30/1504102333924848540.jpg"},{"id":"103216","savename":"http://img10.chanchequan.com/upload/2017/08/30/1504102333925307438.jpg"},{"id":"103217","savename":"http://img10.chanchequan.com/upload/2017/08/30/1504102333961447529.jpg"},{"id":"103218","savename":"http://img10.chanchequan.com/upload/2017/08/30/1504102333967682431.jpg"},{"id":"103219","savename":"http://img10.chanchequan.com/upload/2017/08/30/1504102333971557320.jpg"}],"CityName":"济南市","NumberName":"","ProvinceName":"山东省","TonnageName":"50系","address":"山东省济南市历城区","addtime":1504102406,"b_id":14,"city":147,"city_c":null,"city_p":null,"content":"15年德工956车主改行急售","count":1,"hot":0,"id":10714,"isref":0,"isshow":1,"jubaocount":0,"n_id":0,"name":"其他50系装载机","nothomepage":0,"pages":1,"phone":"15054146999","pic":"103219,103213,103218,103216,103215,103214,103217","pic_img_count":7,"picfmid":103219,"price":13.5,"province":16,"showtype":1,"showzhiding":0,"shuiyin":null,"t_id":1,"tuijian":0,"type":0,"userid":3683,"y_id":null,"year":2015,"zdty_all":0,"zdty_all_admin":0,"zdty_all_time":0,"zdty_c":0,"zdty_c_admin":0,"zdty_c_time":0,"zdty_p":0,"zdty_p_admin":0,"zdty_p_time":0,"zhiding":null}
     */

    private int code;
    private String message;
    private DataBean data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean {
        /**
         * BrandName : 其他
         * CImages : [{"id":"103213","savename":"http://img10.chanchequan.com/upload/2017/08/30/1504102333895677949.jpg"},{"id":"103214","savename":"http://img10.chanchequan.com/upload/2017/08/30/1504102333898548053.jpg"},{"id":"103215","savename":"http://img10.chanchequan.com/upload/2017/08/30/1504102333924848540.jpg"},{"id":"103216","savename":"http://img10.chanchequan.com/upload/2017/08/30/1504102333925307438.jpg"},{"id":"103217","savename":"http://img10.chanchequan.com/upload/2017/08/30/1504102333961447529.jpg"},{"id":"103218","savename":"http://img10.chanchequan.com/upload/2017/08/30/1504102333967682431.jpg"},{"id":"103219","savename":"http://img10.chanchequan.com/upload/2017/08/30/1504102333971557320.jpg"}]
         * CityName : 济南市
         * NumberName :
         * ProvinceName : 山东省
         * TonnageName : 50系
         * address : 山东省济南市历城区
         * addtime : 1504102406
         * b_id : 14
         * city : 147
         * city_c : null
         * city_p : null
         * content : 15年德工956车主改行急售
         * count : 1
         * hot : 0
         * id : 10714
         * isref : 0
         * isshow : 1
         * jubaocount : 0
         * n_id : 0
         * name : 其他50系装载机
         * nothomepage : 0
         * pages : 1
         * phone : 15054146999
         * pic : 103219,103213,103218,103216,103215,103214,103217
         * pic_img_count : 7
         * picfmid : 103219
         * price : 13.5
         * province : 16
         * showtype : 1
         * showzhiding : 0
         * shuiyin : null
         * t_id : 1
         * tuijian : 0
         * type : 0
         * userid : 3683
         * y_id : null
         * year : 2015
         * zdty_all : 0
         * zdty_all_admin : 0
         * zdty_all_time : 0
         * zdty_c : 0
         * zdty_c_admin : 0
         * zdty_c_time : 0
         * zdty_p : 0
         * zdty_p_admin : 0
         * zdty_p_time : 0
         * zhiding : null
         */

        private String BrandName;
        private String CityName;
        private String NumberName;
        private String ProvinceName;
        private String WaterImg;
        private String TonnageName;
        private String address;
        private String addtime;
        private String b_id;
        private String city;
        private Object city_c;
        private Object city_p;
        private String content;
        private String count;
        private String hot;
        private String id;
        private String isref;
        private String isshow;
        private String jubaocount;
        private String n_id;
        private String name;
        private String nothomepage;
        private String pages;
        private String phone;
        private String pic;
        private String pic_img_count;
        private String picfmid;
        private double price;
        private String province;
        private String showtype;
        private String showzhiding;
        private Object shuiyin;
        private String t_id;
        private String tuijian;
        private String type;
        private String userid;
        private Object y_id;
        private String year;
        private String zdty_all;
        private String zdty_all_admin;
        private String zdty_all_time;
        private String zdty_c;
        private String zdty_c_admin;
        private String zdty_c_time;
        private String zdty_p;
        private String zdty_p_admin;
        private String zdty_p_time;
        private Object zhiding;
        private List<CImagesBean> CImages;
        private UserInfo UserInfo;

        public DataBean.UserInfo getUserInfo() {
            return UserInfo;
        }

        public void setUserInfo(DataBean.UserInfo userInfo) {
            UserInfo = userInfo;
        }

        public void setWaterImage(String waterImage){
            this.WaterImg = waterImage;
        }
        public String getWaterImage(){
            return WaterImg;
        }

        public String getBrandName() {
            return BrandName;
        }

        public void setBrandName(String BrandName) {
            this.BrandName = BrandName;
        }

        public String getCityName() {
            return CityName;
        }

        public void setCityName(String CityName) {
            this.CityName = CityName;
        }

        public String getNumberName() {
            return NumberName;
        }

        public void setNumberName(String NumberName) {
            this.NumberName = NumberName;
        }

        public String getProvinceName() {
            return ProvinceName;
        }

        public void setProvinceName(String ProvinceName) {
            this.ProvinceName = ProvinceName;
        }

        public String getTonnageName() {
            return TonnageName;
        }

        public void setTonnageName(String TonnageName) {
            this.TonnageName = TonnageName;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getAddtime() {
            return addtime;
        }

        public void setAddtime(String addtime) {
            this.addtime = addtime;
        }

        public String getB_id() {
            return b_id;
        }

        public void setB_id(String b_id) {
            this.b_id = b_id;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public Object getCity_c() {
            return city_c;
        }

        public void setCity_c(Object city_c) {
            this.city_c = city_c;
        }

        public Object getCity_p() {
            return city_p;
        }

        public void setCity_p(Object city_p) {
            this.city_p = city_p;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getCount() {
            return count;
        }

        public void setCount(String count) {
            this.count = count;
        }

        public String getHot() {
            return hot;
        }

        public void setHot(String hot) {
            this.hot = hot;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getIsref() {
            return isref;
        }

        public void setIsref(String isref) {
            this.isref = isref;
        }

        public String getIsshow() {
            return isshow;
        }

        public void setIsshow(String isshow) {
            this.isshow = isshow;
        }

        public String getJubaocount() {
            return jubaocount;
        }

        public void setJubaocount(String jubaocount) {
            this.jubaocount = jubaocount;
        }

        public String getN_id() {
            return n_id;
        }

        public void setN_id(String n_id) {
            this.n_id = n_id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNothomepage() {
            return nothomepage;
        }

        public void setNothomepage(String nothomepage) {
            this.nothomepage = nothomepage;
        }

        public String getPages() {
            return pages;
        }

        public void setPages(String pages) {
            this.pages = pages;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getPic() {
            return pic;
        }

        public void setPic(String pic) {
            this.pic = pic;
        }

        public String getPic_img_count() {
            return pic_img_count;
        }

        public void setPic_img_count(String pic_img_count) {
            this.pic_img_count = pic_img_count;
        }

        public String getPicfmid() {
            return picfmid;
        }

        public void setPicfmid(String picfmid) {
            this.picfmid = picfmid;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public String getProvince() {
            return province;
        }

        public void setProvince(String province) {
            this.province = province;
        }

        public String getShowtype() {
            return showtype;
        }

        public void setShowtype(String showtype) {
            this.showtype = showtype;
        }

        public String getShowzhiding() {
            return showzhiding;
        }

        public void setShowzhiding(String showzhiding) {
            this.showzhiding = showzhiding;
        }

        public Object getShuiyin() {
            return shuiyin;
        }

        public void setShuiyin(Object shuiyin) {
            this.shuiyin = shuiyin;
        }

        public String getT_id() {
            return t_id;
        }

        public void setT_id(String t_id) {
            this.t_id = t_id;
        }

        public String getTuijian() {
            return tuijian;
        }

        public void setTuijian(String tuijian) {
            this.tuijian = tuijian;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUserid() {
            return userid;
        }

        public void setUserid(String userid) {
            this.userid = userid;
        }

        public Object getY_id() {
            return y_id;
        }

        public void setY_id(Object y_id) {
            this.y_id = y_id;
        }

        public String getYear() {
            return year;
        }

        public void setYear(String year) {
            this.year = year;
        }

        public String getZdty_all() {
            return zdty_all;
        }

        public void setZdty_all(String zdty_all) {
            this.zdty_all = zdty_all;
        }

        public String getZdty_all_admin() {
            return zdty_all_admin;
        }

        public void setZdty_all_admin(String zdty_all_admin) {
            this.zdty_all_admin = zdty_all_admin;
        }

        public String getZdty_all_time() {
            return zdty_all_time;
        }

        public void setZdty_all_time(String zdty_all_time) {
            this.zdty_all_time = zdty_all_time;
        }

        public String getZdty_c() {
            return zdty_c;
        }

        public void setZdty_c(String zdty_c) {
            this.zdty_c = zdty_c;
        }

        public String getZdty_c_admin() {
            return zdty_c_admin;
        }

        public void setZdty_c_admin(String zdty_c_admin) {
            this.zdty_c_admin = zdty_c_admin;
        }

        public String getZdty_c_time() {
            return zdty_c_time;
        }

        public void setZdty_c_time(String zdty_c_time) {
            this.zdty_c_time = zdty_c_time;
        }

        public String getZdty_p() {
            return zdty_p;
        }

        public void setZdty_p(String zdty_p) {
            this.zdty_p = zdty_p;
        }

        public String getZdty_p_admin() {
            return zdty_p_admin;
        }

        public void setZdty_p_admin(String zdty_p_admin) {
            this.zdty_p_admin = zdty_p_admin;
        }

        public String getZdty_p_time() {
            return zdty_p_time;
        }

        public void setZdty_p_time(String zdty_p_time) {
            this.zdty_p_time = zdty_p_time;
        }

        public Object getZhiding() {
            return zhiding;
        }

        public void setZhiding(Object zhiding) {
            this.zhiding = zhiding;
        }

        public List<CImagesBean> getCImages() {
            return CImages;
        }

        public void setCImages(List<CImagesBean> CImages) {
            this.CImages = CImages;
        }

        public static class CImagesBean {
            /**
             * id : 103213
             * savename : http://img10.chanchequan.com/upload/2017/08/30/1504102333895677949.jpg
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

        public static class UserInfo{
            private String nickName;
            private String headImg;

            public String getNickName() {
                return nickName;
            }

            public void setNickName(String nickName) {
                this.nickName = nickName;
            }

            public String getHeadImg() {
                return headImg;
            }

            public void setHeadImg(String headImg) {
                this.headImg = headImg;
            }
        }
    }
}
