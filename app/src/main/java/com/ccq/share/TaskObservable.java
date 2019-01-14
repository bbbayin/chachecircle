package com.ccq.share;


import android.util.Log;

import java.util.Observable;

/**
 * 自动分享完毕，通知观察者进行下一个任务
 *
 * Created by Administrator on 2017/10/25.
 */

public class TaskObservable extends Observable {
    public void notifyShareFinish(){
        Log.d("TaskObservable","分享朋友圈结束，通知下一个任务！");
        setChanged();
        notifyObservers();
    }
}
