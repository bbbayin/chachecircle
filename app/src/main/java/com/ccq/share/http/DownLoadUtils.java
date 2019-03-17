package com.ccq.share.http;

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.MainThread;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;

import com.ccq.share.Constants;
import com.ccq.share.activity.MainActivity;
import com.ccq.share.bean.CarInfoBean;
import com.ccq.share.bean.MomentBean;
import com.ccq.share.service.DownloadService;
import com.ccq.share.utils.PicParam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/****************************************
 * 功能说明:  
 *
 * Author: Created by bayin on 2017/8/28.
 ****************************************/

public class DownLoadUtils extends java.util.Observable {
    private static final int COMPLETE = 0;
    private static final int FINISH_ONE = 1;
    private static final int ERROR = 999;
    private static Retrofit sRetrofit;
    private int mPicSize = 0;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case COMPLETE:
                    setProgressDialogMsg("下载完成！即将启动微信分享");
                    dismissProgressDialog();
                    break;
                case FINISH_ONE:
                    String content = "正在下载图片..." + sListUri.size() + "/" + mPicSize;
                    setProgressDialogMsg(content);
                    break;
                case ERROR:
                    dismissProgressDialog();
                    break;
            }
        }
    };

    static {
        synchronized (DownLoadUtils.class) {
            if (sRetrofit == null)
                sRetrofit = new Retrofit.Builder()
                        .baseUrl(Constants.download_baseUrl)
                        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                        .build();
        }
    }

    private static DownLoadUtils mInstance;
    private ProgressDialog progressDialog;

    public static DownLoadUtils getInstance() {
        synchronized (DownLoadUtils.class) {
            if (mInstance == null) {
                mInstance = new DownLoadUtils();
            }
        }
        return mInstance;
    }

    /**
     * 下载图片完成，发送通知
     */
    public void notifyComplete(MomentBean momentBean) {
        setChanged();
        notifyObservers(momentBean);
    }

    private static ArrayList<Uri> sListUri;

    public void downLoadPic(final List<String> list, final MomentBean momentBean, MainActivity mainActivity) {
        addObserver(mainActivity);
        downLoadPic(null, list, momentBean);
    }

    public void downLoadPic(final Activity activity, final List<String> list, final MomentBean momentBean) {
        if (list == null || list.size() == 0) return;

        while (list.size() > 9) {
            list.remove(list.size() - 1);
        }
        mPicSize = list.size();
//        showProgressDialog(activity);

        sListUri = new ArrayList<>();
        final DownloadService service = sRetrofit.create(DownloadService.class);

        Observable.from(list)
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<String, Observable<ResponseBody>>() {
                    @Override
                    public Observable<ResponseBody> call(String url) {
                        String fileUrl = getUrl(url);
                        if (TextUtils.isEmpty(fileUrl)) {
                            return null;
                        } else {
                            Log.w("开始下载...", fileUrl);
                            return service.downloadPic(fileUrl);
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ResponseBody>() {
                    @Override
                    public void onCompleted() {
                        Log.w("全部完成！", "数量：" + sListUri.size());

                        //发送消息更新UI
                        mHandler.sendEmptyMessage(COMPLETE);
                        //发送消息
                        momentBean.setUriList(sListUri);
                        DownLoadUtils.getInstance().notifyComplete(momentBean);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("xxxx下载出错！", e.toString());
                        mHandler.sendEmptyMessage(ERROR);
                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        try {
                            File file = writeToFile(responseBody.bytes());
                            sListUri.add(Uri.fromFile(file));
                            mHandler.sendEmptyMessage(FINISH_ONE);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    public static String getUrl(String url) {
        if (url.matches("[a-zA-z]+://[^\\s]*")) {
            String replace = url.replace(Constants.download_baseUrl, "");
            return replace + "!auto";
//            return replace + PicParam.PIC_600;
        } else {
            return null;
        }
    }

    private void showProgressDialog(Activity activity) {
        if (activity != null) {
            if (progressDialog == null) {
                initDialog(activity);
            }
            if (!progressDialog.isShowing()) {
                progressDialog.show();
            }
        }
    }

    private void setProgressDialogMsg(String msg) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage(msg);
        }
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void initDialog(Activity activity) {
        progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage("正在下载图片...");
    }

    public static File writeToFile(byte[] bytes) {
        File root = new File(Constants.SD_ROOTPATH);
        if (!root.exists()) root.mkdirs();

        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(Constants.SD_ROOTPATH + fileName);
        if (file.isDirectory()) {
            file.delete();
        }
        try {
            file.createNewFile();
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(bytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
