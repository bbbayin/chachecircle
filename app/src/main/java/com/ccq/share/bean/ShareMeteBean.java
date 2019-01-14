package com.ccq.share.bean;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by Administrator on 2017/9/22.
 *
 * 图片的url，分享内容
 */

public class ShareMeteBean implements Parcelable{
    private List<String> urlList;
    private String shareContent;
    private String waterImage;
    private String nickname;
    private String desc;
    private String headImg;

    public ShareMeteBean(List<String> urlList, String shareContent, String waterImage, String nickname, String desc, String headImg) {
        this.urlList = urlList;
        this.shareContent = shareContent;
        this.waterImage = waterImage;
        this.nickname = nickname;
        this.desc = desc;
        this.headImg = headImg;
    }

    protected ShareMeteBean(Parcel in) {
        urlList = in.createStringArrayList();
        shareContent = in.readString();
        waterImage = in.readString();
        nickname = in.readString();
        desc = in.readString();
        headImg = in.readString();
    }

    public static final Creator<ShareMeteBean> CREATOR = new Creator<ShareMeteBean>() {
        @Override
        public ShareMeteBean createFromParcel(Parcel in) {
            return new ShareMeteBean(in);
        }

        @Override
        public ShareMeteBean[] newArray(int size) {
            return new ShareMeteBean[size];
        }
    };

    public List<String> getUrlList() {
        return urlList;
    }

    public void setUrlList(List<String> urlList) {
        this.urlList = urlList;
    }

    public String getShareContent() {
        return shareContent;
    }

    public void setShareContent(String shareContent) {
        this.shareContent = shareContent;
    }

    public String getWaterImage() {
        return waterImage;
    }

    public void setWaterImage(String waterImage) {
        this.waterImage = waterImage;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getHeadImg() {
        return headImg;
    }

    public void setHeadImg(String headImg) {
        this.headImg = headImg;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(urlList);
        dest.writeString(shareContent);
        dest.writeString(waterImage);
        dest.writeString(nickname);
        dest.writeString(desc);
        dest.writeString(headImg);
    }
}
