package com.ccq.share;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.ccq.share.bean.CarDetailBean;
import com.ccq.share.bean.PushBean;
import com.ccq.share.utils.SpUtils;
import com.umeng.commonsdk.UMConfigure;
import com.umeng.message.IUmengRegisterCallback;
import com.umeng.message.PushAgent;

import java.util.ArrayList;
import java.util.List;

/****************************************
 * 功能说明:  
 *
 * Author: Created by bayin on 2017/8/28.
 ****************************************/

public class MyApp extends Application {

    public static List<PushBean> sShareDataSource;//推送来的数据
    public static boolean isLocked = false;//是否在分享过程中
    private static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        sShareDataSource = new ArrayList<>();
        // bugly
//        CrashReport.initCrashReport(this,"a4037bb8da",true);

        // 在此处调用基础组件包提供的初始化函数 相应信息可在应用管理 -> 应用信息 中找到 http://message.umeng.com/list/apps
        // 参数一：当前上下文context；
        // 参数二：应用申请的Appkey（需替换）；
        // 参数三：渠道名称；
        // 参数四：设备类型，必须参数，传参数为UMConfigure.DEVICE_TYPE_PHONE则表示手机；传参数为UMConfigure.DEVICE_TYPE_BOX则表示盒子；默认为手机；
        // 参数五：Push推送业务的secret 填充Umeng Message Secret对应信息（需替换）
        UMConfigure.init(this, "59a6bf86310c935cd1000c8f", "Umeng", UMConfigure.DEVICE_TYPE_PHONE, "a7bfd89c2805bcd71a3b1667bb0420bb");

        PushAgent mPushAgent = PushAgent.getInstance(this);
        //注册推送服务，每次调用register方法都会回调该接口
        mPushAgent.register(new IUmengRegisterCallback() {

            @Override
            public void onSuccess(String deviceToken) {
                //注册成功会返回device token
                Log.d("mytoken", deviceToken);
                SpUtils.put(getApplicationContext(), Constants.key_TOKEN, deviceToken);
            }

            @Override
            public void onFailure(String s, String s1) {
                Log.e("onFailure", s + "    " + s1);
            }
        });

    }


    /**
     * 组装要分享的文字内容
     *
     * @param detailBean
     * @return
     */
    private String getInformation(CarDetailBean detailBean) {
        StringBuilder sb = new StringBuilder();
        String content = (String) SpUtils.get(this, Constants.KEY_WECHAT_CONTENT, "");

        CarDetailBean.DataBean data = detailBean.getData();
        sb.append(data.getName()).append("，")
                .append(data.getYear()).append("年，价格")
                .append(data.getPrice()).append("万，").append(data.getContent())
                .append("车辆位于").append(data.getProvinceName().replace("省", "")).append(data.getCityName().replace("市", ""))
                .append("，电话：").append(data.getPhone())
                .append("，").append(TextUtils.isEmpty(content) ? "如需分享信息请将你的车辆发布至铲车圈" : content);
        return sb.toString();
    }
}
