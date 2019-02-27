package com.ccq.share.http;

import com.ccq.share.Constants;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLSyntaxErrorException;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Administrator on 2017/8/25.
 */

public class HttpUtils {

    private static Retrofit retrofit,picRetrofit;

    public HttpUtils() {
    }

    private static HttpUtils mInstance;

    public static HttpUtils getInstance() {
        synchronized (HttpUtils.class) {
            if (mInstance == null) {
                mInstance = new HttpUtils();
            }
        }
        return mInstance;
    }

    public Retrofit getRetrofit() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new LoggingInterceptor())
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public Retrofit getPicRetrofit(){
        synchronized (DownLoadUtils.class) {
            if (picRetrofit == null)
                picRetrofit = new Retrofit.Builder()
                        .baseUrl(Constants.download_baseUrl)
                        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                        .build();
        }
        return picRetrofit;
    }


    public static String getMd5(String... strings) {
        if (strings.length == 0) return null;
        else {
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < strings.length; i++) {
                stringBuffer.append(strings[i]);
            }
            String step1 = stringBuffer.toString();
            System.out.println("getMd5第一步  step1 = " + step1);
            MessageDigest md5 = null;
            try {
                md5 = MessageDigest.getInstance("MD5");
                byte[] bytes = md5.digest(step1.getBytes());
                String result = "";
                for (byte b : bytes) {
                    String temp = Integer.toHexString(b & 0xff);
                    if (temp.length() == 1) {
                        temp = "0" + temp;
                    }
                    result += temp;
                }
                System.out.println("getMd5结果 = " + result);
                return result;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static void main(String args[]) {
        String md5 = getMd5("app", "app", "123456");
        System.out.println(md5);
    }
}
