package com.ccq.share;

import android.os.Environment;

import java.io.File;

/**
 * Created by Administrator on 2017/8/27.
 */

public class Constants {

    public static String SD_ROOTPATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ccq";

    static {
        File file = new File(SD_ROOTPATH);
        if (!file.exists()) file.mkdir();
    }

    //测试环境
//    public static String download_baseUrl = "http://img11.miheyingua.cn/";
//    public static String baseUrl = "https://apicheck.chanchequan.com/";
    //正式环境
    public static String baseUrl = "https://api.chanchequan.com/";
    public static String download_baseUrl = "http://img10.chanchequan.com/";
    // 求购铲车默认图片
    public static String qiugou_img_url = "http://img10.chanchequan.com/static/image/qiugou.jpg";


    public static String KEY_WECHAT_CONTENT = "key_wechat_content";
    public static String KEY_QIUGOU_END = "key_qiugou_end";//求购的小尾巴

    public static String WECHAT_SHAREUI_NAME = "com.tencent.mm.ui.tools.ShareToTimeLineUI";

    public static String WECHAT_PACKAGE_NAME = "com.tencent.mm";

    public static String USER = "guest";
    public static String PASS = "guest";
    public static String TIME = "123456";

    public static String KEY_PICS_URL = "key_pics_url";//图片列表的uri

    public static boolean isAutoShare = false;//是否自动分享

    public static String key_TOKEN = "key_token";

    public static String SHARE_FINISH_ACTION = "com.chanchequan.share.FINISH";//分享成功的广播

    //要下载的图片url ，要分享的内容
    public static String KEY_SHARE_METE_DATA = "key_share_mete_data";

    public static String TYPE_CAR = "car";//分享卖车信息
    public static String TYPE_BUYER = "qiugou";//分享求购信息

    public static String KEY_DELAY_TIME = "key_delay_time";
}
