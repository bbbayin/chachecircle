package com.ccq.share.activity;

import android.os.Bundle;
import android.app.Activity;
import android.text.TextUtils;
import android.widget.TextView;

import com.ccq.share.LogUtils;
import com.ccq.share.R;

public class ShareLogActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);

        TextView tvLog = findViewById(R.id.tv_log);

        String readlog = LogUtils.readlog();
        if (!TextUtils.isEmpty(readlog))
            tvLog.setText(readlog);
    }
}
