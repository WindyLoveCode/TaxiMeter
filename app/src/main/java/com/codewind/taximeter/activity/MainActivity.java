package com.codewind.taximeter.activity;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
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

    /**欢迎页*/
    @ViewById(R.id.splash_img)
    ImageView splash_img;//欢迎页图片
    private final int DISMISS_SPLASH = 0;//消失标识
    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DISMISS_SPLASH:
                    Animator animator = AnimatorInflater.loadAnimator(MainActivity.this, R.animator.splash);
                    animator.setTarget(splash_img);
                    animator.start();
                    break;
            }
        }
    };

    /**Top*/
    @ViewById(R.id.layout_main_city)
    LinearLayout layout_city;//城市选择
    @ViewById(R.id.text_main_city)
    TextView text_city;//城市选择
    @ViewById(R.id.text_main_set)
    TextView text_set;//设置按钮

    /**行驶中数据显示*/
    @ViewById(R.id.layout_main_working)
    LinearLayout layout_working;//数据显示layout
    @ViewById(R.id.text_main_distance)
    static TextView text_distance;//行驶距离
    @ViewById(R.id.text_main_time)
    static TextView text_time;//行驶时间
    @ViewById(R.id.text_main_price)
    static TextView text_price;//费用计算
    @ViewById(R.id.btn_main_working)
    Button btn_working;//开始结束按钮
    private boolean isWorking = false;
    private boolean isServiceLive = false;//数据刷新服务是否存活

    /**地图*/
    @ViewById(R.id.mapview_main)
    MapView mapView;//地图视图
    private BaiduMap baiduMap;//百度地图
    private float mCurrentZoom = 18f;//默认地图缩放比例值
    private MapStatus.Builder builder;//地图状态

    /**定位*/
    private LocationClient mLocClient;//定位客户端
    public MyLocationListenner myListener = new MyLocationListenner();//定位监听器
    private SensorManager mSensorManager;//传感器管理服务
    private MyLocationData locData;//定位数据
    private double mCurrentLat = 0.0;//当前经度
    private double mCurrentLon = 0.0;//当前纬度
    private int mCurrentDirection = 0;//当前方向
    boolean isFirstLoc = true; // 是否首次定位

    /**退出时间*/
    private long exitTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("MainActivity-LIVE","onCreate");
//        isServiceLive = Utils.isServiceWork(this, "com.codewind.taximeter.service.WorkingService");
//        if(!isServiceLive){
//            beginService();
//        }


    }

    @AfterViews
    void initView(){
        handler.sendEmptyMessageDelayed(DISMISS_SPLASH, 3000);
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
//        option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);//只用gps定位，需要在室外定位。
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(2000);
        mLocClient.setLocOption(option);
        mLocClient.start();
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

    /**点击开始结束行程*/
    @Click(R.id.btn_main_working)
    void click_working(){
        if(isWorking){//行驶中点击按钮则停止行程
            stopWorking();
        }else{//开启行程
            startWorking();
        }
    }
    /**结束行程*/
    private void stopWorking() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("确认要结束进程吗？");
        builder.setTitle("提示");
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                isWorking = false;
                layout_working.setVisibility(View.GONE);
                btn_working.setText("开始");
                btn_working.setBackgroundResource(R.drawable.btn_bg_working_start);
                dialog.dismiss();
                Intent intent = new Intent(MainActivity.this, WorkingService.class);
                stopService(intent);
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    /**开始行程*/
    private void startWorking(){
        if (!Utils.isGpsOPen(this)) {
            Utils.showDialog(this);
            return;
        }
        isWorking = true;
        layout_working.setVisibility(View.VISIBLE);
        btn_working.setText("停止");
        btn_working.setBackgroundResource(R.drawable.btn_bg_working_stop);

        if (isServiceLive)
            mLocClient.requestLocation();
        Intent intent = new Intent(this, WorkingService.class);
        startService(intent);


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
//            if (location.getLocType() == BDLocation.TypeGpsLocation) {
                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                locateAndZoom(location,ll);
//            }else{
//                Log.i("","当前为室内环境");
//            }
        }

    }

    /**地图缩放*/
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
    /**传感器发生变化*/
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

    /**精确度改变*/
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("MainActivity-LIVE","onStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i("MainActivity-LIVE","onRestart");
    }

    @Override
    protected void onResume() {
        mapView.onResume();
        super.onResume();
        Log.i("MainActivity-LIVE","onResume");
        // 为系统的方向传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("MainActivity-LIVE","onPause");
    }

    @Override
    protected void onStop() {
        // 取消注册传感器监听
        mSensorManager.unregisterListener(this);
        Log.i("MainActivity-LIVE","onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.i("MainActivity-LIVE","onDestroy");
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
    /**定位广播接收器，当有位置变化时，更新界面数据*/
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
                Log.d("MainActivity", "TopActivity---------false");
            }
        }
    }
    /**重启定位服务*/
    private void beginService() {

    }
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (layout_working.getVisibility() == View.VISIBLE) {
                if (!Utils.isServiceWork(this, "com.biubike.service.RouteService"))
//                    cancelBook();
                return true;
            }

            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addCategory(Intent.CATEGORY_HOME);
                startActivity(intent);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
