package com.codewind.taximeter.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.codewind.taximeter.R;
import com.codewind.taximeter.activity.MainActivity;
import com.codewind.taximeter.bean.RoutePoint;
import com.codewind.taximeter.map.MyOrientationListener;
import com.codewind.taximeter.util.Utils;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by 994856229 on 2019/2/11.
 * 定位服务 实时显示行驶数据
 */

public class WorkingService extends Service {
    private double currentLatitude,currentLongitude;//当前经纬度
    private LocationClient mLocationClient = null;//定位客户端
    private MyLocationListener myLocationListener;//定位回调接口
    private MyOrientationListener myOrientationListener;//定位方向监听器
    private String rt_time,rt_distance,rt_price;
    //定位图层显示方式
    private MyLocationConfiguration.LocationMode locationMode;
    public ArrayList<RoutePoint> routPointList = new ArrayList<RoutePoint>();
    public int totalDistance = 0;
    public int totalPrice = 0;
    public long beginTime = 0,totalTime = 0;
    private String showDistance,showTime,showPrice;

    Notification notification;
    RemoteViews contentView;

    public boolean isRunning = true;
    public void setRunning(boolean running){isRunning = running;}

    @Override
    public void onCreate() {
        super.onCreate();
        beginTime = System.currentTimeMillis();
        isRunning = true;
        totalDistance = 0;
        totalTime = 0;
        totalPrice = 0;
        routPointList.clear();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initLocation();//定位初始化
        initNotification();//通知栏初始化
        Utils.acquireWakeLock(this);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**初始化定位*/
    private void initLocation(){
        locationMode = MyLocationConfiguration.LocationMode.NORMAL;
        //定位服务的客户端。宿主程序在客户端声明此类，并调用，目前只支持在主线程中启动
        mLocationClient = new LocationClient(this);
        myLocationListener = new MyLocationListener();
        //注册监听器
        mLocationClient.registerLocationListener(myLocationListener);
        //配置定位SDK各配置参数，比如定位模式、定位时间间隔、坐标系类型等
        LocationClientOption mOption = new LocationClientOption();
        //设置坐标类型
        mOption.setCoorType("bd09ll");
        //设置是否需要地址信息，默认为无地址
        mOption.setIsNeedAddress(true);
        //设置是否打开gps进行定位
        mOption.setOpenGps(true);
        //设置扫描间隔，单位是毫秒 当<1000时，定时定位无效
        mOption.setScanSpan(2000);
        mLocationClient.setLocOption(mOption);

        myOrientationListener = new MyOrientationListener(this);
        //通过接口回调来实现实时方向的改变
        myOrientationListener.setOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {

            }
        });
        if(!mLocationClient.isStarted()){
            mLocationClient.start();
        }
        myOrientationListener.start();
    }

    /**通知栏初始化*/
    private void initNotification(){
        int icon = R.mipmap.bike_icon2;
        contentView = new RemoteViews(getPackageName(),R.layout.notification_layout);
        notification = new NotificationCompat.Builder(this).setContent(contentView).setSmallIcon(icon).build();
        Intent notificationIntent = new Intent(this,MainActivity.class);
        notificationIntent.putExtra("flag","notification");
        notification.contentIntent = PendingIntent.getActivity(this,0,notificationIntent,0);
    }

    /**开始发送通知*/
    private void startNotifi(String time,String distance,String price){
        startForeground(1,notification);
        contentView.setTextViewText(R.id.bike_time,time);
        contentView.setTextViewText(R.id.bike_distance,distance);
        contentView.setTextViewText(R.id.bike_price,price);
        rt_distance = distance;
        rt_price = price;
        rt_time = time;
    }
    /**所有定位信息都通过接口回调来实现*/
    public class MyLocationListener implements BDLocationListener{
        //是否是第一次定位
        private boolean isFirstIn = true;
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            if (null == bdLocation) return;
            if (!isRunning) return;
            //"4.9E-324"表示目前所处的环境（室内或者是网络状况不佳）造成无法获取到经纬度
            if("4.9E-324".equals(String.valueOf(bdLocation.getLatitude())) || "4.9E-324".equals(String.valueOf(bdLocation.getLongitude()))){
                return;
            }//过滤百度定位失败
            double routelate = bdLocation.getLatitude();
            double routeLng = bdLocation.getLongitude();
            RoutePoint routePoint = new RoutePoint();
            routePoint.setRouteLat(routelate);
            routePoint.setRouteLng(routeLng);
            if (routPointList.size()==0){
                routPointList.add(routePoint);
            }else{
                RoutePoint lastPoint = routPointList.get(routPointList.size()-1);
                if(routelate == lastPoint.getRouteLat()&&routeLng == lastPoint.getRouteLng()){

                }else{
                    LatLng lastLatLng = new LatLng(lastPoint.getRouteLat(),lastPoint.getRouteLng());
                    LatLng currentLatLng = new LatLng(routelate,routeLng);
                    if(routelate>0&&routeLng>0){
                        double distance = DistanceUtil.getDistance(lastLatLng,currentLatLng);
                        //大于两米算作有效加入列表
                        if(distance>2){
                            //distance单位是米，转化为km/h
                            routePoint.speed = Double.parseDouble(String.format("%.1f",(distance/1000)*30*60));
                            routePoint.time = System.currentTimeMillis();
                            routPointList.add(routePoint);
                            totalDistance+=distance;
                        }
                    }
                }
            }
            totalTime = (int)(System.currentTimeMillis()-beginTime)/1000/60;
            totalPrice = (int)(Math.floor(totalTime)*1+1);

            if(totalDistance>1000){
                DecimalFormat df = new DecimalFormat("#.00");
                showDistance = df.format((float)totalDistance/1000)+"千米";
            }else{
                showDistance = totalDistance+"米";
            }

            if(totalTime>60){
                showTime = totalTime/60+"时"+totalTime%60+"分";
            }else{
                showTime = totalTime+"分钟";
            }
            showPrice = totalPrice+"元";
            showRouteInfo(showTime,showDistance,showPrice);

        }
    }
    private void showRouteInfo(String time,String distance,String price){
        Intent intent = new Intent("com.locationreceiver");
        Bundle bundle = new Bundle();
        bundle.putString("totalTime",time);
        bundle.putString("totalPrice",price);
        bundle.putString("totalDistance",distance);
        intent.putExtras(bundle);
        sendBroadcast(intent);

        startNotifi(time, distance, price);
    }
}
