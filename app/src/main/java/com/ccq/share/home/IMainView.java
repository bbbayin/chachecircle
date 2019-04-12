package com.ccq.share.home;

import com.ccq.share.activity.MainActivity;
import com.ccq.share.bean.CarInfoBean;

import java.util.List;

public interface IMainView {

    void showProgress();

    void dismissProgress();

    void showCarList(List<CarInfoBean> list,int state);

    void showErrorView();

    void showMessageDialog(String msg);

    void dismissMessageDialog();

    MainActivity get();
}
