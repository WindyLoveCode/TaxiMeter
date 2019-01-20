package com.codewind.taximeter.activity;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Bundle;

import com.baidu.mapapi.SDKInitializer;
import com.codewind.taximeter.R;
import com.codewind.taximeter.receiver.BaiDuSDKReceiver;

import org.androidannotations.annotations.EActivity;

@EActivity(R.layout.activity_welcome)
public class WelcomeActivity extends BaseActivity {
    private static final int SPLASH_DISPLAY_LENGHT = 5000;
    private BaiDuSDKReceiver mReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // apikey的授权需要一定的时间，在授权成功之前地图相关操作会出现异常；apikey授权成功后会发送广播通知，我们这里注册 SDK 广播监听者
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK);
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
        iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
        mReceiver = new BaiDuSDKReceiver(WelcomeActivity.this);
        registerReceiver(mReceiver, iFilter);
        /**等待几秒进入主界面*/
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(WelcomeActivity.this,
                        MainActivity_.class);
                startActivity(intent);
                finish();
            }

        }, SPLASH_DISPLAY_LENGHT);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}
