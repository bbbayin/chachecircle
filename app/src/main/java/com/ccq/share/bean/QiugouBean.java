package com.ccq.share.bean;

/**
 * Created by Administrator on 2017/10/29.
 */

public class QiugouBean {

    /**
     * code : 0
     * message : 获取成功
     * data : {"id":84,"title":"","content":"这时请说明您的需求，文字在7-200字之间","province":1,"city":1,"address":"","phone":"13466529158","uid":182,"addtime":1509195700,"jubaocount":0,"showtype":1,"pages":1,"pic":"http://img11.miheyingua.cn/qiugou/2017/10/28/1509195701443974830.jpg"}
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
         * id : 84
         * title :
         * content : 这时请说明您的需求，文字在7-200字之间
         * province : 1
         * city : 1
         * address :
         * phone : 13466529158
         * uid : 182
         * addtime : 1509195700
         * jubaocount : 0
         * showtype : 1
         * pages : 1
         * pic : http://img11.miheyingua.cn/qiugou/2017/10/28/1509195701443974830.jpg
         */

        private int id;
        private String title;
        private String content;
        private int province;
        private int city;
        private String address;
        private String phone;
        private int uid;
        private int addtime;
        private int jubaocount;
        private int showtype;
        private int pages;
        private String pic;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public int getProvince() {
            return province;
        }

        public void setProvince(int province) {
            this.province = province;
        }

        public int getCity() {
            return city;
        }

        public void setCity(int city) {
            this.city = city;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public int getUid() {
            return uid;
        }

        public void setUid(int uid) {
            this.uid = uid;
        }

        public int getAddtime() {
            return addtime;
        }

        public void setAddtime(int addtime) {
            this.addtime = addtime;
        }

        public int getJubaocount() {
            return jubaocount;
        }

        public void setJubaocount(int jubaocount) {
            this.jubaocount = jubaocount;
        }

        public int getShowtype() {
            return showtype;
        }

        public void setShowtype(int showtype) {
            this.showtype = showtype;
        }

        public int getPages() {
            return pages;
        }

        public void setPages(int pages) {
            this.pages = pages;
        }

        public String getPic() {
            return pic;
        }

        public void setPic(String pic) {
            this.pic = pic;
        }
    }
}
