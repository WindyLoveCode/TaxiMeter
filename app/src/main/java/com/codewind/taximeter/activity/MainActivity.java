package com.codewind.taximeter.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.codewind.taximeter.R;
import com.zaaach.citypicker.CityPicker;
import com.zaaach.citypicker.adapter.OnPickListener;
import com.zaaach.citypicker.model.City;
import com.zaaach.citypicker.model.LocateState;
import com.zaaach.citypicker.model.LocatedCity;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {
    private SDKReceiver mReceiver;

    /**
     * 构造广播监听类，监听 SDK key 验证以及网络异常广播
     */
    public class SDKReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            String s = intent.getAction();

            if (s.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)) {
                Toast.makeText(MainActivity.this,"apikey验证失败，地图功能无法正常使用",Toast.LENGTH_SHORT).show();
            } else if (s.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK)) {
                Toast.makeText(MainActivity.this,"apikey验证成功",Toast.LENGTH_SHORT).show();
            } else if (s.equals(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)) {
                Toast.makeText(MainActivity.this,"网络错误",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // apikey的授权需要一定的时间，在授权成功之前地图相关操作会出现异常；apikey授权成功后会发送广播通知，我们这里注册 SDK 广播监听者
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK);
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
        iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
        mReceiver = new SDKReceiver();
        registerReceiver(mReceiver, iFilter);
    }
    @AfterViews
    void initView(){
        CityPicker.from(MainActivity.this)
                .enableAnimation(false)
                .setAnimationStyle(R.style.CustomAnim)
                .setLocatedCity(null)
                .setHotCities(null)
                .setOnPickListener(new OnPickListener() {
                    @Override
                    public void onPick(int position, City data) {
//                        currentTV.setText(String.format("当前城市：%s，%s", data.getName(), data.getCode()));
                        Toast.makeText(
                                getApplicationContext(),
                                String.format("点击的数据：%s，%s", data.getName(), data.getCode()),
                                Toast.LENGTH_SHORT)
                                .show();
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(getApplicationContext(), "取消选择", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLocate() {
                        //开始定位，这里模拟一下定位
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                CityPicker.from(MainActivity.this).locateComplete(new LocatedCity("深圳", "广东", "101280601"), LocateState.SUCCESS);
                            }
                        }, 3000);
                    }
                })
                .show();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}
