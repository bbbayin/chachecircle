package com.ccq.share.core;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.ccq.share.Constants;
import com.ccq.share.bean.ShareMeteBean;
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
import java.util.List;
import java.util.Observer;

import okhttp3.ResponseBody;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/****************************************
 * 功能说明:  
 *
 * Author: Created by bayin on 2017/9/1.
 ****************************************/

public class DownPicService extends Service implements Observer {
    private static final int COMMON_LOOPER = 1;//一条推送消息的图片下载完毕，开始下载另一条

    ArrayList<Uri> sListUri;//已经下载好的图片uri
    List<String> fileList;
    private List<ShareMeteBean> mDataSourceList = new ArrayList<>();//要下载的图片地址，分享内容数据源

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case COMMON_LOOPER:
                    if (mDataSourceList.size() > 0) {
                        downLoadPics(mDataSourceList.remove(0));
                    } else {
                        isDownLoading = false;
                    }
                    break;
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w("DownPicService", "onStartCommand.....");
        ShareMeteBean metaDataBean = intent.getParcelableExtra(Constants.KEY_SHARE_METE_DATA);
        //判断是否下载图片中
//        if (isDownloading) {
//            //将数据添加到数据池
//            mDataSourceList.add(metaDataBean);
//        } else {
//            downLoadPics(metaDataBean);
//        }
        if (isDownLoading) {
            mDataSourceList.add(metaDataBean);
        } else {
            downLoadPics(metaDataBean);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private boolean isDownLoading = false;

    /**
     * 下载图片
     *
     * @param bean
     */
    private synchronized void downLoadPics(final ShareMeteBean bean) {
        //开始下载
        isDownLoading = true;
        List<String> list = bean.getUrlList();
        //为空，不下载
        if (list == null || list.size() == 0) {
            initLooper();
            return;
        }

        if (list.size() > 9) {
            list = list.subList(0, 8);
        }
        final int picNumber = list.size();

        sListUri = new ArrayList<>();
        fileList = new ArrayList<>();
        final DownloadService service = HttpUtils.getInstance().getPicRetrofit().create(DownloadService.class);

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
                            return service.downloadPic(fileUrl);
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<ResponseBody>() {
                    @Override
                    public void onCompleted() {
                        ToastUtil.show("下载完成,启动微信...");
                        Log.w("全部完成！", "数量：" + sListUri.size());
                        //发送消息
                        share(sListUri, bean);
                    }

                    @Override
                    public void onError(Throwable e) {
                        initLooper();
                        Log.e("xxxx下载出错！", e.toString());
                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        try {
                            File file = DownLoadUtils.writeToFile(responseBody.bytes());
                            sListUri.add(Uri.fromFile(file));
                            fileList.add(file.getAbsolutePath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    /**
     * 下载任务结束，初始化下一次循环
     */
    private void initLooper() {
        mHandler.sendEmptyMessage(COMMON_LOOPER);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private synchronized void share(final ArrayList<Uri> uris, final ShareMeteBean bean) {
        if (uris.size() == 0) {
            return;
        }
        for (Object o :
                fileList.toArray()) {
            Log.w("xxxxxxxxx",o.toString());
        }
        String[] strings = new String[fileList.size()];
        MediaScannerConnection.scanFile(this, fileList.toArray(strings),
                null, new MediaScannerConnection.OnScanCompletedListener() {
            /*
             *   (non-Javadoc)
             * @see android.media.MediaScannerConnection.OnScanCompletedListener#onScanCompleted(java.lang.String, android.net.Uri)
             */
            public void onScanCompleted(String path, Uri uri)
            {
                Log.i("ExternalStorage", "Scanned " + path + ":");
                Log.i("ExternalStorage", "-> uri=" + uri);
                ToastUtil.show("HHAHHAHAHHA");

                ScreenLockUtils.getInstance(getApplicationContext()).unLockScreen();
                WechatTempContent.describeList.add(bean.getShareContent());
                WorkLine.initWorkList();
                WorkLine.size = fileList.size();
                WechatTempContent.describeList.add(bean.getShareContent());
                PackageManager packageManager = getBaseContext().getPackageManager();
                Intent it = packageManager.getLaunchIntentForPackage(Constants.WECHAT_PACKAGE_NAME);
                startActivity(it);

                initLooper();
            }
        });
//        ToastUtil.show("HHAHHAHAHHA");
//

//        Intent intent = new Intent();
//        ComponentName comp = new ComponentName(Constants.WECHAT_PACKAGE_NAME,
//                Constants.WECHAT_SHAREUI_NAME);
//        intent.setComponent(comp);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
//        intent.setType("image/*");
//        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
//        WechatTempContent.describeList.add(bean.getShareContent());
//        WorkLine.initWorkList();
//        WorkLine.size = uris.size();
////        startActivity(intent);
//        PackageManager packageManager = getBaseContext().getPackageManager();
//        Intent it = packageManager.getLaunchIntentForPackage(Constants.WECHAT_PACKAGE_NAME);
//        startActivity(it);
//
//        initLooper();
    }

    @Override
    public void update(java.util.Observable o, Object arg) {
//        Log.d("update", "收到通知，执行下一个任务！");
//        initLooper();
    }
}
