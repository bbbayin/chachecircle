package com.ccq.share.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.ccq.share.Constants;
import com.chacq.share.R;
import com.ccq.share.utils.SpUtils;
import com.wizchen.topmessage.TopMessageManager;

/****************************************
 * 功能说明:  设置微信结束语页面
 *
 * Author: Created by bayin on 2017/8/28.
 ****************************************/

public class SetWechatContentActivity extends AppCompatActivity {
    public static final int SUCCESS = 1;
    private Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case SUCCESS:
                    finish();
                    break;
            }
        }
    };

    private EditText mEtSellContent,mEtBuy;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_layout);
        mEtSellContent = (EditText) findViewById(R.id.et_sell_content);
        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.bt_save_sell).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = mEtSellContent.getText().toString().trim();
                if (TextUtils.isEmpty(content)){
                    TopMessageManager.showInfo("内容不能为空");
                }else {
                    SpUtils.put(SetWechatContentActivity.this,Constants.KEY_WECHAT_CONTENT,content);
                    TopMessageManager.showSuccess("保存成功！");
                    mHandler.sendEmptyMessageDelayed(SUCCESS,2000);
                }
            }
        });


        mEtBuy = (EditText) findViewById(R.id.et_buy_content);
        findViewById(R.id.bt_save_buy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = mEtBuy.getText().toString().trim();
                if (TextUtils.isEmpty(content)){
                    TopMessageManager.showInfo("内容不能为空");
                }else {
                    SpUtils.put(SetWechatContentActivity.this,Constants.KEY_QIUGOU_END,content);
                    TopMessageManager.showSuccess("保存成功！");
                    mHandler.sendEmptyMessageDelayed(SUCCESS,2000);
                }
            }
        });
    }
}
