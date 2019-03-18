package com.ccq.share.adapter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.ccq.share.R;
import com.ccq.share.Constants;
import com.ccq.share.MyGridView;
import com.ccq.share.activity.MainActivity;
import com.ccq.share.bean.CarDetailBean;
import com.ccq.share.bean.CarInfoBean;
import com.ccq.share.bean.ShareMeteBean;
import com.ccq.share.core.DownPicService;
import com.ccq.share.http.HttpUtils;
import com.ccq.share.http.PackageUtils;
import com.ccq.share.service.CarDetailService;
import com.ccq.share.utils.DensityUtils;
import com.ccq.share.utils.PhoneUtils;
import com.ccq.share.utils.SpUtils;
import com.ccq.share.utils.ToastUtil;
import com.previewlibrary.PhotoActivity;
import com.previewlibrary.ThumbViewInfo;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Administrator on 2017/8/25.
 */

public class ProductAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {

    private static final int TYPE_ITEM = 1;
    private static final int TYPE_FOOTER = 0;
    private List<CarInfoBean> mDataList;
    private MainActivity context;
    private ArrayList<ThumbViewInfo> mThumbList;
    private ProgressDialog progressDialog;

    public ProductAdapter(List<CarInfoBean> beanList) {
        this.mDataList = beanList;
    }

    public void refresh(List<CarInfoBean> beanList) {
        this.mDataList = beanList;
        notifyDataSetChanged();
    }

    public void loadMore(List<CarInfoBean> beanList) {
        this.mDataList.addAll(beanList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position + 1 == getItemCount()) {
            return TYPE_FOOTER;
        } else {
            return TYPE_ITEM;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = (MainActivity) parent.getContext();
        LayoutInflater from = LayoutInflater.from(context);
        if (viewType == TYPE_FOOTER) {
            View footer = from.inflate(R.layout.load_more_layout, parent, false);
            return new FooterHolder(footer);
        } else if (viewType == TYPE_ITEM) {
            View item = from.inflate(R.layout.item_product_layout, parent, false);
            return new ItemHolder(item);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemHolder) {
            final ItemHolder itemHolder = (ItemHolder) holder;
            CarInfoBean bean = mDataList.get(position);
            //set data
            Glide.with(context).load(bean.getUserInfo().getHeadimgurl()).into(itemHolder.ivUserHeaderImg);
            itemHolder.tvUserNmae.setText(bean.getUserInfo().getNickname());
            itemHolder.tvCarName.setText(bean.getName() + bean.getYear() + "年");
            itemHolder.tvPrice.setText(bean.getPrice() + "万");
            if ((TextUtils.isEmpty(bean.getContent()))) {
                itemHolder.tvInfo.setVisibility(View.GONE);
            } else {
                itemHolder.tvInfo.setVisibility(View.VISIBLE);
                itemHolder.tvInfo.setText(bean.getContent());
            }

            itemHolder.tvAddress.setText(bean.getProvinceName() + "·" + bean.getCityName());
            itemHolder.tvTime.setText(bean.getAddtime_format());
            //pic
            ViewGroup.LayoutParams layoutParams = itemHolder.gridView.getLayoutParams();
            layoutParams.width = DensityUtils.dp2px(context, 90 * 3 + 8);
            itemHolder.gridView.setLayoutParams(layoutParams);
            if (bean.getPic_img() == null || bean.getPic_img().size() == 0) {
                itemHolder.gridView.setVisibility(View.GONE);
            } else {
                itemHolder.gridView.setVisibility(View.VISIBLE);
                final CarPicAdapter adapter = new CarPicAdapter(bean.getPic_img(), bean.getPic_img_count());
                itemHolder.gridView.setAdapter(adapter);
                //准备图片数据
                itemHolder.gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        assembleDataList(itemHolder.gridView);
                        PhotoActivity.startActivity(context, adapter.getThumbList(), position);
                    }
                });
            }


            itemHolder.tvCall.setTag(position);
            itemHolder.tvShare.setTag(position);
            itemHolder.tvCall.setOnClickListener(this);
            itemHolder.tvShare.setOnClickListener(this);
        }
    }

    /**
     * 从第一个完整可见item逆序遍历，如果初始位置为0，则不执行方法内循环
     */
    private void computeBoundsBackward(MyGridView gridView) {
        CarPicAdapter adapter = (CarPicAdapter) gridView.getAdapter();
        for (int i = gridView.getFirstVisiblePosition(); i < adapter.getCount(); i++) {
            View itemView = gridView.getChildAt(i);
            Rect bounds = new Rect();
            if (itemView != null) {
                ImageView thumbView = (ImageView) itemView.findViewById(R.id.imageview);
                thumbView.getGlobalVisibleRect(bounds);
            }
            adapter.getThumbList().get(i).setBounds(bounds);
//            mThumbList.get(i).setBounds(bounds);
        }

    }

    private void assembleDataList(MyGridView gridView) {
        computeBoundsBackward(gridView);
    }

    @Override
    public int getItemCount() {
        if (mDataList != null && mDataList.size() > 0) {
            return mDataList.size() + 1;
        }
        return 0;
    }

    @Override
    public void onClick(View v) {
        int positon = (int) v.getTag();
        CarInfoBean carInfoBean = mDataList.get(positon);
        switch (v.getId()) {
            case R.id.item_tv_call:
                PhoneUtils.call(context, carInfoBean.getPhone());
                break;
            case R.id.item_tv_share:
                if (PackageUtils.isWeixinAvilible(context)) {
                    if (listener != null) {
                        listener.onShare(String.valueOf(carInfoBean.getId()),
                                String.valueOf(carInfoBean.getUserInfo().getUserid()));
                    }
                } else {
                    ToastUtil.show("未安装微信，不能分享！");
                }
                break;
        }
    }

    private ShareListener listener;

    public void setShareListener(ShareListener listener) {
        this.listener = listener;
    }

    public interface ShareListener {
        void onShare(String carid, String userid);
    }

    private void showProgress() {

        if (progressDialog == null) {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("正在查询信息...");
        }

        progressDialog.show();
    }

    private void dissmissProgress() {
        if (progressDialog != null) progressDialog.dismiss();
    }

    private String str = "车型：%s\n年限：%s年\n价格：%s\n地址：%s\n电话：%s\n描述：%s\n%s";
    private String strNoDesc = "车型：%s\n年限：%s年\n价格：%s\n地址：%s\n电话：%s\n%s";


    /**
     * 查询产品信息
     *
     * @param carid
     * @param userid
     */
    private void queryCarInfo(final Context context, String carid, String userid, final String info) {
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
                                Intent intent = new Intent();
                                intent.setClass(context, DownPicService.class);
                                String nickName;
                                String headImg;
                                if (body.getData() != null && body.getData().getUserInfo() != null) {
                                    nickName = body.getData().getUserInfo().getNickName();
                                    headImg = body.getData().getUserInfo().getHeadImg();
                                } else {
                                    nickName = "铲车圈用户";
                                    headImg = "";
                                }

                                intent.putExtra(Constants.KEY_SHARE_METE_DATA,
                                        new ShareMeteBean(urlList, info, body.getData().getWaterImage(), nickName, body.getData().getName(), headImg));
                                //开启下载服务
                                context.startService(intent);
                            }
                        } else {
                            Log.d("xxx", "查询车辆信息错误，请手动点击分享！");
                        }
                        dissmissProgress();
                    }

                    @Override
                    public void onFailure(Call<CarDetailBean> call, Throwable t) {
                        Log.w("onFailure", "查询产品信息报错" + t.toString());
                        dissmissProgress();
                    }
                });
    }

    /**
     * 组装要分享的文字内容
     *
     * @param data
     * @return
     */
    private String getInformation(CarInfoBean data) {
//        StringBuilder sb = new StringBuilder();
        String content = (String) SpUtils.get(context, Constants.KEY_WECHAT_CONTENT, "如需分享信息请将你的车辆发布至铲车圈");

        synchronized (MainActivity.class) {

            String price = data.getPrice();
            String strPrice = data.getPrice() + "万";
            try {
                if (Double.parseDouble(price) <= 0) {
                    strPrice = "价格面议";
                }
            } catch (Exception e) {
                strPrice = "价格面议";
            }

            if (data.getContent() == null) {
                return String.format(strNoDesc, data.getName(), data.getYear(), strPrice,
                        data.getProvinceName() + data.getCityName(), data.getPhone(), content);
            } else {
                if (TextUtils.isEmpty(data.getContent().trim())) {
                    return String.format(strNoDesc, data.getName(), data.getYear(), strPrice,
                            data.getProvinceName() + data.getCityName(), data.getPhone(), content);
                } else {
                    return String.format(str, data.getName(), data.getYear(), strPrice,
                            data.getProvinceName() + data.getCityName(), data.getPhone(), data.getContent(), content);
                }
            }
        }
    }

    static class ItemHolder extends RecyclerView.ViewHolder {

        private final ImageView ivUserHeaderImg;
        private final TextView tvUserNmae;
        private final TextView tvCarName;
        private final TextView tvPrice;
        private final MyGridView gridView;
        private final TextView tvAddress;
        private final TextView tvInfo;
        private final TextView tvTime;
        private final View tvCall;
        private final View tvShare;

        public ItemHolder(View itemView) {
            super(itemView);
            ivUserHeaderImg = (ImageView) findViewById(R.id.item_iv_user_header);
            tvUserNmae = (TextView) findViewById(R.id.item_tv_user_name);
            tvCarName = (TextView) findViewById(R.id.item_tv_car_name);
            tvPrice = (TextView) findViewById(R.id.item_tv_car_price);
            gridView = (MyGridView) findViewById(R.id.item_gridview);
            tvAddress = (TextView) findViewById(R.id.item_tv_car_location);
            tvInfo = (TextView) findViewById(R.id.item_tv_car_info);
            tvTime = (TextView) findViewById(R.id.item_tv_publish_time);
            tvCall = findViewById(R.id.item_tv_call);
            tvShare = findViewById(R.id.item_tv_share);
        }

        private View findViewById(int id) {
            return itemView.findViewById(id);
        }
    }

    static class FooterHolder extends RecyclerView.ViewHolder {

        public FooterHolder(View itemView) {
            super(itemView);
        }
    }
}
