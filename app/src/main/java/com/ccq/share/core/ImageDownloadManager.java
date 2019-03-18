package com.ccq.share.core;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.ccq.share.Constants;
import com.ccq.share.MyApp;
import com.ccq.share.http.DownLoadUtils;
import com.ccq.share.http.HttpUtils;
import com.ccq.share.service.DownloadService;
import com.ccq.share.utils.ScreenLockUtils;
import com.ccq.share.utils.ToastUtil;
import com.ccq.share.utils.WechatTempContent;
import com.ccq.share.work.WorkLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * 图片下载管理器，任务下载完成启动分享任务，暂停自己
 */
public class ImageDownloadManager {
    private String LOGTAG = "--ImageDownloadManager--";
    private List<DownLoadBean> downList;
    private final static ImageDownloadManager INSTANCE = new ImageDownloadManager();
    private ExecutorService executorService;
    private DownloadService downloadApi = HttpUtils.getInstance().getPicRetrofit().create(DownloadService.class);
    private List<String> filesDownload;
    private Activity sActivity;
    private boolean mBlocked = false;

    private ImageDownloadManager() {
        downList = new ArrayList<>();
        executorService = Executors.newSingleThreadExecutor();
    }

    public static ImageDownloadManager getINSTANCE() {
        return INSTANCE;
    }

    public void putDownloadPool(DownLoadBean bean) {
        if (bean != null) {
            downList.add(bean);
        }
    }

    public void init(Activity activity) {
        this.sActivity = activity;
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(LOGTAG, "图片下载任务启动...");
                for (; ; ) {
                    if (!mBlocked) {
                        if (downList != null && downList.size() > 0) {
                            Log.d(LOGTAG, "[有下载任务了，开启下载]...");
                            blockLooper();
                            download(downList.remove(0));
                        }
//                        Log.i(LOGTAG, "下载图片任务轮询中...");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void blockLooper() {
        mBlocked = true;
        Log.w(LOGTAG, "[下载管理器停止循环]");
    }

    public void startLooper() {
        mBlocked = false;
    }

    private void download(final DownLoadBean bean) {
        //开始下载
        List<String> list = bean.imageList;
        filesDownload = new ArrayList<>();
        //为空，不下载
        if (list == null || list.size() == 0) {
            startLooper();
            return;
        }

        if (list.size() > 9) {
            list = list.subList(0, 8);
        }
        Collections.reverse(list);
        Observable.from(list)
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<String, Observable<ResponseBody>>() {
                    @Override
                    public Observable<ResponseBody> call(String url) {
                        String fileUrl = DownLoadUtils.getUrl(url);
                        if (TextUtils.isEmpty(fileUrl)) {
                            return null;
                        } else {
                            Log.w("开始下载图片...", fileUrl);
                            ToastUtil.show("开始下载图片");
                            return downloadApi.downloadPic(fileUrl);
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<ResponseBody>() {
                    @Override
                    public void onCompleted() {
                        //发送消息
                        share(bean);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(LOGTAG, "下载图片错误：" + e.getMessage());
                        startLooper();
                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        try {
                            File file = DownLoadUtils.writeToFile(responseBody.bytes());
                            if (file != null && file.exists()) {
                                filesDownload.add(file.getAbsolutePath());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    /**
     * 启动微信分享
     *
     * @param bean
     */
    private synchronized void share(final DownLoadBean bean) {
        if (filesDownload.size() == 0) {
            startLooper();
            return;
        }
        String[] strings = new String[filesDownload.size()];

        WorkLine.initWorkList();
        WorkLine.size = filesDownload.size();

        MediaScannerConnection.scanFile(MyApp.getContext(), filesDownload.toArray(strings),
                null, new MediaScannerConnection.OnScanCompletedListener() {

                    public void onScanCompleted(String path, Uri uri) {
                        if (filesDownload != null) {
                            filesDownload.remove(path);
                            if (filesDownload.size() == 0) {
                                // 启动微信
                                ScreenLockUtils.getInstance(MyApp.getContext()).unLockScreen();
                                WechatTempContent.describeList.add(bean.desc);

                                PackageManager packageManager = MyApp.getContext().getPackageManager();
                                Intent it = packageManager.getLaunchIntentForPackage(Constants.WECHAT_PACKAGE_NAME);
                                if (sActivity != null)
                                    sActivity.startActivity(it);
                                else {
                                    ToastUtil.show("重启APP");
                                }
                            }
                        }
                    }
                });
    }


    public static class DownLoadBean {
        private List<String> imageList;
        private String desc;

        public DownLoadBean(List<String> imageList, String desc) {
            this.imageList = imageList;
            this.desc = desc;
        }
    }
}
