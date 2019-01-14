package com.ccq.share;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.ccq.share.bean.CarDetailBean;
import com.ccq.share.bean.PushBean;
import com.ccq.share.http.HttpUtils;
import com.ccq.share.utils.SpUtils;
import com.umeng.message.IUmengRegisterCallback;
import com.umeng.message.PushAgent;
import com.wizchen.topmessage.util.TopActivityManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Retrofit;

/****************************************
 * 功能说明:  
 *
 * Author: Created by bayin on 2017/8/28.
 ****************************************/

public class MyApp extends Application {

    private Retrofit mRetrofit;
    public static List<PushBean> sShareDataSource;//推送来的数据
    public static boolean isLocked = false;//是否在分享过程中
    private static Context mContext;
    public static Context getContext(){
        return mContext;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        registerActivityLifecycleCallbacks(TopActivityManager.getInstance());
        sShareDataSource = new ArrayList<>();

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

            }
        });

        mRetrofit = HttpUtils.getInstance().getRetrofit();
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
                .append("，").append(TextUtils.isEmpty(content) ? "如需分享信息请将你的车辆发布至叉车圈" : content);
        return sb.toString();
    }
}
