package com.ccq.share.core;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.ccq.share.Constants;
import com.ccq.share.LogUtils;
import com.ccq.share.MyApp;
import com.ccq.share.http.DownLoadUtils;
import com.ccq.share.http.HttpUtils;
import com.ccq.share.service.DownloadService;
import com.ccq.share.utils.ScreenLockUtils;
import com.ccq.share.utils.ToastUtil;
import com.ccq.share.utils.WechatTempContent;
import com.ccq.share.work.WorkLine;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.listener.DownloadListener2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private final static ImageDownloadManager INSTANCE = new ImageDownloadManager();
    private DownloadService downloadImageService = HttpUtils.getInstance().getPicRetrofit().create(DownloadService.class);
    private List<String> downloadImageList;
    private Activity sActivity;
    private boolean isWaiting = true;

    private final Handler imageDownloadHandler;

    public void init(Activity activity) {
        this.sActivity = activity;
    }

    public static ImageDownloadManager getINSTANCE() {
        return INSTANCE;
    }

    private ImageDownloadManager() {
        HandlerThread handlerThread = new HandlerThread("imageDownload");
        handlerThread.start();


        imageDownloadHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                synchronized (INSTANCE) {
                    if (msg.obj instanceof DownLoadBean) {
                        Log.i(LOGTAG, msg.obj + "开始<<<<<<<<<");
//                        download((DownLoadBean) msg.obj);
                        downloadPicNew((DownLoadBean) msg.obj);
                        try {
                            while (isWaiting)
                                INSTANCE.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.i(LOGTAG, msg.obj + "结束>>>>>>>>");
                    }
                }
            }
        };
    }


    public void startLooper() {
        synchronized (INSTANCE) {
            isWaiting = false;
            INSTANCE.notifyAll();
            Log.i(LOGTAG, "下载管理器唤醒...");
        }
    }

    public void putDownloadPool(DownLoadBean bean) {
        Message message = imageDownloadHandler.obtainMessage();
        message.obj = bean;
        imageDownloadHandler.sendMessage(message);
    }

    private void download(final DownLoadBean bean) {
        isWaiting = true;
        //开始下载
        List<String> list = bean.imageList;
        downloadImageList = new ArrayList<>();
        //为空，不下载
        if (list == null || list.size() == 0) {
            return;
        }
        sActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtil.show("开始下载图片...");
            }
        });
        if (list.size() > 9) {
            list = list.subList(0, 8);
        }
        Collections.reverse(list);
        Observable.from(list)
                .flatMap(new Func1<String, Observable<ResponseBody>>() {
                    @Override
                    public Observable<ResponseBody> call(String url) {
                        printCurrentThread("call()");

                        String fileUrl = DownLoadUtils.getUrl(url);
                        if (TextUtils.isEmpty(fileUrl)) {
                            return null;
                        } else {
                            Log.w("开始下载图片...", fileUrl);
                            return downloadImageService.downloadPic(fileUrl);
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<ResponseBody>() {
                    @Override
                    public void onCompleted() {
                        printCurrentThread("onCompleted()");
                        // 启动微信
                        share(bean);
                    }

                    @Override
                    public void onError(Throwable e) {
                        printCurrentThread("onError()");
                        Log.e(LOGTAG, "下载图片错误：" + e.getMessage());
                        startLooper();
                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        printCurrentThread("onNext()");
                        try {
                            File file = DownLoadUtils.writeToFile(responseBody.bytes());
                            if (file != null && file.exists()) {
                                downloadImageList.add(file.getAbsolutePath());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void printCurrentThread(String tag) {
        Log.d(LOGTAG, tag + "---[Thread = " + Thread.currentThread().getName() + "]");
    }

    private void downloadPicNew(final DownLoadBean bean) {
        isWaiting = true;
        //开始下载
        List<String> list = bean.imageList;
        downloadImageList = new ArrayList<>();
        //为空，不下载
        if (list == null || list.size() == 0) {
            return;
        }
        sActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtil.show("开始下载图片...");
            }
        });
        if (list.size() > 9) {
            list = list.subList(0, 8);
        }
        Collections.reverse(list);

        // pre check
        String fileName = System.currentTimeMillis() + ".jpg";
        File parentFile = new File(Constants.SD_ROOTPATH, fileName);
        ArrayList<DownloadTask> downloadTasks = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            String url = DownLoadUtils.getUrl(list.get(i));
            if (url == null) continue;
            DownloadTask task = new DownloadTask.Builder(url, parentFile.getAbsolutePath(), fileName)
                    .setMinIntervalMillisCallbackProcess(50).build();
            downloadTasks.add(task);
        }

        DownloadTask.enqueue(downloadTasks.toArray(new DownloadTask[]{}), new DownloadListener2() {
            @Override
            public void taskStart(@NonNull DownloadTask task) {
                Log.i("下载开始", task.getUrl());
            }

            @Override
            public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause, @Nullable Exception realCause) {
                if (task.getFile() == null) {
                    Log.e("下载出错", task.getUrl());
                } else {
                    Log.w("下载完成", task.getFile().getAbsolutePath());
                    downloadImageList.add(task.getFile().getAbsolutePath());
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
        if (downloadImageList.size() == 0) {
            startLooper();
            return;
        }
        String[] strings = new String[downloadImageList.size()];

        WorkLine.initWorkList();
        WorkLine.size = downloadImageList.size();

        MediaScannerConnection.scanFile(MyApp.getContext(), downloadImageList.toArray(strings),
                null, new MediaScannerConnection.OnScanCompletedListener() {

                    public void onScanCompleted(String path, Uri uri) {
                        printCurrentThread("onScanCompleted()");
                        if (downloadImageList != null) {
                            downloadImageList.remove(path);
                            if (downloadImageList.size() == 0) {
                                // 启动微信
                                ScreenLockUtils.getInstance(MyApp.getContext()).unLockScreen();
                                WechatTempContent.describeList.add(bean.desc);

                                PackageManager packageManager = MyApp.getContext().getPackageManager();
                                Intent it = packageManager.getLaunchIntentForPackage(Constants.WECHAT_PACKAGE_NAME);
                                if (sActivity != null)
                                    sActivity.startActivity(it);

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
