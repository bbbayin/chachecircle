package com.ccq.share.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;

import com.ccq.share.R;
import com.ccq.share.Constants;
import com.ccq.share.utils.FileUtils;
import com.ccq.share.utils.PermissionUtils;
import com.ccq.share.utils.SpUtils;
import com.wizchen.topmessage.TopMessageManager;

import java.io.File;

/****************************************
 * 功能说明:  
 *
 * Author: Created by bayin on 2017/8/31.
 ****************************************/

public class MainSettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private Switch mSwitchButton;
    private ProgressBar mProgressbar;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    mProgressbar.setVisibility(View.GONE);
                    TopMessageManager.showSuccess("清除成功！");
                    break;
            }
        }
    };
    private EditText mEtToken;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mSwitchButton = (Switch) findViewById(R.id.switch1);
        mSwitchButton.setOnClickListener(this);

        mProgressbar = (ProgressBar) findViewById(R.id.progressbar);
        findViewById(R.id.clear_cache).setOnClickListener(this);
        findViewById(R.id.edit_wechat).setOnClickListener(this);
        findViewById(R.id.iv_back).setOnClickListener(this);
        findViewById(R.id.set_delay).setOnClickListener(this);
        mEtToken = (EditText) findViewById(R.id.et_token);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Constants.isAutoShare = PermissionUtils.serviceIsRunning(this);
        mSwitchButton.setChecked(Constants.isAutoShare);

        //设置token
        String token = (String) SpUtils.get(getApplicationContext(), Constants.key_TOKEN, "token获取失败");
        mEtToken.setText(token);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.edit_wechat:
                //编辑微信
                Intent intent = new Intent();
                intent.setClass(MainSettingsActivity.this, SetWechatContentActivity.class);
                startActivity(intent);
                break;
            case R.id.clear_cache:
                //清除缓存
                mProgressbar.setVisibility(View.VISIBLE);
                File file = new File(Constants.SD_ROOTPATH);
                FileUtils.deleteDirWithFile(file);
                mHandler.sendEmptyMessageDelayed(1, 3000);
                break;
            case R.id.iv_back:
                finish();
                break;
            case R.id.switch1:
                Intent access = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(access);
                break;
            case R.id.set_delay:
                Intent intent1 = new Intent(this, SetDelayTimeActivity.class);
                startActivity(intent1);
                break;
        }
    }
}
