package com.ccq.share.home;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.ccq.share.Constants;
import com.ccq.share.MyApp;
import com.ccq.share.activity.MainActivity;
import com.ccq.share.bean.CarDetailBean;
import com.ccq.share.bean.CarInfoBean;
import com.ccq.share.bean.QiugouBean;
import com.ccq.share.core.ImageDownloadManager;
import com.ccq.share.http.HttpUtils;
import com.ccq.share.service.CcqService;
import com.ccq.share.utils.SpUtils;
import com.tencent.bugly.crashreport.CrashReport;
import com.umeng.message.entity.UMessage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainPresenter {
    private static String LOGTAG = "--MainPresenter--";
    private IMainView iMainView;
    private String user = "guest";
    private String pass = "guest";
    private String time = "123456";
    private String auth;
    private int page = 1, size = 5;
    private final CcqService ccqService;
    private LinkedList<UMessage> uMessageLinkedList;
    private ExecutorService executorService;

    public MainPresenter(IMainView view) {
        this.iMainView = view;
        uMessageLinkedList = new LinkedList<>();
        auth = HttpUtils.getMd5(user, pass, time);
        Retrofit retrofit = HttpUtils.getInstance().getRetrofit();
        ccqService = retrofit.create(CcqService.class);
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                for (; ; ) {
                    if (uMessageLinkedList != null && uMessageLinkedList.size() > 0) {
                        Log.w(LOGTAG, "有消息了,开始解析...");
                        resolveUmMessage(uMessageLinkedList.remove(0));
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        CrashReport.testJavaCrash();
                    }
                }
            }
        });
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
            if (uMessageLinkedList == null) {
                uMessageLinkedList = new LinkedList<>();
            }
            uMessageLinkedList.add(message);
        }
    }

    /**
     * 处理推送消息
     *
     * @param message
     */
    public synchronized void resolveUmMessage(UMessage message) {
        if (Constants.isAutoShare) {
            String type = message.extra.get("type");
            if (Constants.TYPE_CAR.equals(type)) {
                //分享卖车
                String carid = message.extra.get("carid");
                String userid = message.extra.get("userid");
                Log.d("分享卖车参数：", "userid:" + userid + "   carid:" + carid);
                //排除异常数据
                if (!(TextUtils.isEmpty(carid) || TextUtils.isEmpty(userid) || "0".equals(carid) || "0".equals(userid))) {
                    querySoldCar(carid, userid);
                }
            } else if (Constants.TYPE_BUYER.equals(type)) {
                //求购分享
                String id = message.extra.get("id");
                if (!TextUtils.isEmpty(id) && !"0".equals(id)) {
                    Log.d("求购参数：", "id:" + id);
                    queryBuyCar(id);
                }
            }
        }
    }

    private void querySoldCar(String carid, String userid) {
        String auth = HttpUtils.getMd5(Constants.USER, Constants.PASS, Constants.TIME);
        ccqService.getCarInfo(carid, userid, Constants.USER,
                Constants.PASS, Constants.TIME, auth)
                .enqueue(new Callback<CarDetailBean>() {
                    @Override
                    public void onResponse(@NonNull Call<CarDetailBean> call, @NonNull Response<CarDetailBean> response) {
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
                                //获取分享文字内容
                                if (urlList.size() > 0) {
                                    ImageDownloadManager.getINSTANCE().putDownloadPool(new ImageDownloadManager.DownLoadBean(urlList, getInformation(body)));
                                }
                            }
                        } else {
                            Log.d("xxx", "查询车辆信息错误，请手动点击分享！");
                        }
                    }

                    @Override
                    public void onFailure(Call<CarDetailBean> call, Throwable t) {

                    }
                });
    }

    private String str = "车型：%s\n年限：%s年\n价格：%s\n地址：%s\n电话：%s\n描述：%s\n%s";
    private String strNoDesc = "车型：%s\n年限：%s年\n价格：%s\n地址：%s\n电话：%s\n%s";

    private String getInformation(CarDetailBean data) {
        String content = (String) SpUtils.get(MyApp.getContext(), Constants.KEY_WECHAT_CONTENT, "如需分享信息请将你的车辆发布至铲车圈");

        synchronized (MainActivity.class) {
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
    }

    /**
     * 查询买车信息
     *
     * @param carid
     */
    private void queryBuyCar(String carid) {
        String auth = HttpUtils.getMd5(Constants.USER, Constants.PASS, Constants.TIME);
        ccqService.getQiugou(carid, Constants.USER,
                Constants.PASS, Constants.TIME, auth).enqueue(new Callback<QiugouBean>() {
            @Override
            public void onResponse(Call<QiugouBean> call, Response<QiugouBean> response) {
                try {
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
                            ImageDownloadManager.getINSTANCE().putDownloadPool(new ImageDownloadManager.DownLoadBean(urlList, stringBuffer.toString()));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<QiugouBean> call, Throwable t) {
            }
        });
    }
}
