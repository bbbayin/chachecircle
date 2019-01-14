package com.ccq.share.service;

import com.ccq.share.bean.CarInfoBean;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/**
 * Created by Administrator on 2017/8/27.
 */

public interface CarListService {

    @GET("car/list")
    Call<List<CarInfoBean>> getCarList(@Header("user") String user, @Header("pass") String pass,
                                       @Header("time") String time, @Header("auth") String auth,
                                       @Query("page")int page, @Query("size")int size);

}
