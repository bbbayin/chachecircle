package com.ccq.share.home;

import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.ccq.share.Constants;
import com.ccq.share.MyApp;
import com.ccq.share.activity.MainActivity;
import com.ccq.share.bean.CarDetailBean;
import com.ccq.share.bean.CarInfoBean;
import com.ccq.share.bean.QiugouBean;
import com.ccq.share.http.DownLoadUtils;
import com.ccq.share.http.HttpUtils;
import com.ccq.share.service.CcqService;
import com.ccq.share.service.DownloadService;
import com.ccq.share.utils.FileUtils;
import com.ccq.share.utils.ScreenLockUtils;
import com.ccq.share.utils.SpUtils;
import com.ccq.share.utils.ToastUtil;
import com.ccq.share.utils.WechatTempContent;
import com.ccq.share.work.WorkLine;
import com.liulishuo.okdownload.DownloadContext;
import com.liulishuo.okdownload.DownloadContextListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.listener.DownloadListener2;
import com.umeng.message.entity.UMessage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainPresenter {
    private static String LOGTAG = "--MainPresenter--";
    private IMainView iMainView;
    private String user = "guest";
    private String pass = "guest";
    private String time = "123456";
    private String auth;
    private int page = 1, size = 10;
    private final CcqService ccqService;
    private volatile boolean isWorking = false;
    public static String FINISH = "finish";
    private ConcurrentLinkedQueue<UMessage> messagePool = new ConcurrentLinkedQueue<>();


//    private final Handler mainPresenterHandler;
    private ArrayList<String> downloadImageList;

    private boolean isFirstShare = true;

    public MainPresenter(IMainView view) {
        this.iMainView = view;
        auth = HttpUtils.getMd5(user, pass, time);
        Retrofit retrofit = HttpUtils.getInstance().getRetrofit();
        ccqService = retrofit.create(CcqService.class);

        HandlerThread handlerThread = new HandlerThread("mainPresenter");
        handlerThread.start();
        Observable.interval(1, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        resolveUmMessage();
                    }
                });
//        mainPresenterHandler = new Handler(handlerThread.getLooper()) {
//            @Override
//            public void handleMessage(Message msg) {
//                int maxTime = 60;
//                if (msg.obj instanceof UMessage) {
//                    iMainView.showMessageDialog("收到消息，开始解析");
//                    resolveUmMessage((UMessage) msg.obj);
//                    isFirstShare = false;
//                    while (isWorking) {
//                        try {
//                            Thread.sleep(1000);
//                            maxTime--;
//                            if (maxTime <= 0) {// 超时
//                                isWorking = false;
//                            }
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    iMainView.dismissMessageDialog();
//                    mainUItoast("消息分享结束");
//                }
//            }
//        };

        EventBus.getDefault().register(this);
    }

    @Subscribe
    public void onReceive(String s) {
        if (TextUtils.equals(s, FINISH)) {
            isWorking = false;
        }
    }

    private void mainUItoast(final String s) {
        iMainView.get().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(iMainView.get(), s, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private synchronized void notifyHandler() {
        isWorking = false;
    }

    /**
     * 获取列表数据
     *
     * @param state
     */
    public void getCarList(final int state) {
        if (state == MainActivity.REFRESH) {
            page = 1;
        } else if (state == MainActivity.LOADMORE) {
            page++;
        }
        iMainView.showProgress();
        ccqService.getCarList(user, pass, time, auth, page, size)
                .enqueue(new Callback<List<CarInfoBean>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<CarInfoBean>> call, @NonNull Response<List<CarInfoBean>> response) {

                        List<CarInfoBean> body = response.body();
                        if (body != null) {
                            iMainView.dismissProgress();
                            iMainView.showCarList(body, state);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<CarInfoBean>> call, Throwable t) {
                        iMainView.dismissProgress();
                        iMainView.showErrorView();
                    }
                });
    }

    public synchronized void putMessagePool(UMessage message) {
        if (message != null) {
            ToastUtil.show("加入分享队列");
            messagePool.add(message);
//            Message msg = mainPresenterHandler.obtainMessage();
//            msg.obj = message;
//            if (isFirstShare) {
//                mainPresenterHandler.sendMessage(msg);
//            }else {
//                // 加延迟
//                // 获取延迟时间
//                int delay = 120;
//                try {
//                    Object o = SpUtils.get(iMainView.get(), Constants.KEY_DELAY_TIME, 0);
//                    if (o != null) {
//                        delay = (int) o;
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                mainPresenterHandler.sendMessageDelayed(msg, delay);
//            }
        }
    }

    private synchronized void resolveUmMessage() {
        if (Constants.isAutoShare) {
            isWorking = true;
            UMessage message = messagePool.poll();
            if (message != null) {
                String type = message.extra.get("type");
                if (Constants.TYPE_CAR.equals(type)) {
                    ToastUtil.show("解析消息：分享卖车");
                    //分享卖车
                    String carid = message.extra.get("carid");
                    String userid = message.extra.get("userid");
                    Log.d("分享卖车参数：", "userid:" + userid + "   carid:" + carid);
                    //排除异常数据
                    if (!(TextUtils.isEmpty(carid) || TextUtils.isEmpty(userid) || "0".equals(carid) || "0".equals(userid))) {
                        iMainView.showMessageDialog("卖车，查询任务信息");
                        querySoldCar(carid, userid);
                    } else {
                        iMainView.dismissProgress();
                        ToastUtil.show("消息解析失败，id,userid为空！");
                    }
                } else if (Constants.TYPE_BUYER.equals(type)) {
                    //求购分享
                    String id = message.extra.get("id");
                    if (!TextUtils.isEmpty(id) && !"0".equals(id)) {
                        iMainView.showMessageDialog("求购，查询信息");
                        queryBuyCar(id);
                    } else {
                        iMainView.dismissProgress();
                        ToastUtil.show("消息解析失败，id为空！");
                    }
                }
            }else {
                isWorking = false;
            }
        }
    }

    /**
     * 处理推送消息
     *
     * @param message
     */
    private synchronized void resolveUmMessage(UMessage message) {
        if (Constants.isAutoShare) {
            isWorking = true;
            String type = message.extra.get("type");
            if (Constants.TYPE_CAR.equals(type)) {
                ToastUtil.show("解析消息：分享卖车");
                //分享卖车
                String carid = message.extra.get("carid");
                String userid = message.extra.get("userid");
                Log.d("分享卖车参数：", "userid:" + userid + "   carid:" + carid);
                //排除异常数据
                if (!(TextUtils.isEmpty(carid) || TextUtils.isEmpty(userid) || "0".equals(carid) || "0".equals(userid))) {
                    iMainView.showMessageDialog("卖车，查询任务信息");
                    querySoldCar(carid, userid);
                } else {
                    iMainView.dismissProgress();
                    ToastUtil.show("消息解析失败，id,userid为空！");
                }
            } else if (Constants.TYPE_BUYER.equals(type)) {
                //求购分享
                String id = message.extra.get("id");
                if (!TextUtils.isEmpty(id) && !"0".equals(id)) {
                    iMainView.showMessageDialog("求购，查询信息");
                    queryBuyCar(id);
                } else {
                    iMainView.dismissProgress();
                    ToastUtil.show("消息解析失败，id为空！");
                }
            }
        }
    }

    /**
     * 卖车
     *
     * @param carid
     * @param userid
     */
    private void querySoldCar(String carid, String userid) {
        String auth = HttpUtils.getMd5(Constants.USER, Constants.PASS, Constants.TIME);
        try {
            Response<CarDetailBean> response = ccqService.getCarInfo(carid, userid, Constants.USER, Constants.PASS, Constants.TIME, auth)
                    .execute();
            CarDetailBean body = response.body();
            if (body != null) {
                if (body.getCode() == 0) {
                    Log.w("onResponse", body.getData().getContent());
                    //获取图片url
                    ArrayList<String> urlList = new ArrayList<>();
                    List<CarDetailBean.DataBean.CImagesBean> cImages = body.getData().getCImages();

                    for (CarDetailBean.DataBean.CImagesBean bean :
                            cImages) {
                        urlList.add(bean.getSavename());
                    }
                    if (urlList.size() > 0) {
                        // 下载图片
//                        download(urlList, getInformation(body));
                        downloadPicNew(urlList, getInformation(body));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String str = "车型：%s\n年限：%s年\n价格：%s\n地址：%s\n电话：%s\n描述：%s\n%s";
    private String strNoDesc = "车型：%s\n年限：%s年\n价格：%s\n地址：%s\n电话：%s\n%s";

    private String getInformation(CarDetailBean data) {
        String content = (String) SpUtils.get(MyApp.getContext(), Constants.KEY_WECHAT_CONTENT, "如需分享信息请将你的车辆发布至铲车圈");

        CarDetailBean.DataBean detail = data.getData();
        String strPrice = detail.getPrice() + "万";
        try {
            if (detail.getPrice() <= 0) {
                strPrice = "价格面议";
            }
        } catch (Exception e) {
            strPrice = "价格面议";
        }
        if (detail.getContent() == null) {
            return String.format(strNoDesc, detail.getName(), detail.getYear(), strPrice,
                    detail.getProvinceName() + detail.getCityName(), detail.getPhone(), content);
        } else {
            if (TextUtils.isEmpty(detail.getContent().trim())) {
                return String.format(strNoDesc, detail.getName(), detail.getYear(), strPrice,
                        detail.getProvinceName() + detail.getCityName(), detail.getPhone(), content);
            } else {
                return String.format(str, detail.getName(), detail.getYear(), strPrice,
                        detail.getProvinceName() + detail.getCityName(), detail.getPhone(), detail.getContent(), content);
            }
        }
    }

    /**
     * 查询买车信息
     *
     * @param carid
     */
    private void queryBuyCar(String carid) {
        String auth = HttpUtils.getMd5(Constants.USER, Constants.PASS, Constants.TIME);
        try {
            Response<QiugouBean> response = ccqService.getQiugou(carid, Constants.USER, Constants.PASS, Constants.TIME, auth).execute();
            if (response.body() != null) {
                int code = response.body().getCode();
                if (code == 0) {
                    //成功
                    QiugouBean.DataBean data = response.body().getData();
                    String content = data.getContent();
                    ArrayList<String> urlList = new ArrayList<>();
                    urlList.add(Constants.qiugou_img_url);
                    StringBuilder stringBuffer = new StringBuilder();
                    stringBuffer.append("【求购】");
                    stringBuffer.append(content).append("。");
                    if (!TextUtils.isEmpty(data.getAddress())) {
                        stringBuffer.append(data.getAddress()).append(",");
                    }
                    if (!TextUtils.isEmpty(data.getTitle())) {
                        stringBuffer.append(data.getTitle()).append("，");
                    }
                    stringBuffer.append("电话").append(data.getPhone()).append("。");
                    String end = (String) SpUtils.get(iMainView.get(), Constants.KEY_QIUGOU_END, "如有意向请致电");
                    stringBuffer.append(end);
                    // 加入下载队列
                    downloadPicNew(urlList, stringBuffer.toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //    private void download(List<String> imageURLs, final String desc) {
//        //开始下载
//        downloadImageList = new ArrayList<>();
//        //为空，不下载
//        if (imageURLs == null || imageURLs.size() == 0) {
//            return;
//        }
//        iMainView.get().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                iMainView.showMessageDialog("开始下载图片");
//            }
//        });
//        if (imageURLs.size() > 9) {
//            imageURLs = imageURLs.subList(0, 8);
//        }
//        final int size = imageURLs.size();
//        Collections.reverse(imageURLs);
//        FileUtils.deleteDirWithFile(new File(Constants.SD_ROOTPATH));
//        Observable.from(imageURLs)
//                .flatMap(new Func1<String, Observable<ResponseBody>>() {
//                    @Override
//                    public Observable<ResponseBody> call(String url) {
//                        String fileUrl = DownLoadUtils.getUrl(url);
//                        if (TextUtils.isEmpty(fileUrl)) {
//                            return null;
//                        } else {
//                            return imageService.downloadPic(fileUrl);
//                        }
//                    }
//                })
//                .subscribeOn(Schedulers.io())
//                .subscribe(new Subscriber<ResponseBody>() {
//                    @Override
//                    public void onCompleted() {
//                        // 启动微信
//                        iMainView.dismissMessageDialog();
//                        mainUItoast("图片下载完成，启动微信");
//                        share(desc);
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//                        Log.e(LOGTAG, "下载图片错误：" + e.getMessage());
//                        iMainView.showMessageDialog("图片下载失败！");
//                        try {
//                            Thread.sleep(1000);
//                        } catch (InterruptedException d) {
//                            d.printStackTrace();
//                        }
//                        iMainView.dismissMessageDialog();
//                        notifyHandler();
//                    }
//
//                    @Override
//                    public void onNext(ResponseBody responseBody) {
//                        try {
//                            File file = DownLoadUtils.writeToFile(responseBody.bytes());
//                            if (file != null && file.exists()) {
//                                downloadImageList.add(file.getAbsolutePath());
//                            }
//                            iMainView.showMessageDialog(String.format("图片下载进度：%s/%s",
//                                    downloadImageList.size(), size));
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });
//    }
    final int MAXPRIORITY = 9000;

    private void downloadPicNew(List<String> imageURLs, final String desc) {
        //开始下载
        downloadImageList = new ArrayList<>();
        //为空，不下载
        if (imageURLs == null || imageURLs.size() == 0) {
            return;
        }
        iMainView.get().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                iMainView.showMessageDialog("开始下载图片");
            }
        });
        if (imageURLs.size() > 9) {
            imageURLs = imageURLs.subList(0, 9);
        }
        final int size = imageURLs.size();
        Collections.reverse(imageURLs);
        FileUtils.deleteDirWithFile(new File(Constants.SD_ROOTPATH));

        //todo pre check
        // task manager
        DownloadContext.Builder builder = new DownloadContext.QueueSet()
                .setParentPathFile(new File(Constants.SD_ROOTPATH))
                .setMinIntervalMillisCallbackProcess(150)
                .commit();
        for (int i = 0; i < imageURLs.size(); i++) {

            String fileName = String.format("%s_%s.jpg", System.currentTimeMillis(), i);
            String url = DownLoadUtils.completeURL(imageURLs.get(i));
            if (url == null) continue;
            DownloadTask.Builder taskBuilder = new DownloadTask.Builder(url, Constants.SD_ROOTPATH, fileName);
            taskBuilder.setPriority(MAXPRIORITY >> i);
            DownloadTask task = taskBuilder.build();

            Log.i("下载任务", task.getFilename());
            builder.bindSetTask(task);
        }
        builder.setListener(new DownloadContextListener() {
            @Override
            public void taskEnd(@NonNull DownloadContext context, @NonNull DownloadTask task, @NonNull EndCause cause, @Nullable Exception realCause, int remainCount) {
                if (task.getFile() != null) {
                    Log.i("下载完成", task.getUrl());
                    if (task.getFile() != null && task.getFile().exists()) {
                        downloadImageList.add(task.getFile().getAbsolutePath());
                    }
                    iMainView.showMessageDialog(String.format("图片下载进度：%s/%s",
                            downloadImageList.size(), size));
                }
            }

            @Override
            public void queueEnd(@NonNull DownloadContext context) {
                // 队列结束
                Log.i("任务结束", "success");
                iMainView.dismissMessageDialog();
                share(desc);
            }
        });

        builder.build().startOnSerial(new DownloadListener2() {
            @Override
            public void taskStart(@NonNull DownloadTask task) {

            }

            @Override
            public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause, @Nullable Exception realCause) {

            }
        });
    }


    /**
     * 启动微信分享
     */
    private synchronized void share(final String desc) {
        if (downloadImageList.size() == 0) {
            notifyHandler();
            return;
        }
        String[] strings = new String[downloadImageList.size()];

        WorkLine.initWorkList();
        WorkLine.size = downloadImageList.size();

        MediaScannerConnection.scanFile(MyApp.getContext(), downloadImageList.toArray(strings),
                null, new MediaScannerConnection.OnScanCompletedListener() {

                    public void onScanCompleted(String path, Uri uri) {
                        if (downloadImageList != null) {
                            downloadImageList.remove(path);
                            if (downloadImageList.size() == 0) {
                                // 启动微信
                                ScreenLockUtils.getInstance(MyApp.getContext()).unLockScreen();
                                WechatTempContent.describeList.add(desc);
                                launchWeChat();
//                                PackageManager packageManager = MyApp.getContext().getPackageManager();
//                                Intent it = packageManager.getLaunchIntentForPackage(Constants.WECHAT_PACKAGE_NAME);
//                                if (iMainView != null && iMainView.get() != null)
//                                    iMainView.get().startActivity(it);
                            }
                        }
                    }
                });
    }

    private void launchWeChat() {
        Intent intent = new Intent();
        ComponentName componentName = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI");
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(componentName);
        if (iMainView != null && iMainView.get() != null)
            iMainView.get().startActivity(intent);

    }
}
