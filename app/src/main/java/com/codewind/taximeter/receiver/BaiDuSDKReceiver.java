package com.codewind.taximeter.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;

/**
 * 构造广播监听类，监听 SDK key 验证以及网络异常广播
 */
public class BaiDuSDKReceiver extends BroadcastReceiver {
    private Context context;
    public BaiDuSDKReceiver(Context context){
        this.context = context;
    }
    public void onReceive(Context context, Intent intent) {
        String s = intent.getAction();

        if (s.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)) {
            Toast.makeText(context,"apikey验证失败，地图功能无法正常使用",Toast.LENGTH_SHORT).show();
        } else if (s.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK)) {
            Toast.makeText(context,"apikey验证成功",Toast.LENGTH_SHORT).show();
        } else if (s.equals(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)) {
            Toast.makeText(context,"网络错误",Toast.LENGTH_SHORT).show();
        }
    }
}
