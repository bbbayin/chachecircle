package com.ccq.share.service;

import com.ccq.share.bean.QiugouBean;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/**
 * 查询求购信息
 *
 * Created by Administrator on 2017/10/29.
 */

public interface QiuGouService {
    @GET("user/qiugou/info")
    Call<QiugouBean> getQiugou(@Query("id") String id, @Header("user") String user, @Header("pass") String pass,
                               @Header("time") String time, @Header("auth") String auth);
}
