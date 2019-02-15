package com.codewind.taximeter.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.codewind.taximeter.R;
import com.codewind.taximeter.bean.RoutePoint;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.List;

@EActivity(R.layout.activity_route_detail)
public class RouteDetailActivity extends BaseActivity {
    @ViewById(R.id.img_routedetail_back)
    ImageView img_back;//返回键
    @ViewById(R.id.map_routedetail)
    MapView mapView;//地图
    @ViewById(R.id.text_routedetail_time)
    TextView text_time;//行驶时长
    @ViewById(R.id.text_routedetail_distance)
    TextView text_distance;//行驶距离
    @ViewById(R.id.text_routedetail__pricce)
    TextView text_price;//价格计算

    private BaiduMap baiduMap;//百度地图
    private String time,distance,price,routePointsStr;
    private ArrayList<RoutePoint> routePoints;
    private List<LatLng> points;

    private BitmapDescriptor start_icon,end_icon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @AfterViews
    void initView(){
        Intent intent = getIntent();
         time = intent.getStringExtra("totalTime");
         distance = intent.getStringExtra("totalDistance");
         price = intent.getStringExtra("totalPrice");
         routePointsStr = intent.getStringExtra("routePoints");
         routePoints = new Gson().fromJson(routePointsStr,new TypeToken<List<RoutePoint>>(){
         }.getType());

        start_icon = BitmapDescriptorFactory.fromResource(R.mipmap.route_start);
        end_icon = BitmapDescriptorFactory.fromResource(R.mipmap.route_end);

         drawRoute();

        text_time.setText("行驶时长：" + time );
        text_distance.setText("行驶距离：" + distance );
        text_price.setText("余额支付：" + price );
    }
    private void drawRoute(){
        points = new ArrayList<LatLng>();

        for (int i = 0; i < routePoints.size(); i++) {
            RoutePoint point = routePoints.get(i);
            LatLng latLng = new LatLng(point.getRouteLat(), point.getRouteLng());
            Log.d("gaolei", "point.getRouteLat()----show-----" + point.getRouteLat());
            Log.d("gaolei", "point.getRouteLng()----show-----" + point.getRouteLng());
            points.add(latLng);
        }
        if (points.size() > 2) {
            OverlayOptions ooPolyline = new PolylineOptions().width(10)
                    .color(0xFF36D19D).points(points);
            baiduMap.addOverlay(ooPolyline);
            RoutePoint startPoint = routePoints.get(0);
            LatLng startPosition = new LatLng(startPoint.getRouteLat(), startPoint.getRouteLng());

            MapStatus.Builder builder = new MapStatus.Builder();
            builder.target(startPosition).zoom(18.0f);
            baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            RoutePoint endPoint = routePoints.get(routePoints.size() - 1);
            LatLng endPosition = new LatLng(endPoint.getRouteLat(), endPoint.getRouteLng());
            addOverLayout(startPosition, endPosition);
        }
    }
    private void addOverLayout(LatLng startPosition, LatLng endPosition) {
        //先清除图层
        // mBaiduMap.clear();
        // 定义Maker坐标点
        // 构建MarkerOption，用于在地图上添加Marker
        MarkerOptions options = new MarkerOptions().position(startPosition)
                .icon(start_icon);
        // 在地图上添加Marker，并显示
        baiduMap.addOverlay(options);
        MarkerOptions options2 = new MarkerOptions().position(endPosition)
                .icon(end_icon);
        // 在地图上添加Marker，并显示
        baiduMap.addOverlay(options2);

    }
}
