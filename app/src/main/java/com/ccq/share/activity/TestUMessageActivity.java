package com.ccq.share.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.ccq.share.R;
import com.umeng.message.entity.UMessage;

import org.json.JSONException;
import org.json.JSONObject;

public class TestUMessageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_umessage_layout);
        findViewById(R.id.bt_send_message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("type","car");
                    jsonObject.put("carid","113");
                    jsonObject.put("userid","110");
                    UMessage uMessage = new UMessage(jsonObject);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
