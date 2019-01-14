package com.ccq.share.service;

import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;

/****************************************
 * 功能说明:  
 *
 * Author: Created by bayin on 2017/8/28.
 ****************************************/

public interface DownloadService {
    @GET("{path}")
    Observable<ResponseBody> downloadPic(@Path("path") String url);
}
