package com.ccq.share.activity;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.ccq.share.Constants;
import com.ccq.share.MyApp;
import com.chacq.share.R;
import com.ccq.share.adapter.ProductAdapter;
import com.ccq.share.bean.CarDetailBean;
import com.ccq.share.bean.CarInfoBean;
import com.ccq.share.bean.MomentBean;
import com.ccq.share.bean.PushBean;
import com.ccq.share.bean.QiugouBean;
import com.ccq.share.bean.ShareMeteBean;
import com.ccq.share.core.DownPicService;
import com.ccq.share.http.DownLoadUtils;
import com.ccq.share.http.HttpUtils;
import com.ccq.share.service.CarDetailService;
import com.ccq.share.service.CarListService;
import com.ccq.share.service.QiuGouService;
import com.ccq.share.utils.PermissionUtils;
import com.ccq.share.utils.SpUtils;
import com.ccq.share.utils.WechatTempContent;
import com.ccq.share.view.ProgressView;
import com.umeng.message.PushAgent;
import com.umeng.message.UmengMessageHandler;
import com.umeng.message.entity.UMessage;
import com.wizchen.topmessage.TopMessageManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, Observer, View.OnClickListener {
    public static MainActivity instance;
    private static final int INIT = 1;//初始化
    private static final int REFRESH = 2;
    private static final int LOADMORE = 3;
    private static final int CLEAR_DATA = 4;
    private static final int ERROR = -1;//加载出错
    private static final int LOOPER_PUSH_DATA = 111;//查询推送来的消息列表
    private static final int SHOW_DELAY_PROGRESS = 5;//显示延时进度框
    private SwipeRefreshLayout swipLayout;
    private RecyclerView refreshView;
    private ProductAdapter adapter;
    private Retrofit retrofit;
    private ProgressBar progressBar;

    private String user = "guest";
    private String pass = "guest";
    private String time = "123456";
    private String auth;
    private int page = 1, size = 5;
    private List<CarInfoBean> mCarList;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            hideErrorLayout();
            switch (msg.what) {
                case INIT:
                    if (adapter == null) {
                        if (mCarList != null && mCarList.size() > 0) {
                            adapter = new ProductAdapter(mCarList);
                            refreshView.setAdapter(adapter);
                        } else {
                            showNetErrorLayout();
                        }
                    } else {
                        adapter.refresh(mCarList);
                    }
                    break;
                case REFRESH:
                    if (mCarList != null && mCarList.size() > 0) {
                        adapter.refresh(mCarList);
                    }
                    break;
                case LOADMORE:
                    if (mCarList != null && mCarList.size() > 0)
                        adapter.loadMore(mCarList);
                    break;
                case CLEAR_DATA:
                    hideProgress();
                    TopMessageManager.showSuccess("清除成功！");
                    break;
                case ERROR:
                    if (adapter == null || mCarList == null || mCarList.size() == 0)
                        mEmptyLayout.setVisibility(View.VISIBLE);
                    else
                        TopMessageManager.showError("网络出现错误，请稍后再试");
                    break;
                case LOOPER_PUSH_DATA:
                    if (isNeedDelay) {
                        progressView.setVisibility(View.GONE);
                        progressView.endCount();
                    }
                    if (MyApp.sShareDataSource.size() > 0) {
                        PushBean remove = MyApp.sShareDataSource.remove(0);
                        if (remove.getType() == PushBean.TYPE_SELL) {
                            //售车
                            queryCarInfo(remove.getCarid(), remove.getUserid());
                        } else if (remove.getType() == PushBean.TYPE_BUY) {
                            //求购
                            wantBuy(remove.getId());
                        }
                    } else {
                        MyApp.isLocked = false;
                    }
                    break;
                case SHOW_DELAY_PROGRESS:
                    if (isNeedDelay && !progressView.isShowing()) {
                        progressView.setVisibility(View.VISIBLE);
                        progressView.startCount(delay);
                    }
                    break;
            }
        }
    };
    private LinearLayoutManager manager;
    private boolean isLoading;
    private PushAgent mPushAgent;

    private WeakReference<MainActivity> mWeakReference = new WeakReference<MainActivity>(this);
    private View mEmptyLayout;
    private AlertDialog.Builder builder;
    private int delay;
    private boolean isNeedDelay = false;
    private ProgressView progressView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        initView();
        PermissionUtils.checkPerssion(this, 1);

        Constants.isAutoShare = PermissionUtils.serviceIsRunning(this);

        if (Constants.isAutoShare)
            Toast.makeText(this, "辅助功能已开启", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(this, "辅助功能未开启！！！请前往设置", Toast.LENGTH_LONG).show();

        swipLayout.setOnRefreshListener(this);
        manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        refreshView.setLayoutManager(manager);
        refreshView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int lastVisibleItemPosition = manager.findLastVisibleItemPosition();
                if (lastVisibleItemPosition + 1 == adapter.getItemCount()) {
                    boolean isRefreshing = swipLayout.isRefreshing();
                    if (isRefreshing) {
                        adapter.notifyItemRemoved(adapter.getItemCount());
                        return;
                    }
                    if (!isLoading) {
                        isLoading = true;
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                loadMore();
                                isLoading = false;
                            }
                        }, 1000);
                    }
                }
            }
        });

        initData(INIT);
        //推送服务
        mPushAgent = PushAgent.getInstance(this);
        mPushAgent.setMessageHandler(messageHandler);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Constants.isAutoShare) {
//            showAlertDialog();
        }
        //检测延时
        delay = (int) SpUtils.get(this, Constants.KEY_DELAY_TIME, 0);
        if (delay > 0) {
            isNeedDelay = true;
            progressView.setTime(delay);
        } else isNeedDelay = false;
    }

    private void showAlertDialog() {
        if (builder == null) {
            initDialog();
        }
        builder.show();
    }

    private void initDialog() {
        builder = new AlertDialog.Builder(this).setTitle("提示")
                .setMessage("自动分享功能未开启，现在设置？")
                .setNegativeButton("暂不设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setClass(MainActivity.this, MainSettingsActivity.class);
                        startActivity(intent);
                    }
                });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            //启动一个意图,回到桌面
            Intent backHome = new Intent(Intent.ACTION_MAIN);
            backHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            backHome.addCategory(Intent.CATEGORY_HOME);
            startActivity(backHome);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public static final String TAG = "MainActivity";
    /**
     * 收到推送消息的回调
     */
    UmengMessageHandler messageHandler = new UmengMessageHandler() {
        @Override
        public Notification getNotification(Context context, final UMessage uMessage) {
            Log.d(TAG, "收到消息：" + uMessage.text);

            shareMessage(uMessage);

            switch (uMessage.builder_id) {
                case 1:
                    Notification.Builder builder = new Notification.Builder(context);
                    RemoteViews myNotificationView = new RemoteViews(context.getPackageName(), R.layout.notification_view);
                    myNotificationView.setTextViewText(R.id.notification_title, uMessage.title);
                    myNotificationView.setTextViewText(R.id.notification_text, uMessage.text);
                    myNotificationView.setImageViewResource(R.id.notification_small_icon, getSmallIconId(context, uMessage));
                    builder.setContent(myNotificationView)
                            .setSmallIcon(getSmallIconId(context, uMessage))
                            .setTicker(uMessage.ticker)
                            .setAutoCancel(true);

                    return builder.getNotification();
                default:
                    //默认为0，若填写的builder_id并不存在，也使用默认。
                    return super.getNotification(context, uMessage);
            }
        }
    };

    private void shareMessage(UMessage uMessage) {
        if (Constants.isAutoShare) {
            String type = uMessage.extra.get("type");
            if (Constants.TYPE_CAR.equals(type)) {
                //分享卖车
                String carid = uMessage.extra.get("carid");
                String userid = uMessage.extra.get("userid");
                Log.d("参数：", "userid:" + userid + "   carid:" + carid);
                //排除异常数据
                if (!(TextUtils.isEmpty(carid) || TextUtils.isEmpty(userid) || "0".equals(carid) || "0".equals(userid))) {
//                    if (MyApp.isLocked) {
//                        //在分享过程中...添加到数据列表
//                        MyApp.sShareDataSource.add(new PushBean(userid, carid));
//                    } else {
//                        //空闲。。。
//                        MyApp.isLocked = true;
//                        queryCarInfo(carid, userid);
//                    }
                    MyApp.sShareDataSource.add(new PushBean(PushBean.TYPE_SELL, userid, carid));
                    if (!MyApp.isLocked) {
                        mHandler.sendEmptyMessageDelayed(LOOPER_PUSH_DATA, delay * 1000);
                        mHandler.sendEmptyMessage(SHOW_DELAY_PROGRESS);
                    }
                }
            } else if (Constants.TYPE_BUYER.equals(type)) {
                //求购分享
                String id = uMessage.extra.get("id");
                if (!TextUtils.isEmpty(id)) {
                    Log.d("求购参数：", "id:" + id);
                    //加入队列
                    MyApp.sShareDataSource.add(new PushBean(PushBean.TYPE_BUY, id));
                    if (!MyApp.isLocked) {
                        mHandler.sendEmptyMessageDelayed(LOOPER_PUSH_DATA, delay * 1000);
                        mHandler.sendEmptyMessage(SHOW_DELAY_PROGRESS);
                    }
                }
            }
        }
    }

    /**
     * 求购
     *
     * @param id
     */
    private void wantBuy(String id) {
        MyApp.isLocked = true;
        QiuGouService qiuGouService = HttpUtils.getInstance().getRetrofit().create(QiuGouService.class);
        String auth = HttpUtils.getMd5(Constants.USER, Constants.PASS, Constants.TIME);
        qiuGouService.getQiugou(id, Constants.USER,
                Constants.PASS, Constants.TIME, auth).enqueue(new Callback<QiugouBean>() {
            @Override
            public void onResponse(Call<QiugouBean> call, Response<QiugouBean> response) {
                try {
                    int code = response.body().getCode();
                    if (code == 0) {
                        //成功
                        QiugouBean.DataBean data = response.body().getData();
                        String content = data.getContent();
                        String pic = data.getPic();
                        ArrayList<String> urlList = new ArrayList<>();
                        urlList.add(pic);
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
                        String end = (String) SpUtils.get(MainActivity.this, Constants.KEY_QIUGOU_END, "如有意向请致电");
                        stringBuffer.append(end);
                        MomentBean momentBean = new MomentBean();
                        momentBean.setInformation(stringBuffer.toString());
                        DownLoadUtils.getInstance().downLoadPic(urlList, momentBean, mWeakReference.get());

                    }
                    initLooper();
                } catch (Exception e) {
                    initLooper();
                }
            }

            @Override
            public void onFailure(Call<QiugouBean> call, Throwable t) {
                initLooper();
            }
        });
    }


    /**
     * 查询产品信息
     *
     * @param carid
     * @param userid
     */
    private void queryCarInfo(String carid, String userid) {
        MyApp.isLocked = true;
        String auth = HttpUtils.getMd5(Constants.USER, Constants.PASS, Constants.TIME);

        CarDetailService service = HttpUtils.getInstance().getRetrofit().create(CarDetailService.class);
        service.getCarInfo(carid, userid, Constants.USER,
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
                                if (urlList.size() > 0 && mWeakReference.get() != null) {
                                    //获取分享文字内容
                                    Intent intent = new Intent();
                                    intent.setClass(MainActivity.this, DownPicService.class);
                                    String nickName;
                                    String headImg;
                                    if (body.getData() != null && body.getData().getUserInfo() != null) {
                                        nickName = body.getData().getUserInfo().getNickName();
                                        headImg = body.getData().getUserInfo().getHeadImg();
                                    } else {
                                        nickName = "叉车圈用户";
                                        headImg = "";
                                    }

                                    intent.putExtra(Constants.KEY_SHARE_METE_DATA,
                                            new ShareMeteBean(urlList, getInformation(body), body.getData().getWaterImage(), nickName, body.getData().getName(), headImg));
                                    //开启下载服务
                                    startService(intent);
                                }
                            }
                        } else {
                            Log.d("xxx", "查询车辆信息错误，请手动点击分享！");
                        }
                        initLooper();
                    }

                    @Override
                    public void onFailure(Call<CarDetailBean> call, Throwable t) {
                        Log.w("onFailure", "查询产品信息报错" + t.toString());
                        initLooper();
                    }
                });
    }

    /**
     * 初始化推送消息轮询
     */
    private void initLooper() {
        mHandler.sendEmptyMessageDelayed(LOOPER_PUSH_DATA, 5 * 1000);
    }

    private String str = "车型：%s\n年限：%s年\n价格：%s\n地址：%s\n电话：%s\n描述：%s\n%s";
    private String strNoDesc = "车型：%s\n年限：%s年\n价格：%s\n地址：%s\n电话：%s\n%s";

    /**
     * 组装要分享的文字内容
     *
     * @param data
     * @return
     */
    private String getInformation(CarDetailBean data) {
//        StringBuilder sb = new StringBuilder();
        String content = (String) SpUtils.get(this, Constants.KEY_WECHAT_CONTENT, "如需分享信息请将你的车辆发布至叉车圈");

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
     * 初始化布局
     */
    private void initView() {
        swipLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        swipLayout.setColorSchemeResources(android.R.color.holo_blue_light,
                android.R.color.holo_red_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_green_light);

        refreshView = (RecyclerView) findViewById(R.id.recyclerview);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);


        findViewById(R.id.bt_settings).setOnClickListener(this);
        //加载出错页面
        mEmptyLayout = findViewById(R.id.include_empty_layout);
        mEmptyLayout.findViewById(R.id.net_error_layout).setOnClickListener(this);

        progressView = (ProgressView) findViewById(R.id.progressView);
        progressView.setOnTimeEndListener(new ProgressView.OnTimeEndListener() {
            @Override
            public void onTimeEnd() {
                progressView.setVisibility(View.GONE);
                progressView.endCount();
            }
        });
    }

    private void initData(final int state) {
        showProgress();
        retrofit = HttpUtils.getInstance().getRetrofit();
        CarListService carListService = retrofit.create(CarListService.class);
        auth = HttpUtils.getMd5(user, pass, time);
        carListService.getCarList(user, pass, time, auth, page, size)
                .enqueue(new Callback<List<CarInfoBean>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<CarInfoBean>> call, @NonNull Response<List<CarInfoBean>> response) {

                        List<CarInfoBean> body = response.body();
                        if (body != null) {
                            Log.w("xxxx访问成功", "数据长度" + body.size());
                            hideProgress();
                            mCarList = body;
                            mHandler.sendEmptyMessage(state);
                            swipLayout.setRefreshing(false);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<CarInfoBean>> call, Throwable t) {
                        hideProgress();
                        Log.w("xxxx报错了", t.toString());
                        mHandler.sendEmptyMessage(ERROR);
                    }
                });
    }

    @Override
    public void onRefresh() {
        page = 1;
        initData(REFRESH);
    }

    public void loadMore() {
        page++;
        initData(LOADMORE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
    }

    private void showEmptyLayout() {
        swipLayout.setVisibility(View.GONE);
    }


    private void showNetErrorLayout() {
        mEmptyLayout.setVisibility(View.VISIBLE);
    }

    private void hideErrorLayout() {
        mEmptyLayout.setVisibility(View.GONE);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof MomentBean) {
            MomentBean bean = (MomentBean) arg;
            share(bean.getUriList(), bean.getInformation());
        }
    }


    private synchronized void share(ArrayList<Uri> uris, String content) {
        if (uris.size() == 0) {
            Log.e("xxxxxxxxxxx", "资源为零！！！！！");
            return;
        }
        Intent intent = new Intent();
        ComponentName comp = new ComponentName("com.tencent.mm",
                Constants.WECHAT_SHAREUI_NAME);
        intent.setComponent(comp);
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK).addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        intent.putExtra(Intent.EXTRA_TEXT, content);
        intent.putExtra("Kdescription", content);
        WechatTempContent.describeList.add(content);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_settings:
                Intent intent2 = new Intent();
                intent2.setClass(MainActivity.this, MainSettingsActivity.class);
                startActivity(intent2);
                break;
            case R.id.net_error_layout:
                hideErrorLayout();
                initData(INIT);
                break;
        }
    }
}
