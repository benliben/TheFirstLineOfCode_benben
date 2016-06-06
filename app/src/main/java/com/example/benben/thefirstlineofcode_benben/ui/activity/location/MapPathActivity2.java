package com.example.benben.thefirstlineofcode_benben.ui.activity.location;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;


import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.navi.NaviParaOption;
import com.baidu.mapapi.search.core.RouteLine;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.route.BikingRouteLine;
import com.baidu.mapapi.search.route.BikingRoutePlanOption;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteLine;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteLine;
import com.baidu.mapapi.search.route.TransitRoutePlanOption;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteLine;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.example.benben.thefirstlineofcode_benben.R;
import com.example.benben.thefirstlineofcode_benben.http.LoadDialog;
import com.example.benben.thefirstlineofcode_benben.model.LocationModel;
import com.example.benben.thefirstlineofcode_benben.ui.activity.BaseActivity;
import com.example.benben.thefirstlineofcode_benben.ui.overlayutil.BikingRouteOverlay;
import com.example.benben.thefirstlineofcode_benben.ui.overlayutil.DrivingRouteOverlay;
import com.example.benben.thefirstlineofcode_benben.ui.overlayutil.TransitRouteOverlay;
import com.example.benben.thefirstlineofcode_benben.ui.overlayutil.WalkingRouteOverlay;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


/**
 * Created by benben on 2016/5/26.
 * 定位 路线
 */
@SuppressWarnings({"JavaDoc", "UnusedAssignment"})
public class MapPathActivity2 extends BaseActivity implements
        OnGetRoutePlanResultListener,BaiduMap.OnMapClickListener,OnGetGeoCoderResultListener {
    private static final String TAG = "MapPathActivity2";

    public static void startMapPathActivity2(Activity activity) {
        Intent intent = new Intent(activity, MapPathActivity2.class);
        ActivityCompat.startActivity(activity, intent, null);
    }


    private LocationModel locationInfo;
    private PlanNode enNode;//搜索的终点
    private PlanNode stNode;//搜索的起点

    /**
     * 浏览路线节点相关
     */
    @InjectView(R.id.pre)

    Button mBtnPre;//上一个节点
    @InjectView(R.id.next)

    Button mBtnNext;//下一个节点
    private int nodeIndex = -1;//节点索引，供浏览节点使用
    private RouteLine route = null;
    private BikingRouteOverlay routeOverlay = null;
    private boolean useDefaultIcon = false;//路线图标

    private LatLng ll;


    /**
     * 地图相关使用继承MapView的MyRouteMapView目的是重写touch事件实现泡泡处理
     * 如果不处理touch事件，则无需继承，直接使用MapView即可
     */
    @InjectView(R.id.path_map)
    MapView mMapView;
     BaiduMap mBaiduMap = null;

    /**
     * 搜索相关
     */
    private RoutePlanSearch mSearch = null;//搜索模块，可以去掉地图模块单独使用

    /**
     * 定位相关
     */
    private LocationClient mLocationClient = null;// LocationClient类是定位sdk的核心类
    private final BDLocationListener myListener = new MyLocationListenner();// 注册定位监听，返回定位的结果

    /**是否首次定位*/
    private boolean isFirstLoc = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_path);
        ButterKnife.inject(this);
        LoadDialog.Show(MapPathActivity2.this, "正在定位中...");
        mBaiduMap = mMapView.getMap();
        mBtnPre = (Button) findViewById(R.id.pre);
        mBtnNext = (Button) findViewById(R.id.next);
        mBtnPre.setVisibility(View.INVISIBLE);
        mBtnNext.setVisibility(View.INVISIBLE);
        /**地图的点击事件*/
        mBaiduMap.setOnMapClickListener(this);
        /**初始化搜索模块事件，注册监听*/
        mSearch = RoutePlanSearch.newInstance();
        mSearch.setOnGetRoutePlanResultListener(this);
        if (ll==null){
            LoadDialog.Show(MapPathActivity2.this, "正在定位中...");
        }else {
            LoadDialog.Show(MapPathActivity2.this, "正在定位中...");
            MapStatusUpdate u = MapStatusUpdateFactory
                    .newLatLng(ll);
            mBaiduMap.setMapStatus(u);
            bindData();
        }
        initMap();
         }

    private void bindData() {
        PlanNode stNode = PlanNode.withLocation(ll);
        DrivingRoutePlanOption option = new DrivingRoutePlanOption();
        option.from(stNode);
    }

    /**
     * 地图初始化
     */
    private void initMap() {
        mBaiduMap = mMapView.getMap();
        /**隐藏百度logo*/
        mMapView.removeViewAt(1);
        /**隐藏缩放控件ZoomControl*/
        View child = mMapView.getChildAt(1);
        if (child instanceof ImageView || child instanceof ZoomControls) {
            child.setVisibility(View.INVISIBLE);
        }
        /**隐藏左边规格*/
        mMapView.removeViewAt(2);
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        // 定位初始化
        mLocationClient = new LocationClient(this);
        //注册监听事件
        mLocationClient.registerLocationListener(myListener);
        initLocation();
    }
    private void initLocation() {
        Log.i(TAG, "____________________________2_____________________________________");
        LocationClientOption option = new LocationClientOption();
        // option.setLocationMode(LocationMode.Hight_Accuracy);//
        // 可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        // 坐标类型分为三种：国测局经纬度坐标系(gcj02)，百度墨卡托坐标系(bd09)，百度经纬度坐标系(bd09ll)。
        option.setCoorType("bd09ll");// 可选，默认gcj02，设置返回的定位结果坐标系
        int span = 1000;
        option.setScanSpan(span);// 可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);// 可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);// 可选，默认false,设置是否使用gps
        option.setNeedDeviceDirect(true);// 返回的结果包含手机的方向
        // option.setLocationNotify(true);//
        // 可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        // option.setIsNeedLocationDescribe(true);//
        // 可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//
        // 可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        // option.setIgnoreKillProcess(false);//
        // 可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        // option.SetIgnoreCacheException(false);//
        // 可选，默认false，设置是否收集CRASH信息，默认收集
        // option.setEnableSimulateGps(false);// 可选，默认false，设置是否需要过滤gps仿真结果，默认需要
        mLocationClient.setLocOption(option);
        LoadDialog.Hide();
        mLocationClient.start();// 开始定位
    }

    @Override
    public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {

    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {

    }


    /**定位SDK监听函数*/
    private class MyLocationListenner implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            /**构造定位数据*/
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(0)
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .build();
            mBaiduMap.setMyLocationData(locData);//设置定位数据
            if (isFirstLoc) {
                isFirstLoc = false;
                // 获取精度维度坐标
                ll = new LatLng(location.getLatitude(), location.getLongitude());//经纬度
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(19.0f);
                // animationMapStatus()方法把定位到的点移动到地图中心
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                StringBuilder sb = new StringBuilder(256);
                sb.append("time : ");
                sb.append(location.getTime());
                sb.append("\nerror code : ");
                sb.append(location.getLocType());
                sb.append("\nlatitude : ");
                sb.append(location.getLatitude());
                sb.append("\nlontitude : ");
                sb.append(location.getLongitude());
                sb.append("\nradius : ");
                sb.append(location.getRadius());
                if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
                    sb.append("\nspeed : ");
                    sb.append(location.getSpeed());// 速度 单位：公里每小时
                    sb.append("\nsatellite : ");
                    sb.append(location.getSatelliteNumber());//卫星数量
                    sb.append("\nheight : ");
                    sb.append(location.getAltitude());// 海拔 单位：米
                    sb.append("\ndirection : ");
                    sb.append(location.getDirection());// 单位度
                    sb.append("\naddr : ");
                    sb.append(location.getAddrStr());//地址
                    sb.append("\ndescribe : ");
                    sb.append("gps定位成功");
                    Log.i(TAG, "sb: " + sb);

                } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
                    sb.append("\naddr : ");
                    sb.append(location.getAddrStr());
                    // 运营商信息
                    sb.append("\noperationers : ");
                    sb.append(location.getOperators());
                    sb.append("\ndescribe : ");
                    sb.append("网络定位成功");
                } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
                    sb.append("\ndescribe : ");
                    sb.append("离线定位成功，离线定位结果也是有效的");
                } else if (location.getLocType() == BDLocation.TypeServerError) {
                    sb.append("\ndescribe : ");
                    sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
                } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                    sb.append("\ndescribe : ");
                    sb.append("网络不同导致定位失败，请检查网络是否通畅");
                } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                    sb.append("\ndescribe : ");
                    sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
                }
                sb.append("\nlocationdescribe : ");
                sb.append(location.getLocationDescribe());// 位置语义化信息
                List<Poi> list = location.getPoiList();// POI数据
                if (list != null) {
                    sb.append("\npoilist size = : ");
                    sb.append(list.size());
                    for (Poi p : list) {
                        sb.append("\npoi= : ");
                        sb.append(p.getId() + " " + p.getName() + " " + p.getRank());
                    }
                }
                Log.i(TAG, "onReceiveLocation: " + sb.toString());
                LoadDialog.Hide();
                new AlertDialog.Builder(MapPathActivity2.this).setTitle("定位").setMessage(sb.toString()).show();
            }
        }
    }


    /*************************************************路线**************************************************/

    /**
     * ；路线规划搜索
     * @param v
     */
    @SuppressWarnings("JavaDoc")
    public void searchButtonProcess(View v) {
        Log.i(TAG, "_______________________1______________________");
        /**重置浏览节点的路线数据*/
        route = null;
        mBtnPre.setVisibility(View.INVISIBLE);
        mBtnNext.setVisibility(View.INVISIBLE);
        mBaiduMap.clear();
        /**处理搜索按钮响应*/
        EditText editSt = (EditText) findViewById(R.id.path_start);
        EditText editEn = (EditText) findViewById(R.id.path_end);


        /**设置起终点信息，对于tranist search来说，城市名无意义*/

        assert editSt != null;
        stNode = PlanNode.withCityNameAndPlaceName("北京", editSt.getText().toString());
        assert editEn != null;
        enNode = PlanNode.withCityNameAndPlaceName("北京", editEn.getText().toString());



        Log.i(TAG, "ll"+ll);
        Log.i(TAG, "enNode"+enNode);
        /**实际使用中请对起点终点城市进行正确的设定*/

        String node=editSt.getText().toString();
        if (node.equals("我的位置")){
            if (v.getId() == R.id.path_drive) {
                mSearch.drivingSearch((new DrivingRoutePlanOption()).from(PlanNode.withLocation(ll))
                        .to(enNode));// 驾车搜索
            } else if (v.getId() == R.id.path_bath) {
                mSearch.transitSearch((new TransitRoutePlanOption()).from(PlanNode.withLocation(ll))
                        .to(enNode));// 公交搜索
            } else if (v.getId() == R.id.path_walk) {
                mSearch.walkingSearch((new WalkingRoutePlanOption()).from(PlanNode.withLocation(ll))
                        .to(enNode));// 步行搜索
            } else if (v.getId() == R.id.path_bike) {
                mSearch.bikingSearch((new BikingRoutePlanOption()).from(PlanNode.withLocation(ll)).to(
                        enNode));// 骑行搜索
                Log.i(TAG, "mSearch"+mSearch);
            }
            LoadDialog.Show(MapPathActivity2.this,"正在规划路线，请稍后...");
        }else {
            if (v.getId() == R.id.path_drive) {
                mSearch.drivingSearch((new DrivingRoutePlanOption()).from(stNode)
                        .to(enNode));// 驾车搜索
            } else if (v.getId() == R.id.path_bath) {
                mSearch.transitSearch((new TransitRoutePlanOption()).from(stNode)
                        .to(enNode));// 公交搜索
            } else if (v.getId() == R.id.path_walk) {
                mSearch.walkingSearch((new WalkingRoutePlanOption()).from(stNode)
                        .to(enNode));// 步行搜索
            } else if (v.getId() == R.id.path_bike) {
                mSearch.bikingSearch((new BikingRoutePlanOption()).from(stNode).
                        to(enNode));// 骑行搜索
            }
            LoadDialog.Show(MapPathActivity2.this,"正在规划路线，请稍后...");
        }
    }



    /**
     * 节点浏览
     *
     * @param v
     */
    @SuppressWarnings("JavaDoc")
    public void nodeClick(View v) {
        Log.i(TAG, "_______________________2______________________");
        if (route == null || route.getAllStep() == null) {
            return;
        }
        if (nodeIndex == -1 && v.getId() == R.id.pre) {
            return;
        }
        /**设置索引节点*/
        if (v.getId() == R.id.next) {
            if (nodeIndex < route.getAllStep().size() - 1) {
                nodeIndex++;
            } else {
                return;
            }
        } else if (v.getId() == R.id.pre) {
            if (nodeIndex > 0) {
                nodeIndex--;
            } else {
                return;
            }
        }
        /**获取节点结果信息*/
        LatLng nodeLocation = null;
        String nodeTitle = null;
        Object step = route.getAllStep().get(nodeIndex);
        if (step instanceof DrivingRouteLine.DrivingStep) {
            nodeLocation = ((DrivingRouteLine.DrivingStep) step).getEntrance()
                    .getLocation();
            nodeTitle = ((DrivingRouteLine.DrivingStep) step).getInstructions();
        } else if (step instanceof WalkingRouteLine.WalkingStep) {
            nodeLocation = ((WalkingRouteLine.WalkingStep) step).getEntrance()
                    .getLocation();
            nodeTitle = ((WalkingRouteLine.WalkingStep) step).getInstructions();
        } else if (step instanceof TransitRouteLine.TransitStep) {
            nodeLocation = ((TransitRouteLine.TransitStep) step).getEntrance()
                    .getLocation();
            nodeTitle = ((TransitRouteLine.TransitStep) step).getInstructions();
        } else if (step instanceof BikingRouteLine.BikingStep) {
            nodeLocation = ((BikingRouteLine.BikingStep) step).getEntrance()
                    .getLocation();
            nodeTitle = ((BikingRouteLine.BikingStep) step).getInstructions();
        }
        if (nodeLocation == null || nodeTitle == null) {
            return;
        }
        // 移动节点到中心
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(nodeLocation));
        // show popup
        TextView popupText = new TextView(MapPathActivity2.this);
        popupText.setBackgroundResource(R.mipmap.popup);//设置背景
        popupText.setTextColor(0xFF000000);
        popupText.setText(nodeTitle);
        mBaiduMap.showInfoWindow(new InfoWindow(popupText, nodeLocation, 0));
//        new AlertDialog.Builder(MapPathActivity2.this).setTitle(nodeTitle).setMessage(nodeTitle).show();
    }

    /**
     * 切换路线图标，刷新地图使其生效 注意： 起终点图标使用中心对齐.
     * @param v
     */
    public void changeRouteIcon(View v) {
        if (routeOverlay == null) {
            return;
        }
        if (useDefaultIcon) {
            ((Button) v).setText("自定义起终点图标");
            Toast.makeText(this, "将使用系统起终点图标", Toast.LENGTH_SHORT).show();

        } else {
            ((Button) v).setText("系统起终点图标");
            Toast.makeText(this, "将使用自定义起终点图标", Toast.LENGTH_SHORT).show();

        }
        useDefaultIcon = !useDefaultIcon;
        routeOverlay.removeFromMap();
        routeOverlay.addToMap();
    }

    /**自行车路线*/
    @SuppressWarnings({ "unchecked", "unchecked" })
    @Override
    public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {
        LoadDialog.Hide();
        if (bikingRouteResult == null
                || bikingRouteResult.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(MapPathActivity2.this, "抱歉，未找到结果",
                    Toast.LENGTH_SHORT).show();
        }
        assert bikingRouteResult != null;
        if (bikingRouteResult.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            // result.getSuggestAddrInfo()
            return;
        }
        if (bikingRouteResult.error == SearchResult.ERRORNO.NO_ERROR) {
            nodeIndex = -1;
            mBtnPre.setVisibility(View.VISIBLE);
            mBtnNext.setVisibility(View.VISIBLE);
            route = bikingRouteResult.getRouteLines().get(0);
            BikingRouteOverlay overlay1 = new MyBikingRouteOverlay(mBaiduMap);
            routeOverlay = overlay1;
            mBaiduMap.setOnMarkerClickListener(overlay1);
            overlay1.setData(bikingRouteResult.getRouteLines().get(0));
            overlay1.addToMap();
            overlay1.zoomToSpan();
            List<String> datas = new ArrayList<>();
            List<BikingRouteLine> routeLines = bikingRouteResult.getRouteLines();
            if (routeLines != null) {
                System.out.println(routeLines.size());
                for (int i = 0; i < route.getAllStep().size(); i++) {
                    datas.add(((BikingRouteLine.BikingStep) route.getAllStep().get(i)).getInstructions()+"\n");
                }
                new AlertDialog.Builder(MapPathActivity2.this)
                        .setTitle("骑行方案").setMessage(datas.toString()).show();
                System.out.println(datas.toString());
            }
            LoadDialog.Show(MapPathActivity2.this,"正在规划路线中，请稍后...");

        }
    }

    /**开车路线*/
    @Override
    public void onGetDrivingRouteResult(DrivingRouteResult result) {
        LoadDialog.Hide();
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(MapPathActivity2.this, "抱歉，未找到结果",
                    Toast.LENGTH_SHORT).show();
        }
        assert result != null;
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            // result.getSuggestAddrInfo()
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            nodeIndex = -1;
            mBtnPre.setVisibility(View.VISIBLE);
            mBtnNext.setVisibility(View.VISIBLE);
            route = result.getRouteLines().get(0);
            DrivingRouteOverlay overlay = new MyDrivingRouteOverlay(mBaiduMap);
//            routeOverlay = overlay;
            mBaiduMap.setOnMarkerClickListener(overlay);
            overlay.setData(result.getRouteLines().get(0));
            overlay.addToMap();
            overlay.zoomToSpan();
            List<String> datas = new ArrayList<>();
            List<DrivingRouteLine> routeLines = result.getRouteLines();
            if (routeLines != null) {
                System.out.println(routeLines.size());
                for (int i = 0; i < route.getAllStep().size(); i++) {
                    datas.add(( (DrivingRouteLine.DrivingStep) route.getAllStep().get(i)).getInstructions()+"\n");
                }
                new AlertDialog.Builder(MapPathActivity2.this)
                        .setTitle("骑行方案").setMessage(datas.toString()).show();
                System.out.println(datas.toString());
            }


        }
    }

    /**公交车路线*/
    @Override
    public void onGetTransitRouteResult(TransitRouteResult result) {
        LoadDialog.Hide();
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(MapPathActivity2.this, "抱歉未找到结果",
                    Toast.LENGTH_SHORT).show();
        }
        assert result != null;
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            // result.getSuggestAddrInfo()
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            nodeIndex = -1;
            mBtnPre.setVisibility(View.VISIBLE);
            mBtnNext.setVisibility(View.VISIBLE);
            route = result.getRouteLines().get(0);
            TransitRouteOverlay overlay = new MyTransitRouteOverlay(mBaiduMap);
            mBaiduMap.setOnMarkerClickListener(overlay);
//            routeOverlay = overlay;
            overlay.setData(result.getRouteLines().get(0));
            overlay.addToMap();
            overlay.zoomToSpan();
            //获取详细信息路线
            List<String> datas = new ArrayList<>();
            List<TransitRouteLine> routeLines = result.getRouteLines();
            if (routeLines != null) {
                System.out.println(routeLines.size());
                for (int i = 0; i < route.getAllStep().size(); i++) {
                    datas.add(( (TransitRouteLine.TransitStep) route.getAllStep().get(i)).getInstructions()+"\n");
                }
                new AlertDialog.Builder(MapPathActivity2.this)
                        .setTitle("方案").setMessage(datas.toString()).show();
                System.out.println(datas.toString());
            }
        }
    }

    /**步行路线*/
    @Override
    public void onGetWalkingRouteResult(WalkingRouteResult result) {
        LoadDialog.Hide();
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(MapPathActivity2.this, "抱歉未找到结果",
                    Toast.LENGTH_SHORT).show();
        }
        assert result != null;
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            // result.getSuggestAddrInfo()
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            nodeIndex = -1;
            mBtnPre.setVisibility(View.VISIBLE);
            mBtnNext.setVisibility(View.VISIBLE);
            route = result.getRouteLines().get(0);
            WalkingRouteOverlay overlay = new MyWalkingRouteOverlay(mBaiduMap);
            mBaiduMap.setOnMarkerClickListener(overlay);
//            routeOverlay = overlay;
            overlay.setData(result.getRouteLines().get(0));
            overlay.addToMap();
            overlay.zoomToSpan();
            List<String> datas = new ArrayList<>();
            List<WalkingRouteLine> routeLines = result.getRouteLines();
            //获取详细信息路线
            if (routeLines != null) {
                System.out.println(routeLines.size());
                for (int i = 0; i < route.getAllStep().size()-1; i++) {
                    datas.add(( (WalkingRouteLine.WalkingStep) route.getAllStep().get(i)).getInstructions()+"\n");
                }
                new AlertDialog.Builder(MapPathActivity2.this)
                        .setTitle("方案").setMessage(datas.toString()).show();
                System.out.println(datas.toString());
            }
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        mBaiduMap.hideInfoWindow();
    }

    @Override
    public boolean onMapPoiClick(MapPoi mapPoi) {
        return false;
    }

    // 定制RouteOverly
    private class MyDrivingRouteOverlay extends DrivingRouteOverlay {

        public MyDrivingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            if (useDefaultIcon) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.icon_track_navi_end);
            }
            return null;
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            if (useDefaultIcon) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.icon_track_navi_start);
            }
            return null;
        }
    }

    private class MyTransitRouteOverlay extends TransitRouteOverlay {

        public MyTransitRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            if (useDefaultIcon) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.icon_track_navi_end);
            }
            return null;
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            if (useDefaultIcon) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.icon_track_navi_start);
            }
            return null;
        }
    }

    private class MyBikingRouteOverlay extends BikingRouteOverlay {
        public MyBikingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            if (useDefaultIcon) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.icon_track_navi_end);
            }
            return null;
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            if (useDefaultIcon) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.icon_track_navi_start);
            }
            return null;
        }

    }

    private class MyWalkingRouteOverlay extends WalkingRouteOverlay {

        public MyWalkingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            if (useDefaultIcon) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.icon_track_navi_end);
            }
            return null;
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            if (useDefaultIcon) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.icon_track_navi_start);
            }
            return null;
        }
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        // 退出时销毁定位
        mLocationClient.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        mSearch.destroy();
        mMapView.onDestroy();
        mMapView = null;
        super.onDestroy();
    }


    /**
     * 开始导航
     */
    @SuppressWarnings("UnusedAssignment")
    public void startNavi() {
        //noinspection UnusedAssignment
        LatLng put = new LatLng(locationInfo.getLatitude(), locationInfo.getLongitude());

        final NaviParaOption para = new NaviParaOption();
        para.startPoint(ll);
        para.startName("从这里开始");
        EditText editEn = (EditText) findViewById(R.id.path_end);
        assert editEn != null;
        enNode = PlanNode.withCityNameAndPlaceName("北京", editEn.getText().toString());
//        para.endPoint(enNode);
        para.endName("到这里结束");
    }
}