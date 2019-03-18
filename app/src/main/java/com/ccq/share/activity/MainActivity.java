package com.ccq.share.activity;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.ccq.share.Constants;
import com.ccq.share.R;
import com.ccq.share.adapter.ProductAdapter;
import com.ccq.share.bean.CarInfoBean;
import com.ccq.share.core.ImageDownloadManager;
import com.ccq.share.home.IMainView;
import com.ccq.share.home.MainPresenter;
import com.ccq.share.utils.PermissionUtils;
import com.ccq.share.utils.ToastUtil;
import com.ccq.share.view.ProgressView;
import com.umeng.message.PushAgent;
import com.umeng.message.UmengMessageHandler;
import com.umeng.message.entity.UMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener, IMainView {
    public static MainActivity instance;
    public static final int INIT = 1;//初始化
    public static final int REFRESH = 2;
    public static final int LOADMORE = 3;

    private static final int CLEAR_DATA = 4;
    private static final int ERROR = -1;//加载出错
    private static final int LOOPER_PUSH_DATA = 111;//查询推送来的消息列表
    private static final int SHOW_DELAY_PROGRESS = 5;//显示延时进度框
    private SwipeRefreshLayout swipLayout;
    private RecyclerView refreshView;
    private ProductAdapter adapter;
    private ProgressBar progressBar;
    private List<CarInfoBean> mCarList;
    private MainPresenter mPresenter;

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
                            adapter.setShareListener(new ProductAdapter.ShareListener() {
                                @Override
                                public void onShare(String carid, String userid) {
                                    try {
                                        String format = String.format("{\"display_type\":\"notification\",\"extra\":{\"type\":\"car\",\"userid\":\"%s\",\"carid\":\"%s\"},\"msg_id\":\"uulkaxn155289114121410\",\"body\":{\"after_open\":\"go_app\",\"play_lights\":\"false\",\"ticker\":\"放辣椒了\",\"play_vibrate\":\"false\",\"text\":\"发链接\",\"title\":\"放辣椒了\",\"play_sound\":\"true\"},\"random_min\":0}", userid, carid);
                                        JSONObject jsonObject = new JSONObject(format);
                                        UMessage uMessage = new UMessage(jsonObject);
                                        mPresenter.putMessagePool(uMessage);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
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
                    ToastUtil.show("清除成功！");
                    break;
                case ERROR:
                    if (adapter == null || mCarList == null || mCarList.size() == 0)
                        mEmptyLayout.setVisibility(View.VISIBLE);
                    else
                        ToastUtil.show("网络出现错误，请稍后再试");
                    break;
//                case LOOPER_PUSH_DATA:
//                    if (isNeedDelay) {
//                        progressView.setVisibility(View.GONE);
//                        progressView.endCount();
//                    }
//                    if (MyApp.sShareDataSource.size() > 0) {
//                        PushBean remove = MyApp.sShareDataSource.remove(0);
//                        if (remove.getType() == PushBean.TYPE_SELL) {
//                            //售车
//                            queryCarInfo(remove.getCarid(), remove.getUserid());
//                        } else if (remove.getType() == PushBean.TYPE_BUY) {
//                            //求购
//                            wantBuy(remove.getId());
//                        }
//                    } else {
//                        MyApp.isLocked = false;
//                    }
//                    break;
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

    public static final String TAG = "MainActivity";
    /**
     * 收到推送消息的回调
     */
    UmengMessageHandler messageHandler = new UmengMessageHandler() {
        @Override
        public Notification getNotification(Context context, final UMessage uMessage) {
            Log.d(TAG, "收到消息：" + uMessage.text);
            mPresenter.putMessagePool(uMessage);
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
        }
    };


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

        mPresenter = new MainPresenter(this);
        mPresenter.getCarList(INIT);
        // 下载任务
        ImageDownloadManager.getINSTANCE().init(mWeakReference.get());
        //推送服务
        mPushAgent = PushAgent.getInstance(this);
        mPushAgent.onAppStart();
        mPushAgent.setMessageHandler(messageHandler);
        findViewById(R.id.title).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < 5; i++) {
                    //{"policy":{"expire_time":"2019-03-20 14:58:38"},"description":"b","production_mode":false,"appkey":"59a6bf86310c935cd1000c8f","payload":{"body":{"title":"放辣椒了","ticker":"放辣椒了","text":"发链接","after_open":"go_app","play_vibrate":"false","play_lights":"false","play_sound":"true"},"display_type":"notification","extra":{"type":"car","userid":"12359","carid":"200538"}},"device_tokens":"Aoi6G8zeTflsGfBRqYucmv_yDiJBGP4Kk6UrWGoitBFO","type":"unicast","timestamp":"1552890822251"}
                    try {
                        JSONObject jsonObject = new JSONObject("{\"display_type\":\"notification\",\"extra\":{\"type\":\"car\",\"userid\":\"12359\",\"carid\":\"200538\"},\"msg_id\":\"uulkaxn155289114121410\",\"body\":{\"after_open\":\"go_app\",\"play_lights\":\"false\",\"ticker\":\"放辣椒了\",\"play_vibrate\":\"false\",\"text\":\"发链接\",\"title\":\"放辣椒了\",\"play_sound\":\"true\"},\"random_min\":0}");
                        UMessage uMessage = new UMessage(jsonObject);
                        mPresenter.putMessagePool(uMessage);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        if (!Constants.isAutoShare) {
////            showAlertDialog();
//        }
//        //检测延时
//        delay = (int) SpUtils.get(this, Constants.KEY_DELAY_TIME, 0);
//        if (delay > 0) {
//            isNeedDelay = true;
//            progressView.setTime(delay);
//        } else isNeedDelay = false;
//    }

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


    /**
     * 求购
     *
     * @param id
     */
//    private void wantBuy(String id) {
//        MyApp.isLocked = true;
//        QiuGouService qiuGouService = HttpUtils.getInstance().getRetrofit().create(QiuGouService.class);
//        String auth = HttpUtils.getMd5(Constants.USER, Constants.PASS, Constants.TIME);
//        qiuGouService.getQiugou(id, Constants.USER,
//                Constants.PASS, Constants.TIME, auth).enqueue(new Callback<QiugouBean>() {
//            @Override
//            public void onResponse(Call<QiugouBean> call, Response<QiugouBean> response) {
//                try {
//                    int code = response.body().getCode();
//                    if (code == 0) {
//                        //成功
//                        QiugouBean.DataBean data = response.body().getData();
//                        String content = data.getContent();
//                        ArrayList<String> urlList = new ArrayList<>();
//                        urlList.add(Constants.qiugou_img_url);
//                        StringBuilder stringBuffer = new StringBuilder();
//                        stringBuffer.append("【求购】");
//                        stringBuffer.append(content).append("。");
//                        if (!TextUtils.isEmpty(data.getAddress())) {
//                            stringBuffer.append(data.getAddress()).append(",");
//                        }
//                        if (!TextUtils.isEmpty(data.getTitle())) {
//                            stringBuffer.append(data.getTitle()).append("，");
//                        }
//                        stringBuffer.append("电话").append(data.getPhone()).append("。");
//                        String end = (String) SpUtils.get(MainActivity.this, Constants.KEY_QIUGOU_END, "如有意向请致电");
//                        stringBuffer.append(end);
////                        MomentBean momentBean = new MomentBean();
////                        momentBean.setInformation(stringBuffer.toString());
////                        DownLoadUtils.getInstance().downLoadPic(urlList, momentBean, mWeakReference.get());
//                        // TODO: 2019/1/28
//                        Intent intent = new Intent();
//                        intent.setClass(MainActivity.this, DownPicService.class);
//
//                        intent.putExtra(Constants.KEY_SHARE_METE_DATA,
//                                new ShareMeteBean(urlList, stringBuffer.toString(), "", "", "", ""));
//                        //开启下载服务
//                        startService(intent);
//                    }
//                    initLooper();
//                } catch (Exception e) {
//                    initLooper();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<QiugouBean> call, Throwable t) {
//                initLooper();
//            }
//        });
//    }


    /**
     * 查询产品信息
     *
     * @param carid
     * @param userid
     */
//    private void queryCarInfo(String carid, String userid) {
//        MyApp.isLocked = true;
//        String auth = HttpUtils.getMd5(Constants.USER, Constants.PASS, Constants.TIME);
//
//        CarDetailService service = HttpUtils.getInstance().getRetrofit().create(CarDetailService.class);
//        service.getCarInfo(carid, userid, Constants.USER,
//                Constants.PASS, Constants.TIME, auth)
//                .enqueue(new Callback<CarDetailBean>() {
//                    @Override
//                    public void onResponse(@NonNull Call<CarDetailBean> call, @NonNull Response<CarDetailBean> response) {
//                        CarDetailBean body = response.body();
//                        if (body != null) {
//                            if (body.getCode() == 0) {
//                                Log.w("onResponse", body.getData().getContent());
//                                //获取图片url
//                                ArrayList<String> urlList = new ArrayList<>();
//                                List<CarDetailBean.DataBean.CImagesBean> cImages = body.getData().getCImages();
//
//                                for (CarDetailBean.DataBean.CImagesBean bean :
//                                        cImages) {
//                                    urlList.add(bean.getSavename());
//                                }
//                                //获取分享文字内容
//                                if (urlList.size() > 0 && mWeakReference.get() != null) {
//                                    //获取分享文字内容
//                                    Intent intent = new Intent();
//                                    intent.setClass(MainActivity.this, DownPicService.class);
//                                    String nickName;
//                                    String headImg;
//                                    if (body.getData() != null && body.getData().getUserInfo() != null) {
//                                        nickName = body.getData().getUserInfo().getNickName();
//                                        headImg = body.getData().getUserInfo().getHeadImg();
//                                    } else {
//                                        nickName = "铲车圈用户";
//                                        headImg = "";
//                                    }
//
//                                    intent.putExtra(Constants.KEY_SHARE_METE_DATA,
//                                            new ShareMeteBean(urlList, getInformation(body), body.getData().getWaterImage(), nickName, body.getData().getName(), headImg));
//                                    //开启下载服务
//                                    startService(intent);
//                                }
//                            }
//                        } else {
//                            Log.d("xxx", "查询车辆信息错误，请手动点击分享！");
//                        }
//                        initLooper();
//                    }
//
//                    @Override
//                    public void onFailure(Call<CarDetailBean> call, Throwable t) {
//                        Log.w("onFailure", "查询产品信息报错" + t.toString());
//                        initLooper();
//                    }
//                });
//    }

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
//    private String getInformation(CarDetailBean data) {
////        StringBuilder sb = new StringBuilder();
//        String content = (String) SpUtils.get(this, Constants.KEY_WECHAT_CONTENT, "如需分享信息请将你的车辆发布至铲车圈");
//
//        synchronized (MainActivity.class) {
//            CarDetailBean.DataBean detail = data.getData();
//            String strPrice = detail.getPrice() + "万";
//            try {
//                if (detail.getPrice() <= 0) {
//                    strPrice = "价格面议";
//                }
//            } catch (Exception e) {
//                strPrice = "价格面议";
//            }
//            if (detail.getContent() == null) {
//                return String.format(strNoDesc, detail.getName(), detail.getYear(), strPrice,
//                        detail.getProvinceName() + detail.getCityName(), detail.getPhone(), content);
//            } else {
//                if (TextUtils.isEmpty(detail.getContent().trim())) {
//                    return String.format(strNoDesc, detail.getName(), detail.getYear(), strPrice,
//                            detail.getProvinceName() + detail.getCityName(), detail.getPhone(), content);
//                } else {
//                    return String.format(str, detail.getName(), detail.getYear(), strPrice,
//                            detail.getProvinceName() + detail.getCityName(), detail.getPhone(), detail.getContent(), content);
//                }
//            }
//        }
//    }

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


    @Override
    public void onRefresh() {
        mPresenter.getCarList(REFRESH);
    }

    public void loadMore() {
        mPresenter.getCarList(LOADMORE);
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

    @Override
    public void showProgress() {
        swipLayout.setRefreshing(true);
    }

    @Override
    public void dismissProgress() {
        swipLayout.setRefreshing(false);
    }

    @Override
    public void showCarList(List<CarInfoBean> list, int state) {
        this.mCarList = list;
        mHandler.sendEmptyMessage(state);
    }

    @Override
    public void showErrorView() {
        mHandler.sendEmptyMessage(ERROR);
    }

    @Override
    public MainActivity get() {
        return mWeakReference.get();
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_settings:
                Intent intent2 = new Intent();
                intent2.setClass(MainActivity.this, MainSettingsActivity.class);
                startActivity(intent2);
                break;
            case R.id.net_error_layout:
                hideErrorLayout();
                mPresenter.getCarList(INIT);
                break;
        }
    }
}
