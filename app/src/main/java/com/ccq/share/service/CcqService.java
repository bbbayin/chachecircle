package com.ccq.share.service;

import com.ccq.share.bean.CarDetailBean;
import com.ccq.share.bean.CarInfoBean;
import com.ccq.share.bean.QiugouBean;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by Administrator on 2017/8/27.
 */

public interface CcqService {

    @GET("car/list")
    Call<List<CarInfoBean>> getCarList(@Header("user") String user, @Header("pass") String pass,
                                       @Header("time") String time, @Header("auth") String auth,
                                       @Query("page") int page, @Query("size") int size);

    @GET("car/info")
    Call<CarDetailBean> getCarInfo(@Query("carid") String carid, @Query("userid") String userid,
                                   @Header("user") String user, @Header("pass") String pass,
                                   @Header("time") String time, @Header("auth") String auth);

    @GET("user/qiugou/info")
    Call<QiugouBean> getQiugou(@Query("id") String id, @Header("user") String user, @Header("pass") String pass,
                               @Header("time") String time, @Header("auth") String auth);
}
