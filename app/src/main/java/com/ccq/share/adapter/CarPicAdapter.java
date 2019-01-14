package com.ccq.share.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.chacq.share.R;
import com.ccq.share.bean.CarInfoBean;
import com.ccq.share.utils.DensityUtils;
import com.previewlibrary.ThumbViewInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/8/28.
 */

public class CarPicAdapter extends BaseAdapter {

    private List<CarInfoBean.PicImgBean> mPicList;
    private int count;
    private ArrayList<ThumbViewInfo> mThumList;

    public CarPicAdapter(List<CarInfoBean.PicImgBean> list, int count) {
        mPicList = list;
        this.count = count;
        mThumList = new ArrayList<>();
        for (int i = 0; i < mPicList.size(); i++) {
            mThumList.add(new ThumbViewInfo(mPicList.get(i).getSavename() + "!auto"));
        }
    }

    public ArrayList<ThumbViewInfo> getThumbList() {
        return mThumList;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            holder = new Holder();
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pic, parent, false);
            holder.imageView = (ImageView) convertView.findViewById(R.id.imageview);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        ViewGroup.LayoutParams layoutParams = holder.imageView.getLayoutParams();
        layoutParams.width = DensityUtils.dp2px(parent.getContext(), 90);
        layoutParams.height = DensityUtils.dp2px(parent.getContext(), 90);
        holder.imageView.setLayoutParams(layoutParams);

        Glide.with(parent.getContext())
                .load(getImageUrl(mPicList.get(position).getSavename()))
                .into(holder.imageView);
        return convertView;
    }

    private String getImageUrl(String url) {
        return url + "!150auto";
    }

    static class Holder {
        public ImageView imageView;
    }
}
