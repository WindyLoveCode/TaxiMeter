package com.codewind.taximeter.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.codewind.taximeter.R;
import com.codewind.taximeter.service.WorkingService;
import com.codewind.taximeter.util.Utils;
import com.zaaach.citypicker.CityPicker;
import com.zaaach.citypicker.adapter.OnPickListener;
import com.zaaach.citypicker.model.City;
import com.zaaach.citypicker.model.LocateState;
import com.zaaach.citypicker.model.LocatedCity;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

@EActivity(R.layout.activity_main)
public class MainActivity extends BaseActivity implements SensorEventListener {
    /**城市选择*/
    @ViewById(R.id.layout_main_city) LinearLayout layout_city;
    @ViewById(R.id.text_main_city) TextView text_city;
    @ViewById(R.id.text_main_distance)
    static TextView text_distance;
    @ViewById(R.id.text_main_time)
    static TextView text_time;
    @ViewById(R.id.text_main_price)
    static TextView text_price;
    /**设置*/
    @ViewById(R.id.text_main_set) TextView text_set;
    /**百度地图*/
    @ViewById(R.id.mapview_main) MapView mapView;
    private BaiduMap baiduMap;
    private float mCurrentZoom = 18f;//默认地图缩放比例值
    /**定位相关*/
    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();
    private int mCurrentDirection = 0;
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;

    boolean isFirstLoc = true; // 是否首次定位
    private MyLocationData locData;
    private SensorManager mSensorManager;//传感器管理服务

    private MapStatus.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
    @AfterViews
    void initView(){
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);// 获取传感器管理服务

        //不显示百度地图logo
        View child = mapView.getChildAt(1);
        if (child != null && (child instanceof ImageView || child instanceof ZoomControls)) {
            child.setVisibility(View.INVISIBLE);
        }
        // 不显示地图上比例尺
        mapView.showScaleControl(false);
        // 不显示地图缩放控件（按钮控制栏）
        mapView.showZoomControls(false);

        baiduMap = mapView.getMap();
        //开启定位图层
        baiduMap.setMyLocationEnabled(true);
        baiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                com.baidu.mapapi.map.MyLocationConfiguration.LocationMode.FOLLOWING, true, null));
        /**添加地图缩放状态变化监听，当手动放大或缩小地图时，拿到缩放后的比例，然后获取到下次定位，给地图重新设置缩放比例，否则地图会重新回到默认的mCurrentZoom缩放比例*/
        baiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {

            @Override
            public void onMapStatusChangeStart(MapStatus arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onMapStatusChangeFinish(MapStatus arg0) {
                mCurrentZoom = arg0.zoom;
            }

            @Override
            public void onMapStatusChange(MapStatus arg0) {
                // TODO Auto-generated method stub

            }
        });

        // 定位初始化
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);//只用gps定位，需要在室外定位。
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(2000);
        mLocClient.setLocOption(option);
        mLocClient.start();
        Intent intent = new Intent(this, WorkingService.class);
        startService(intent);
    }
    /**点击选择城市*/
    @Click(R.id.layout_main_city)
    void click_city(){
                CityPicker.from(MainActivity.this)
                .enableAnimation(false)
                .setAnimationStyle(R.style.CustomAnim)
                .setLocatedCity(null)
                .setHotCities(null)
                .setOnPickListener(new OnPickListener() {
                    @Override
                    public void onPick(int position, City data) {
                        text_city.setText(data.getName()+"市");
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
    /**点击设置*/
    @Click(R.id.text_main_set)
    void click_set(){
        Intent intent_set = new Intent(MainActivity.this,SetActivity_.class);
        startActivity(intent_set);
    }
    /**定位SDK监听函数*/
    public class MyLocationListenner implements BDLocationListener {

        @Override
        public void onReceiveLocation(final BDLocation location) {
            Log.i("MainActivity","---");
            if (location == null || mapView == null) {
                return;
            }

            //注意这里只接受gps点，需要在室外定位。
            if (location.getLocType() == BDLocation.TypeGpsLocation) {
                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                locateAndZoom(location,ll);
            }else{
                Log.i("","当前为室内环境");
            }
        }

    }
    private void locateAndZoom(final BDLocation location, LatLng ll) {
        mCurrentLat = location.getLatitude();
        mCurrentLon = location.getLongitude();
        locData = new MyLocationData.Builder().accuracy(0)
                // 此处设置开发者获取到的方向信息，顺时针0-360
                .direction(mCurrentDirection).latitude(location.getLatitude())
                .longitude(location.getLongitude()).build();
        baiduMap.setMyLocationData(locData);

        builder = new MapStatus.Builder();
        builder.target(ll).zoom(mCurrentZoom);
        baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
    }

    double lastX;
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double x = sensorEvent.values[SensorManager.DATA_X];

        if (Math.abs(x - lastX) > 1.0) {
            mCurrentDirection = (int) x;

            if (isFirstLoc) {
                lastX = x;
                return;
            }

            locData = new MyLocationData.Builder().accuracy(0)
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection).latitude(mCurrentLat).longitude(mCurrentLon).build();
            baiduMap.setMyLocationData(locData);
        }
        lastX = x;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    @Override
    protected void onResume() {
        mapView.onResume();
        super.onResume();
        // 为系统的方向传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);
    }
    @Override
    protected void onStop() {
        // 取消注册传感器监听
        mSensorManager.unregisterListener(this);
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        // 退出时销毁定位
        mLocClient.unRegisterLocationListener(myListener);
        if (mLocClient != null && mLocClient.isStarted()) {
            mLocClient.stop();
        }
        // 关闭定位图层
        baiduMap.setMyLocationEnabled(false);
        mapView.getMap().clear();
        mapView.onDestroy();
        mapView = null;
//        startBD.recycle();
//        finishBD.recycle();
        super.onDestroy();
    }
    public static class LocationReceiver extends BroadcastReceiver {
        public LocationReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Utils.isTopActivity(context)) {
                String time = intent.getStringExtra("totalTime");
                String distance = intent.getStringExtra("totalDistance");
                String price = intent.getStringExtra("totalPrice");
                text_time.setText(time);
                text_distance.setText(distance);
                text_price.setText(price);
            } else {
                Log.d("gaolei", "MainActivity-------TopActivity---------false");
            }
        }
    }
}
