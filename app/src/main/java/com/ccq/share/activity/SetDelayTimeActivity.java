package com.ccq.share.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ccq.share.Constants;
import com.chacq.share.R;
import com.ccq.share.utils.SpUtils;
import com.wizchen.topmessage.TopMessageManager;

/**
 * Created by Administrator on 2017/11/14.
 */

public class SetDelayTimeActivity extends Activity implements View.OnClickListener {

    private SeekBar mSeekbar;
    private int maxDelay = 60;
    private TextView mTvDelay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_delay_time);


        mTvDelay = (TextView) findViewById(R.id.tv_delay);
        mSeekbar = (SeekBar) findViewById(R.id.seekbar);
        mSeekbar.setMax(60);
        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTvDelay.setText(progress + "秒");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        //init
        int delay = (int) SpUtils.get(this, Constants.KEY_DELAY_TIME, 0);
        mTvDelay.setText(delay + "秒");
        mSeekbar.setProgress(delay);
        findViewById(R.id.bt_save_delay).setOnClickListener(this);
        findViewById(R.id.iv_back).setOnClickListener(this);
        findViewById(R.id.tv_clear_delay).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_save_delay:
                int delay = mSeekbar.getProgress();
                if (delay > 0) {
                    SpUtils.put(this, Constants.KEY_DELAY_TIME, delay);
                    TopMessageManager.showSuccess("设置延迟成功！" + delay + "秒");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    }, 1300);
                } else {
                    TopMessageManager.showInfo("设置延迟时间要大于0秒");
                }
                break;
            case R.id.tv_clear_delay:
                SpUtils.remove(this, Constants.KEY_DELAY_TIME);
                TopMessageManager.showSuccess("清除延迟成功！");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 1300);
                break;
            case R.id.iv_back:
                finish();
                break;
        }
    }
}
