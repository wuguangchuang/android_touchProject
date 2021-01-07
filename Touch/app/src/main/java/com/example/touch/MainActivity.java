package com.example.touch;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import adapter.MyFragmentAdapter;
import dataInformation.AppType;
import dataInformation.CalibrationData;
import dataInformation.CalibrationSettings;
import dataInformation.Touch_fireware_info;
import dataInformation.Touch_info;
import dataInformation.Touch_package;
import dataInformation.Touch_test_standard;
import fragment_interface.About_fragment_interface;
import fragment_interface.Setting_fragment_interface;
import fragment_interface.Signal_fragment_interface;
import fragment_interface.Test_fragment_interface;
import fragment_interface.Upgrade_fragment_interface;
import fragment_package.Setting_fragment;
import fragment_package.Signal_fragment;

import static fragment_package.Setting_fragment.calibrateFinish;
import static fragment_package.Setting_fragment.enterCalibrate;

public class MainActivity extends AppCompatActivity{

    public static String softwareVersion = "v1.2.3";
//    public static AppType appType = AppType.APP_FACTORY;
    public static AppType appType = AppType.APP_CLIENT;
//    public static AppType appType = AppType.APP_RD;
//    public static AppType appType = AppType.APP_PCBA;

    public static FragmentManager fragmentManager;
    public static MyCailbrateManager cailbrateManagerContext;
    public static View calibrateView;
    public static TabLayout mTabLayout;
    public static TabLayout.Tab updateTab;
    public static TabLayout.Tab testTab;
    public static TabLayout.Tab signalTab;
    public static TabLayout.Tab aboutTab;
    public static TabLayout.Tab settingTab;
    public static TabLayout.Tab curPage;

    private ViewPager viewpager;
    private MyFragmentAdapter adapter;

    public USBReceiver getUsbReceiver() {
        return usbReceiver;
    }

    private USBReceiver usbReceiver;

    private PendingIntent mPermissionIntent;
    private UsbManager usbManager;
    //用于访问TouchManager
    public upgrade_interface upgrade_interface;
    public About_interface about_interface;
    public Test_interface test_interface;
    public Setting_interface setting_interface;
    public Signal_interface_to_touchManager signal_interface_to_touchManager;
    //用于访问Fragment
    public Upgrade_fragment_interface upgrade_fragment_interface = null;
    public Test_fragment_interface test_fragment_interface = null;
    public About_fragment_interface about_fragment_interface = null;
    public Setting_fragment_interface setting_fragment_interface = null;
    public Signal_fragment_interface signal_fragment_interface = null;

    //
    private SwitchInterfaceThread switchInterfaceThread;

    private UsbDevice myTouchDevice = null;

    public static TouchManager touchManager;

    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final String TAG = "myText";
    private static final String TAGHidraw = "hidrawText";
    private UsbInterface usbInterface;
    private UsbInterface usbInterfaceRead;
    private UsbInterface usbInterfaceWrite;

    private String trueHidraw = null;

//    private HidrawX_info hidrawX_info;

    private volatile List<UsbDevice> usbDevicesList;
    public static volatile List<Touch_info> touch_info_list;
    private boolean exitSignalFlag = false;

    String existHidraw = null;

    private CheckDevideThread checkDevideThread;
    private List<String> hidrawFilePath;

    private Semaphore connect_sem = new Semaphore(1);

    public static String deviceInfo = "";
    public static String softwareInfo = "";

    public static String getDeviceInfo() {
        return deviceInfo;
    }

    public static String getSoftwareInfo() {
        return softwareInfo;
    }

    public TabLayout getmTabLayout() {
        return mTabLayout;
    }
    public String upgradeString  = "";
    public int upgradePro = 0;
    public int upgradeImageType = 0;
    public String upgradeImageText = "";

    public String testString = "";

    String appInfo ;//每个设备连接的信息

    //======================================
    public static volatile boolean noTouch = false;
    public static volatile Semaphore noTouch_sem;
    public static volatile int noTouch_sem_value = 0;
    //代表USB设备的一个接口
    private UsbInterface mInterface = null;
    private UsbDeviceConnection mDeviceConnection = null;
    //代表一个接口的某个节点的类:写数据节点
    private UsbEndpoint usbEpOut = null;
    //代表一个接口的某个节点的类:读数据节点
    private UsbEndpoint usbEpIn = null;

    int usbCoordStatus = -1;
    int uartCoordStatus = -1;
    //======================================
    public static void setCalibtareContext(MyCailbrateManager myCailbrateManager,View _view)
    {
        cailbrateManagerContext = myCailbrateManager;
        calibrateView = _view;
    }
    //自动升级标志位
    private boolean switchAutoUpgrade = false;
    static public boolean autoUpgrade = false;
    private AutoUpgradeThread autoUpgradeThread;
    //自动测试
    private boolean switchAutoTest = false;
    static public boolean autoTest = false;
    private AutoTestThread autoTestThread;

    private boolean exitProgram = false;
    int width;
    int  height;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
//        hideNavKey(this); //隐藏导航栏
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //隐藏状态栏


        WindowManager mWindowManager  = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        Display display = mWindowManager.getDefaultDisplay();
        display.getRealMetrics(metrics);
        Log.d(TAG, "像素 onCreate: width = " + metrics.widthPixels + "height = " + metrics.heightPixels);
        MyCalibrateView.width = metrics.widthPixels;
        MyCalibrateView.height = metrics.heightPixels;

        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate: 调用了onCreate");
        noTouch_sem = new Semaphore(0);
        initClass();
        initFragment();
        registerReceiver();
        usbManager = (UsbManager) MainActivity.this.getSystemService(Context.USB_SERVICE);
//        if(upgrade_fragment_interface != null)
//            upgrade_fragment_interface.setMainActivity(MainActivity.this);

        // 获取Fragment管理者
        fragmentManager = getSupportFragmentManager();


        //打开软件之后检测之前连接的设备
        checkDevideThread = new CheckDevideThread();
        checkDevideThread.start();
        if(savedInstanceState != null)
        {
            upgradeString = savedInstanceState.getString("upgradeText");
            testString = savedInstanceState.getString("testText");
            upgradeImageType = savedInstanceState.getInt("upgradeImageType");
            upgradeImageText = savedInstanceState.getString("upgradeImageText");
            upgradePro = savedInstanceState.getInt("upgradeProgress");
        }
        AutoWorking();

    }

    public static void hideNavKey(Context context) {
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
            View v = ((Activity) context).getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = ((Activity) context).getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }
    public static void addFragment(Fragment fragment, String tag) {
        // 开启事务
        FragmentTransaction beginTransaction = fragmentManager
                .beginTransaction();
        // 执行事务,添加Fragment
        beginTransaction.add(R.id.viewpager, fragment, tag);
        // 添加到回退栈,并定义标记
        beginTransaction.addToBackStack(tag);
        // 提交事务
        beginTransaction.commit();

    }
    private void initFragment(){
        //使用适配器将ViewPager与Fragment绑定在一起
        viewpager = findViewById(R.id.viewpager);
        adapter = new MyFragmentAdapter(getSupportFragmentManager());
        viewpager.setAdapter(adapter);
        //将TabLayout与ViewPager绑定在一起
        mTabLayout = findViewById(R.id.tabLayout);
        mTabLayout.setupWithViewPager(viewpager);

        updateTab = mTabLayout.getTabAt(0);
        testTab = mTabLayout.getTabAt(1);
        signalTab = mTabLayout.getTabAt(2);
        settingTab = mTabLayout.getTabAt(3);
        aboutTab = mTabLayout.getTabAt(4);

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                curPage = tab;
                if(curPage == updateTab)
                    Log.d(TAG, "onTabSelected: 当前为升级界面");
                else if(curPage == testTab)
                {
                    Log.d(TAG, "onTabSelected: 当前为测试界面");
                }

                else if(curPage == signalTab)
                    Log.d(TAG, "onTabSelected: 当前为信号图界面");
                else if(curPage == settingTab)
                    Log.d(TAG, "onTabSelected: 当前为设置界面");
                else if(curPage == aboutTab)
                    Log.d(TAG, "onTabSelected: 当前为关于界面");
                switchInterfaceThread = new SwitchInterfaceThread();
                switchInterfaceThread.start();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }
    class SwitchInterfaceThread extends Thread{

        private boolean running  = true;
        @Override
        public void run() {
            if(exitSignalFlag)
            {
                exitSignalFlag = false;
                if(signal_interface_to_touchManager != null)
                {
                    signal_interface_to_touchManager.refreshSigal(false,null);
                    while (running)
                    {
                        if(TouchManager.signalFinshed)
                        {
                            if(Signal_fragment.needRestoreStatus)
                                signal_fragment_interface.restoreCoords();
                            running = false;
                            break;
                        }
                        else
                        {
                            try {
                                sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                    }

                }
            }
            if(TouchManager.upgradeRunning)
            {
//                Toast.makeText(MainActivity.this,getString(R.string.upgrade_running),Toast.LENGTH_SHORT).show();
                while (running)
                {
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if(TouchManager.testRunning)
            {
//                Toast.makeText(MainActivity.this,getString(R.string.test_running),Toast.LENGTH_SHORT).show();
                while (running)
                {
                        try {
                            sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                }
            }
            if(curPage == signalTab)
            {
                Log.e(TAG, "onTabSelected: 执行了信号图界面的" );
                exitSignalFlag = true;
                if(signal_fragment_interface != null)
                {
                    signal_fragment_interface.refreshItems(false);
                    signal_fragment_interface.startSignalChart(false);
                }
            }
            else if(curPage == settingTab)
            {
                Log.e(TAG, "onTabSelected: 执行了设置界面的" );
                if(setting_fragment_interface != null)
                {
                    if(firstDevice() != null && firstDevice().bootloader == 0)
                    {
                        setting_fragment_interface.setEnable(true);
                        Log.e(TAG, "onTabSelected: 开始刷新设置界面数据" );
                        setting_interface.startRefreshSetting();
                    }
                    else
                    {
                        setting_fragment_interface.setEnable(false);
                    }
                }
            }
            else if(curPage == aboutTab)
            {
                Log.d(TAG, "当前为关于界面");
                if(about_fragment_interface != null)
                {
                    refreshAboutData();
                }

            }
        }
    }
    public void refreshAboutData(){
        if(about_fragment_interface != null && touch_info_list != null && touch_info_list.size() > 0)
        {
            deviceInfo = touchManager.about_text_str(touch_info_list.get(0));
            softwareInfo = touchManager.about_data_str(touch_info_list.get(0));
            about_fragment_interface.setTextViewText(deviceInfo);
            about_fragment_interface.setTextViewData(softwareInfo);
        }
    }
    private void initClass(){
        usbReceiver = new USBReceiver(MainActivity.this);
        usbDevicesList = new ArrayList<>();
        hidrawFilePath = new ArrayList<>();
        touch_info_list = new ArrayList<Touch_info>();

        if(touch_info_list.size() > 0)
        {
            touchManager = new TouchManager(this,touch_info_list.get(0));
//            String deviceInfo = touchManager.about_text_str(touch_info_list.get(0));
//            about_fragment_interface.setTextViewText(deviceInfo);
        }
        else
        {
            touchManager = new TouchManager(this,null);
//            String deviceInfo = touchManager.about_text_str(null);
//            about_fragment_interface.setTextViewText(deviceInfo);
        }
        upgrade_interface = touchManager;
        test_interface = touchManager;
        about_interface = touchManager;
        setting_interface = touchManager;
        signal_interface_to_touchManager = touchManager;



//        hidrawX_info = new HidrawX_info(this);
//        hidrawX_info.start();
    }


    //升级部分
    public void setUpgrade_fragment_interface(Upgrade_fragment_interface upgrade_fragment_interface) {
        this.upgrade_fragment_interface = upgrade_fragment_interface;
    }
    public void setUpgradestatus(String btnText,boolean enable) {
        upgrade_fragment_interface.setUpgradeBtnStatus(btnText,enable);
    }
    public void setUpgradeInProgress(int progress) {
        upgrade_fragment_interface.setUpgradeInProgress(progress);
    }
    public void setUpgradeTextStr(String message){
        upgrade_fragment_interface.setTextViewStr(appendMessageText(message));
    }
    //测试部分
    public void setTest_fragment_interface(Test_fragment_interface _test_fragment_interface){
        this.test_fragment_interface = _test_fragment_interface;
    }
    public void setTestInProgress(int progress) {
        test_fragment_interface.setTestInProgress(progress);
    }
    public void setTestBtnStatus(String btnText,boolean checked){
        test_fragment_interface.setTestBtn(btnText,checked);
    }
    public void setTestTextView(String text){
        test_fragment_interface.setTextViewStr(appendMessageText(text));
    }
    public void setImageInfo(int page,int type,String text){
        if(page == 0)
        {
            upgrade_fragment_interface.setUpgradeImageInfo(type,text);
        }
        else if(page == 1)
        {
            test_fragment_interface.setTestImageInfo(type,text);
        }
//        test_fragment_interface.setTestImageInfo(type,text);
    }
    public void setAbout_fragment_interface(About_fragment_interface _about_fragment_interface) {
        this.about_fragment_interface = _about_fragment_interface;
    }
    public void setSetting_fragment_interface(Setting_fragment_interface _setting_fragment_interface) {
        this.setting_fragment_interface = _setting_fragment_interface;
    }
    public void setSignal_fragment_interface(Signal_fragment_interface signal_fragment_interface) {
        this.signal_fragment_interface = signal_fragment_interface;
    }

    //USB 接收类
    public class USBReceiver extends BroadcastReceiver {

        public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
        MainActivity myMainActivity;
        private static final String TAG = "myText";

        public USBReceiver(MainActivity mainActivity) {
            myMainActivity = mainActivity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {


            String action = intent.getAction();
            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if(checkDevice(device) < 0)
            {
                Log.e(TAG, "onReceive: 插拔的不是触摸框设备");
                return;
            }
            if(exitProgram)
            {
                Log.d(TAG, "onReceive: 退出程序");
                return;
            }
            if (ACTION_USB_PERMISSION.equals(action)) {
                // 获取权限结果的广播
                    getPermissionResult(intent,device);

            }
            else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {

                try {
                    connect_sem.acquire(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.e(TAG, "onReceive: 有设备连接");
                // 有新的设备插入了，
                checkDevideThread = new CheckDevideThread();
                checkDevideThread.start();
            }
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

                try {
                    connect_sem.acquire(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 有设备拔出了
                Log.e(TAG, "onReceive: 有设备断开");
                List<UsbDevice> currentDevList = myMainActivity.getDeviceList();
                boolean exist = false;
                Touch_info touchInfo;
                for(int i = 0;i < touch_info_list.size() && i < usbDevicesList.size();i++)
                {
                    exist = false;
                    touchInfo = touch_info_list.get(i);
                    for(UsbDevice currentDev : currentDevList)
                    {
                        if(touchInfo.product_id == currentDev.getProductId())
                        {
                            exist = true;
                            break;
                        }
                    }
                    int ret = -1;
                    if(!exist)
                    {
//                        upgrade_fragment_interface.setTextViewStr(appendMessageText("断开设备："+ Integer.toString(touchInfo.product_id)));
                        if(touchInfo.bootloader == 0)
                        {
                            if(upgrade_fragment_interface != null)
                                upgrade_fragment_interface.setTextViewStr(appendMessageText("TouchApp " + touchInfo.model + "已断开"));
                            else if(test_fragment_interface != null)
                                test_fragment_interface.setTextViewStr(appendMessageText("TouchApp " + touchInfo.model + " 已断开"));

//                            Toast.makeText(MainActivity.this,touchInfo.model+" 已断开",Toast.LENGTH_SHORT).show();
                        }
                        else if(touchInfo.bootloader == 1){
                            if(upgrade_fragment_interface != null)
                                upgrade_fragment_interface.setTextViewStr(appendMessageText("Bootloader 已断开"));
                            else if(test_fragment_interface != null)
                                test_fragment_interface.setTextViewStr(appendMessageText("Bootloader 已断开"));

//                            Toast.makeText(MainActivity.this,"Bootloader 已断开",Toast.LENGTH_SHORT).show();
                        }
                        for(int f = 0; f < hidrawFilePath.size() && touch_info_list.get(i).filePath != null;f++)
                        {

                            if(hidrawFilePath.get(f) != null && touch_info_list.get(i).filePath.equals(hidrawFilePath.get(f)))
                            {
                                hidrawFilePath.remove(f);
                            }
                        }
                        for(int k = 0;k < usbDevicesList.size();k++)
                        {
                            if(usbDevicesList.get(k).getDeviceId() == touchInfo.dev.getDeviceId())
                            {
                                usbDevicesList.remove(i);
                            }

                        }
                        if(touch_info_list.get(i).mDeviceConnection != null)
                        {
                            if(touch_info_list.get(i).usbInterface != null)
                                touch_info_list.get(i).mDeviceConnection.releaseInterface(touch_info_list.get(i).usbInterface);
                            touch_info_list.get(i).mDeviceConnection.close();
                        }
                        touch_info_list.remove(i);
                    }
                }
//                Log.d(TAG, "onReceive: ==================拔出设备======================");
//                Log.d(TAG, "onReceive: usbDevicesList.size = " + usbDevicesList.size());
//                Log.d(TAG, "onReceive: usbDevicesList.size = "+touch_device_list.size());
//                for (int i = 0;i < usbDevicesList.size() && i < touch_info_list.size();i++)
//                {
//                    Log.d(TAG, "onReceive: usbDevicesList = "+usbDevicesList.get(i).getProductName());
//                    Log.d(TAG, "onReceive: usbDevicesList = "+touch_device_list.get(i).touch_info.dev.getProductName());
//                }
                if(touch_info_list.size() > 0)
                {
//                    for(int i = 0;i < touch_info_list.size();i++)
//                    {
//                        if(touch_info_list.get(i).bootloader == 0)
//                        {
                            TouchManager.initDeviceData(touch_info_list.get(0));
                            deviceInfo = touchManager.about_text_str(touch_info_list.get(0));
                            softwareInfo = touchManager.about_data_str(touch_info_list.get(0));
                            if(about_fragment_interface != null)
                            {
                                about_fragment_interface.setTextViewText(deviceInfo);
                                about_fragment_interface.setTextViewData(softwareInfo);
                            }

//                            break;
//                        }
//                    }
                }
                else
                {
                    TouchManager.initDeviceData(null);
                    deviceInfo = touchManager.about_text_str(null);
                    softwareInfo = touchManager.about_data_str(null);
                    if(about_fragment_interface != null)
                    {
                        about_fragment_interface.setTextViewText(deviceInfo);
                        about_fragment_interface.setTextViewData(softwareInfo);
                    }

                }
                if(curPage == settingTab) {
                    if (firstDevice() != null && firstDevice().bootloader == 0 &&
                            setting_fragment_interface != null&& ((!noTouch && firstDevice().filePath != null) ||
                            (noTouch && firstDevice().mDeviceConnection != null && firstDevice().usbEpOut != null && firstDevice().usbEpIn != null))) {
                        setting_fragment_interface.setEnable(true);
                        setting_fragment_interface.refreshSettings();
                    } else {
                        setting_fragment_interface.setEnable(false);
                    }
                }
                else if(curPage == signalTab)
                {
                    exitSignalFlag = true;
                    if(signal_fragment_interface != null)
                    {
                        signal_fragment_interface.startSignalChart(false);
                        signal_fragment_interface.clearSignaldata();
                    }
                }

                Log.e(TAG, "onReceive: 设备断开处理完成");
                connect_sem.release(1);
//                myMainActivity.firstDeviceConnect();
            } else {
//                Toast.makeText(myMainActivity, "ACTION_USB_PERMISSION != com.android.example.USB_PERMISSION", Toast.LENGTH_SHORT).show();
                Log.e("USBReceiver", "onReceive: 广播接收失败");
            }
        }
    }
    //获取USB设备列表
    public List<UsbDevice> getDeviceList() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        List<UsbDevice> usbDevices = new ArrayList<>();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            usbDevices.add(device);
//            Log.e(TAG, "getDeviceList: 设备名为 " + device.getDeviceName());
//            Log.d(TAG, "getDeviceList: device.getConfiguration(0).describeContents() = " + device.getConfiguration(0).describeContents());
//            Log.d(TAG, "getDeviceList: USB设备类别 = " + device.getDeviceClass());
//            Log.d(TAG, "getDeviceList: 设备ID = " + device.getDeviceId());
//            Log.d(TAG, "getDeviceList: 设备产品名称 = " + device.getProductName());
//            Log.d(TAG, "getDeviceList: 产品ID = " + device.getProductId());
//            Log.d(TAG, "getDeviceList: 生产商ID = " + device.getVendorId());
//            Log.d(TAG, "getDeviceList: 设备接口个数 = "+ device.getInterfaceCount());
//            Log.d(TAG, "getDeviceList: device.getSerialNumber() = " + device.getSerialNumber());

//            for(int i = 0;i  < device.getInterfaceCount();i++)
//            {
//                Log.d(TAG, "getDeviceList:接口的id号 = "+device.getInterface(i).getId());
//                Log.d(TAG, "getDeviceList: 接口的类别 = " + device.getInterface(i).getInterfaceClass());
//                Log.d(TAG, "getDeviceList: 接口的协议类别 = "+ device.getInterface(i).getInterfaceProtocol());
//                Log.d(TAG, "getDeviceList: 此接口的节点数量 = " + device.getInterface(i).getEndpointCount());
//            }
//            Toast.makeText(MainActivity.this,device.getDeviceName()+"已连接",Toast.LENGTH_SHORT);
//            Log.d(TAG, "getDeviceList: ===================================================");
        }
        return usbDevices;
    }
    //获取指定VID和PID的设备
    public UsbDevice getUsbDevice(int vendorId, int productId) {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            if (device.getVendorId() == vendorId && device.getProductId() == productId) {
                Toast.makeText(MainActivity.this,"已找到您的设备",Toast.LENGTH_SHORT).show();
                Log.e(TAG, "getDeviceList: " + device.getDeviceName());
                return device;
            }
        }
        Toast.makeText(MainActivity.this, "没有对应的设备", Toast.LENGTH_SHORT).show();
        return null;
    }

    //注册广播
    public void registerReceiver(){
        mPermissionIntent = PendingIntent.getBroadcast(MainActivity.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        Log.d(TAG, "registerReceiver: 已注册");
        MainActivity.this.registerReceiver(usbReceiver,filter);
    }
    public void cancalRegistReceiver(){
        MainActivity.this.unregisterReceiver(usbReceiver);
    }
    public static Touch_info firstDevice(){
        if(touch_info_list.size() <= 0)
        {
            return null;
        }
        return touch_info_list.get(0);
    }
    public void checkExistDevice(){
        List<UsbDevice> usbDevices = new ArrayList<>();
        usbDevices = getDeviceList();
        for(int i = 0;i < usbDevices.size();i++){
            boolean exist = false;
            for(UsbDevice device:usbDevicesList)
            {
                if(device.getDeviceId() == usbDevices.get(i).getDeviceId()){
                    exist = true;
                    break;
                }
            }
            int ret = -1;
            if(!exist && ((ret = checkDevice(usbDevices.get(i))) >= 0)){

                UsbDevice touchDevice = usbDevices.get(i);
                usbDevicesList.add(0,touchDevice);
                Log.d(TAG, "onReceive: 新增设备："+touchDevice.getProductName());

                Touch_info touch_info = new Touch_info();

                touch_info.dev = touchDevice;
                touch_info.product_id = touchDevice.getProductId();

                touch_info.connected = true;
                touch_info.bootloader = (byte)(ret & 0xff);
                touch_info.model = "";



                if(!noTouch)
                {
                    String checkStr = findHidraw(usbDevices.get(i).getVendorId(),usbDevices.get(i).getProductId());
                    touch_info.filePath = checkStr;
                    if(checkStr != null)
                    {
                        Log.d(TAG, "check_hid: 设备文件保存成功 ：" + touch_info.filePath);
                    }
                    else
                    {
                        Log.d(TAG, "checkExistDevice: 找不到hidraw设备文件");
                    }
                    
                }
                else
                {
                    touch_info.noTouch = true;
                }

//                check_device_sem.release(1);
                TouchManager.initDeviceData(touch_info);

                appInfo = touch_info.model;
                int strlen = 0;
                if(!noTouch && touch_info.filePath != null && touch_info.filePath.contains("/dev/hidraw"))
                {
                    if(ret == 0)
                    {
                        byte[] str = new byte[256];
                        strlen = touchManager.getStringInfo(0x02,str,str.length);
                        if(strlen > 0)
                        {
                            touch_info.model = new String(str);
                            appInfo = "TouchApp " + touch_info.model + " 已连接";
                            Toast.makeText(MainActivity.this,touch_info.model+" 已连接",Toast.LENGTH_SHORT).show();

                        }
                        else
                        {
                            appInfo = getString(R.string.no_read_string);
                        }
                    }
                    else
                    {
                        appInfo = "Bootloader 已连接";
                    }
                    touch_info_list.add(0,touch_info);
                    if(upgrade_fragment_interface != null)
                        upgrade_fragment_interface.setTextViewStr(appendMessageText(appInfo));
                    else if(test_fragment_interface != null)
                        test_fragment_interface.setTextViewStr(appendMessageText(appInfo));

                    if(strlen > 0 && touch_info.bootloader ==0)
                    {
                        Touch_fireware_info  touch_fireware_info= touchManager.getFirewareInfo();
                        int fireware_version =  touchManager.byteToChar(touch_fireware_info.version_l,touch_fireware_info.version_h);
                        int fireware_checkSum = touchManager.byteToChar(touch_fireware_info.checksum_l,touch_fireware_info.checksum_h);
                        appInfo = String.format("固件版本：0x%04X    固件校验码：0x%04X",fireware_version , fireware_checkSum);
                        if(upgrade_fragment_interface != null)
                            upgrade_fragment_interface.setTextViewStr(appendMessageText(appInfo));
                        else if(test_fragment_interface != null)
                            test_fragment_interface.setTextViewStr(appendMessageText(appInfo));
                    }
                    if(TouchManager.upgradeRunning && touch_info.bootloader == 1)
                    {
                        TouchManager.reset_sem.release(1);
                    }
                }
                else
                {
                    if(!noTouch && touch_info.filePath == null )
                    {
                        appInfo = getString(R.string.no_hidraw);
                    }
                    else if(!noTouch && (touch_info.filePath != null && (touch_info.filePath.equals("existHidraw") || touch_info.filePath.equals("noPermission"))))
                    {
                        appInfo = getString(R.string.no_permission);
                    }
                    touch_info_list.add(0,touch_info);

                    if(noTouch)
                    {
                        switchNoTouchMode(touch_info);
                    }
                    else
                    {
                        showNoTouchDialog(touch_info);
                    }

//                    if(upgrade_fragment_interface != null)
//                        upgrade_fragment_interface.setTextViewStr(appendMessageText(appInfo));
//                    else if(test_fragment_interface != null)
//                        test_fragment_interface.setTextViewStr(appendMessageText(appInfo));
                }
                return;
            }
        }
    }
    private void showNoTouchDialog(Touch_info touchInfo){
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        String chip = null;
        if(existHidraw == null)
        {
            chip = "没有可用的设备,";
        }
        else if(existHidraw.equals("noPermission") || existHidraw.equals("existHidraw"))
        {
            chip = "设备没有读写权限,";
        }
        normalDialog.setMessage(chip+"是否切换到无触摸模式?");
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "showNormalDialog: 点击了确定");
                        switchNoTouchMode(touchInfo);

                    }
                });
        normalDialog.setNegativeButton("取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "showNormalDialog: 点击了取消");
                        noTouch = false;
                        if(upgrade_fragment_interface != null)
                            upgrade_fragment_interface.setTextViewStr(appendMessageText(appInfo));
                        else if(test_fragment_interface != null)
                            test_fragment_interface.setTextViewStr(appendMessageText(appInfo));
                    }
                });
        // 显示
        normalDialog.show();
    }
    //切换到无触摸时的设备连接
    public void switchNoTouchMode(Touch_info touchInfo)
    {

        touchInfo.noTouch = true;
        noTouch = true;
        String appInfo = "";
        if(!hasPermission(touchInfo.dev)){
            Log.d(TAG, "onReceive: 新增设备还没有权限，正在申请权限");
            int permissionRet = requestPermission(touchInfo.dev);
            touchInfo.permission = false;
        }
        else
        {
            Log.e(TAG, "onClick: 已有权限，无需再获取");

            boolean openPortSuccess = openPort(touchInfo.dev);
            touchInfo.permission = true;
            byte[] str = new byte[256];
            TouchManager.initDeviceData(touchInfo);
//            touchInfo.bootloader = (byte)checkDeviceVPID();
            int strlen = 0;
            if(touchInfo.bootloader == 0)
            {
                strlen = touchManager.getStringInfo(0x02,str,str.length);
                if(strlen > 0)
                {
                    touchInfo.model = new String(str);
                    appInfo = "TouchApp " + touchInfo.model + " 已连接";
                    Toast.makeText(MainActivity.this,touchInfo.model+" 已连接",Toast.LENGTH_SHORT).show();
                }
                else
                {
                    appInfo = getString(R.string.no_read_string);
                }
            }
            else if(touchInfo.bootloader ==1)
            {
                appInfo = "Bootloader 已连接";
            }

            if(upgrade_fragment_interface != null)
                upgrade_fragment_interface.setTextViewStr(appendMessageText(appInfo));
            else if(test_fragment_interface != null)
                test_fragment_interface.setTextViewStr(appendMessageText(appInfo));

            if(strlen > 0 && touchInfo.bootloader ==0)
            {
                Touch_fireware_info  touch_fireware_info= touchManager.getFirewareInfo();
                int fireware_version =  touchManager.byteToChar(touch_fireware_info.version_l,touch_fireware_info.version_h);
                int fireware_checkSum = touchManager.byteToChar(touch_fireware_info.checksum_l,touch_fireware_info.checksum_h);
                appInfo = String.format("固件版本：0x%04X    固件校验码：0x%04X",fireware_version , fireware_checkSum);
                if(upgrade_fragment_interface != null)
                    upgrade_fragment_interface.setTextViewStr(appendMessageText(appInfo));
                else if(test_fragment_interface != null)
                    test_fragment_interface.setTextViewStr(appendMessageText(appInfo));

                usbCoordStatus = touchManager.getCoordsEnabled((byte)(getResInteger(R.integer.COORDS_CHANNEL_USB)&0xff));
                uartCoordStatus = touchManager.getCoordsEnabled((byte)(getResInteger(R.integer.COORDS_CHANNEL_SERIAL)&0xff));
                touchManager.enableCoords(false);
            }
            if(touchInfo != null && touchInfo.bootloader ==0 && touchInfo.mDeviceConnection != null && touchInfo.usbEpIn != null && touchInfo.usbEpOut != null)
            {
//                refreshReconnectDevicedata();
//                switchInterfaceThread = new SwitchInterfaceThread();
//                switchInterfaceThread.start();
            }


            if(noTouch && TouchManager.upgradeRunning)
            {
                noTouch_sem.release(1);
                Log.e(TAG, "sem:noTouch_sem + 1 = " + ++noTouch_sem_value);
            }
        }
    }


    public String appendMessageText(String message)
    {
//        Calendar c = Calendar.getInstance();
//        int ms = c.get(Calendar.MILLISECOND);
        return (getTime()+"  "+message);
    }
    public String getTime(){
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date).toString();
    }
    public interface upgrade_interface{
        void startUpgrade();
        void cancelUpgrade();
    }
    public interface Test_interface{
        void startTest();
        void cancelTest();
    }
    public interface Setting_interface{
        void startRefreshSetting();
        Map<String,Byte> getSettingsInfos();
        int getCalibrationSettings(CalibrationSettings calibrationSettings);
        int getCalibrationPointData(byte where, byte index, CalibrationData data);
        int setCalibrationPointData(byte index,CalibrationData data);
        int saveCalibrationData();
        int setCoordsEnabled(byte channel,byte enable);
        byte getCoordsMode(byte channel);
        byte setCoordsMode(byte channel,byte mode);
        int getMirror(byte[]current, byte[]def);
        int setMirror(byte[]values);
        int getRotation(byte[]current, byte[]def);
        int setRotation(byte[]buffer);
        int getMacOSMode(byte[]current, byte[]def);
        int setMacOSMode(byte mode);
    }
    public interface About_interface{
        Touch_package getStringInfo(int type);
        Touch_fireware_info getFirewareInfo();
    }
    public interface Signal_interface_to_touchManager{
        void refreshSigal(boolean force,List<Map<String,Integer>> checkSignalMapList);
        void setClickSignalItem(List<Map<String,Integer>> mapList);
        int setTesting(int on);
        int getSignalTestItems(byte[]items,int max,int mode);
        int getSignalTestStandard(byte index, Touch_test_standard standard, int mode);
        byte getCoordsEnabled(byte channel);
        int setCoordsEnabled(byte channel,byte enable);
        int signalInit(byte mode);
    }
    public static AppType getAppType(){
        return appType;
    }

    public class CheckDevideThread extends Thread{
        private boolean running = true;
        @Override
        public void run() {
            while (running ){
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(upgrade_fragment_interface != null || test_fragment_interface != null){
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            checkExistDevice();

                            connect_sem.release(1);
                            if(!noTouch)
                            {
                                refreshReconnectDevicedata();
                            }

                        }
                    });

                    break;
                }

            }

        }
    }
    private void refreshReconnectDevicedata(){
        if(TouchManager.upgradeRunning || TouchManager.testRunning)
        {
            return;
        }
        if(curPage == settingTab) {
            if(setting_fragment_interface != null)
            {
                if (firstDevice() != null && firstDevice().bootloader == 0 && firstDevice().filePath != null) {
                    setting_fragment_interface.setEnable(true);
                    setting_fragment_interface.refreshSettings();
                } else {
                    setting_fragment_interface.setEnable(false);
                }
            }

        }
        else if(curPage == signalTab)
        {
            exitSignalFlag = true;
            if(signal_fragment_interface != null)
            {
                signal_fragment_interface.refreshItems(false);
                signal_fragment_interface.startSignalChart(false);
            }
        }
        else if(curPage == aboutTab)
        {
            if(touch_info_list.size() > 0)
            {
                TouchManager.initDeviceData(touch_info_list.get(0));
                deviceInfo = touchManager.about_text_str(touch_info_list.get(0));
                softwareInfo = touchManager.about_data_str(touch_info_list.get(0));
                if(about_fragment_interface != null)
                {
                    about_fragment_interface.setTextViewText(deviceInfo);
                    about_fragment_interface.setTextViewData(softwareInfo);
                }
            }
            else
            {
                TouchManager.initDeviceData(null);
                deviceInfo = touchManager.about_text_str(null);
                softwareInfo = touchManager.about_data_str(null);
                if(about_fragment_interface != null)
                {
                    about_fragment_interface.setTextViewText(deviceInfo);
                    about_fragment_interface.setTextViewData(softwareInfo);
                }

            }
        }
    }
    //寻找USB对应的hidaw设备
    public String findHidraw(int vid,int pid){
        boolean exist = false;
        String path = "/dev";
        File dirFile = new File(path);
        File[] fs = dirFile.listFiles();//遍历目录下面的文件和目录
        if(fs == null) {
//            Log.d(TAG, "checkNewHidraw: 没有找到hidraw驱动设备");
            return null;
        }
        boolean existHidraw = false;
        for(File f: fs)
        {
            exist = false;
            if(f.getAbsolutePath().contains("/dev/hidraw"))
            {
                for(int t = 0;t < touch_info_list.size();t++)
                {
                    if(touch_info_list.get(t).equals(f.getAbsolutePath()))
                    {
                        exist = true;
                        break;
                    }
                }
                if(!exist)
                {
                    existHidraw = true;
                    Log.d(TAG, "findHidraw: 请确认" + f.getAbsolutePath() + " 设备是否正确");
                    File tmpFile = new File(f.getAbsolutePath());
                    if(!tmpFile.canRead() || !tmpFile.canWrite()){
                        Log.d(TAG, "run: 没有读写的权限，需要修改权限");
                        int ret = CommandThread.changeHidrawPermission(f.getAbsolutePath());
                        if(ret < 0)
                        {
                            continue;
                        }
                    }
                    int ret = HidrawManager.openHidraw(f.getAbsolutePath());
                    Log.d(TAG, "findHidraw: ret = " + ret);
                    if(ret >= 0)
                    {
                        return f.getAbsolutePath();
                    }


                }
                
            }
        }
        if(existHidraw)
        {
            return "existHidraw";
        }
        else
        {
            return null;
        }
    }
    //================================================================================
    //     * 判断对应 USB 设备是否有权限
    public boolean hasPermission(UsbDevice device) {
        return usbManager.hasPermission(device);
    }

    //     * 请求获取指定 USB 设备的权限
    public int requestPermission(UsbDevice device) {
        if (device != null) {
            if (usbManager.hasPermission(device)) {
                Log.d(TAG, "requestPermission: 已经获取到权限");
                return 0;
            } else {
                if (mPermissionIntent != null) {
                    usbManager.requestPermission(device, mPermissionIntent);
                    Log.d(TAG, "requestPermission: 请求USB权限");
                    return -1;
                } else {
                    Log.d(TAG, "requestPermission: 请注册USB广播");
                    return -2;
                }
            }
        }
        return -3;
    }
    public boolean openPort(UsbDevice device) {
        //获取设备接口，一般只有一个
//        usbInterface = device.getInterface(0);
        try {
            Log.d(TAG, "openPort: device.getInterfaceCount() = "+ device.getInterfaceCount());
        }catch (NullPointerException e)
        {
            e.printStackTrace();
            return false;
        }

        UsbInterface tmpUsbInterface = null;
        for(int i = 0; i < device.getInterfaceCount();i++){
            tmpUsbInterface = device.getInterface(i);
        }

        UsbDeviceConnection deviceConnection;
        // 判断是否有权限
        if (hasPermission(device)) {
            // 打开设备，获取 UsbDeviceConnection 对象，连接设备，用于后面的通讯

//            Log.d(TAG, "openPort: mDeviceConnection = "+deviceConnection.toString());

            deviceConnection = usbManager.openDevice(device);
            if (deviceConnection == null) {
                Log.d(TAG, "openPort: 打开设备失败");
                return false;
            }
            if (deviceConnection.claimInterface(tmpUsbInterface, true)) {
                Log.d(TAG, "openPort: 锁定interface接口成功");
            } else {

                Log.d(TAG, "openPort: " + deviceConnection.toString());
                if(tmpUsbInterface != null)
                    deviceConnection.releaseInterface(tmpUsbInterface);
                deviceConnection.close();
                deviceConnection = null;
                Log.d(TAG, "openPort: 锁定interface接口失败");

            }

        } else {
            Log.d(TAG, "openPort: 没有 USB 权限");
            return false;
        }

        // Try using the same interface for reading and writing
        int endPointCount = tmpUsbInterface.getEndpointCount();
        UsbEndpoint tmpUsbEpIn = null;
        UsbEndpoint tmpUsbEpOut = null;
        if (endPointCount == 1)//only getting 1 endpoint
        {
//            Log.e(TAG, "openPort: only getting 1 endpoint");
            tmpUsbEpIn = tmpUsbInterface.getEndpoint(0);
            //As an act of desperation try equating ep2 to this read EP, so that we can later attempt to write to it anyway
            tmpUsbEpOut = tmpUsbInterface.getEndpoint(0);
        } else if (endPointCount == 2) {
//            Log.e(TAG, "openPort: only getting 2 endpoint");
            tmpUsbEpIn = tmpUsbInterface.getEndpoint(0);
            tmpUsbEpOut = tmpUsbInterface.getEndpoint(1);
        }
        else  // ! UsingSingleInterface
        {
//            Log.d(TAG, "openPort: !UsingSingleInterface");
            usbInterfaceRead = device.getInterface(0x00);
            usbInterfaceWrite = device.getInterface(0x01);
            if ((usbInterfaceRead.getEndpointCount() == 1) && (usbInterfaceWrite.getEndpointCount() == 1)) {
                tmpUsbEpIn = usbInterfaceRead.getEndpoint(0);
                tmpUsbEpOut = usbInterfaceWrite.getEndpoint(0);
            }
        }
        for (int i = 0;i < touch_info_list.size();i++)
        {
            if(device.getProductId() == touch_info_list.get(i).product_id)
            {
//                Log.e(TAG, "openPort: 端口打开成功");
                touch_info_list.get(i).usbEpOut = tmpUsbEpOut;
                touch_info_list.get(i).usbEpIn = tmpUsbEpIn;
                touch_info_list.get(i).mDeviceConnection = deviceConnection;
                touch_info_list.get(i).usbInterface = tmpUsbInterface;
                break;
            }
        }
        return true;
    }

    //获取权限之后打开通讯端口
    public void getPermissionResult(Intent intent,UsbDevice device)
    {
        int strlen = 0;
        Touch_info touch_info = null;
        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            Log.d(TAG, "getPermissionResult: " + device.getProductName() + "获取权限成功");

            boolean openPortSuccess = openPort(device);

            if(device != null && touch_info_list != null && touch_info_list.size() > 0 && openPortSuccess)
            {
                for(int i = 0;i < touch_info_list.size();i++)
                {
                   if(device.getProductId() == touch_info_list.get(i).dev.getProductId())
                   {
                       touch_info_list.get(i).permission = true;
                       //other与原来的null与existHidraw区分：此处表示切换到无触摸模式成功
                       touch_info_list.get(i).filePath = "noTouch";
                       byte[] str = new byte[256];
                       TouchManager.initDeviceData(touch_info_list.get(i));
//                       touch_info_list.get(i).bootloader = (byte)checkDeviceVPID();
                       if(TouchManager.byteToInt(touch_info_list.get(i).bootloader) == 0) {
                           touch_info = touch_info_list.get(i);
                           strlen = touchManager.getStringInfo(0x02, str, str.length);
                           if (strlen > 0) {
                               touch_info_list.get(i).model = new String(str);
                               appInfo = "TouchApp " + touch_info_list.get(i).model + " 已连接";
                               Toast.makeText(MainActivity.this,touch_info_list.get(i).model+" 已连接",Toast.LENGTH_SHORT).show();
                               if(touch_info_list.get(i) != null && touch_info_list.get(i).bootloader ==0 && touch_info_list.get(i).mDeviceConnection != null && touch_info_list.get(i).usbEpIn != null && touch_info_list.get(i).usbEpOut != null)
                               {
//                                   refreshReconnectDevicedata();
                               }
                           } else {
                               appInfo = getString(R.string.no_read_string);
                           }
                       }
                       else if(TouchManager.byteToInt(touch_info_list.get(i).bootloader) == 1)
                       {
                           appInfo = "Bootloader 已连接";
                           Log.d(TAG, "getPermissionResult: Bootloader 已连接");
                       }
                       break;
                   }
                }

            }
            else
            {
                appInfo = "打开通讯端口失败";
            }
        } else {
//          Toast.makeText(myMainActivity, "获取权限失败" + device.getDeviceName(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "获取权限失败：" + device.getProductName());
            appInfo = "获取无触摸权限失败";
        }

        if(upgrade_fragment_interface != null)
            upgrade_fragment_interface.setTextViewStr(appendMessageText(appInfo));
        else if(test_fragment_interface != null)
            test_fragment_interface.setTextViewStr(appendMessageText(appInfo));

        if(strlen > 0 && touch_info != null && touch_info.bootloader ==0)
        {
            Touch_fireware_info  touch_fireware_info= touchManager.getFirewareInfo();
            int fireware_version =  touchManager.byteToChar(touch_fireware_info.version_l,touch_fireware_info.version_h);
            int fireware_checkSum = touchManager.byteToChar(touch_fireware_info.checksum_l,touch_fireware_info.checksum_h);
            appInfo = String.format("固件版本：0x%04X    固件校验码：0x%04X",fireware_version , fireware_checkSum);
            if(upgrade_fragment_interface != null)
                upgrade_fragment_interface.setTextViewStr(appendMessageText(appInfo));
            else if(test_fragment_interface != null)
                test_fragment_interface.setTextViewStr(appendMessageText(appInfo));
        }

        if(noTouch && TouchManager.upgradeRunning)
        {
            noTouch_sem.release(1);
            Log.e(TAG, "sem:noTouch_sem + 1 = " + ++noTouch_sem_value);
        }
    }



    //================================================================================

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
//        Log.e(TAG, "onConfigurationChanged: 被回调");
        super.onConfigurationChanged(newConfig);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ................");
        Setting_fragment.enterCalibrate = false;
        if(curPage == settingTab)
        {
            Log.d(TAG, "onResume: 进入设置界面");
            if(calibrateFinish)
            {
                if(setting_fragment_interface != null)
                {
                    Log.d(TAG, "onResume: 刷新校准数据");
                    setting_fragment_interface.refreshCalibrationData(1);
                }

                calibrateFinish = false;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: &&&&&&&&&&&&&&&&&&&");
    }

    @Override
    protected void onRestart() {
        exitProgram = false;

        if(noTouch && !enterCalibrate){
            checkDevideThread = new CheckDevideThread();
            checkDevideThread.start();
        }
        super.onRestart();
    }

    @Override
    protected void onStop() {

        if(noTouch && !enterCalibrate)
        {
            exitProgram = true;
            if(!touchManager.reconnectUSB(0,0))
            {
                touchManager.reset(getResInteger(R.integer.RESET_DST_BOOLOADER), 1);
                Toast.makeText(this,"稍等几秒后便可恢复触摸",Toast.LENGTH_SHORT).show();
            }
            clearDeviceData();

//            touchManager.setCoordsEnabled((byte)(getResInteger(R.integer.COORDS_CHANNEL_USB)&0xff), (byte) usbCoordStatus);
//            touchManager.setCoordsEnabled((byte)(getResInteger(R.integer.COORDS_CHANNEL_SERIAL)&0xff), (byte)uartCoordStatus);
        }
        Log.d(TAG, "onStop: *********************");

        super.onStop();
    }
    private void clearDeviceData(){
        for(int i = 0;i < touch_info_list.size();i++)
        {
            if(touch_info_list.get(i).bootloader == 0)
            {
                if(upgrade_fragment_interface != null)
                    upgrade_fragment_interface.setTextViewStr(appendMessageText("TouchApp " + touch_info_list.get(i).model + "已断开"));
                else if(test_fragment_interface != null)
                    test_fragment_interface.setTextViewStr(appendMessageText("TouchApp " + touch_info_list.get(i).model + " 已断开"));
            }
            else if(touch_info_list.get(i).bootloader == 1){
                if(upgrade_fragment_interface != null)
                    upgrade_fragment_interface.setTextViewStr(appendMessageText("Bootloader 已断开"));
                else if(test_fragment_interface != null)
                    test_fragment_interface.setTextViewStr(appendMessageText("Bootloader 已断开"));

            }
        }
        usbDevicesList.clear();
        touch_info_list.clear();
        hidrawFilePath.clear();
    }

    @Override
    protected void onDestroy() {

        Log.d(TAG, "onDestroy:已销毁...");

        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("upgradeText",upgrade_fragment_interface.getUpdateString());
        outState.putInt("upgradeProgress",upgrade_fragment_interface.getUpgradeProgress());
        outState.putInt("upgradeImageType",upgrade_fragment_interface.getImageType());
        outState.putString("upgradeImageText",upgrade_fragment_interface.getImageText());

        outState.putString("testText",test_fragment_interface.getTestText());
        super.onSaveInstanceState(outState);
    }

    public int  checkDevice(UsbDevice usbDevice){

        if(usbDevice.getVendorId() == 0xAED7 && usbDevice.getProductId() == 0x0013)
        {
            return 0;
        }
        if(usbDevice.getVendorId() == 0x14E1 && usbDevice.getProductId() == 0x3500)
        {
            return 0;
        }
        if(usbDevice.getVendorId() == 0x14E1 && usbDevice.getProductId() == 0x3400)
        {
            return 0;
        }
        if(usbDevice.getVendorId() == 0x14E1 && usbDevice.getProductId() == 0x2500)
        {
            return 0;
        }
        if(usbDevice.getVendorId() == 0x1FF7 && usbDevice.getProductId() == 0x0013)
        {
            return 0;
        }
        if(usbDevice.getVendorId() == 0xAED7 && usbDevice.getProductId() == 0xFEDC)
        {
            return 1;
        }
        if(usbDevice.getVendorId() == 0x24B8 && usbDevice.getProductId() == 0x0040)
        {
            return 0;
        }
        if(usbDevice.getVendorId() == 0x1101 && usbDevice.getProductId() == 0x0010)
        {
            return 0;
        }
        if(usbDevice.getVendorId() == 0x1FF7 && usbDevice.getProductId() == 0x001D)
        {
            return 0;
        }

        return -1;
    }
    public int checkDeviceVPID(){
        int vid = -1;
        int pid = -1;
        Touch_fireware_info fireware_info = touchManager.getFirewareInfo();
        if(fireware_info == null)
        {
            return -1;
        }
        else
        {
            vid = touchManager.byteToChar(fireware_info.usb_vid_l,fireware_info.usb_vid_h);
            pid = touchManager.byteToChar(fireware_info.usb_pid_l,fireware_info.usb_pid_h);
            Log.e(TAG, "checkDeviceVPID: " + String.format("VID = %04X,PID = %04X",vid,pid));
        }
        if(vid == 0xAED7 && pid == 0x0013)
        {
            return 0;
        }
        if(vid == 0x14E1 && pid == 0x3500)
        {
            return 0;
        }
        if(vid == 0x14E1 && pid == 0x3400)
        {
            return 0;
        }
        if(vid == 0x14E1 && pid == 0x2500)
        {
            return 0;
        }
        if(vid == 0x1FF7 && pid == 0x0013)
        {
            return 0;
        }
        if(vid == 0xAED7 && pid == 0xFEDC)
        {
            return 1;
        }
        if(vid == 0x24B8 && pid == 0x0040)
        {
            return 0;
        }
        if(vid == 0x1101 && pid == 0x0010)
        {
            return 0;
        }
        if(vid == 0x1FF7 && pid == 0x001D)
        {
            return 0;
        }
        return -1;
    }

    public int getResInteger(int type){
        return this.getResources().getInteger(type);
    }
    public char byteToChar(byte l,byte h){
        char ch = (char)((h << 8) | l);
        return ch;
    }
    public static int byteToInt(byte b)
    {
        int ret;
        if(b < 0)
        {
            ret = (256 - Math.abs(b));
        }
        else
        {
            ret = b;
        }
        return ret & 0xff;
    }
    private void AutoWorking()
    {
        if(switchAutoUpgrade)
        {
            autoUpgradeThread = new AutoUpgradeThread();
            autoUpgradeThread.start();
        }
        if(switchAutoTest)
        {
            autoTestThread = new AutoTestThread();
            autoTestThread.start();
        }
    }

    private class AutoUpgradeThread extends Thread{
        private boolean running = true;
        @Override
        public void run() {
            while (running)
            {
                if(autoUpgrade && firstDevice() != null && firstDevice().bootloader == 0)
                {
                    autoUpgrade = false;
                    upgrade_interface.startUpgrade();
                }
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private class AutoTestThread extends  Thread{
        boolean running = true;
        @Override
        public void run() {
            while(running)
            {
                if(autoTest && firstDevice() != null && firstDevice().bootloader == 0)
                {
                    autoTest = false;
                    test_interface.startTest();
                }
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }



}
