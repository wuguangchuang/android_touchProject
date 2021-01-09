package com.example.touch;

import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import dataInformation.ArrayAndClassConversion;
import dataInformation.CalibrationCapture;
import dataInformation.CalibrationData;
import dataInformation.CalibrationSettings;
import dataInformation.FirewareFileHeader;
import dataInformation.FirewareHeader;
import dataInformation.FirewarePackage;
import dataInformation.TouchTestData;
import dataInformation.Touch_device;
import dataInformation.Touch_fireware_info;
import dataInformation.Touch_info;
import dataInformation.Touch_package;
import dataInformation.Touch_test_standard;
import dataInformation.UntData;

public class TouchManager implements MainActivity.upgrade_interface,MainActivity.Test_interface,
//        MainActivity.About_interface ,MainActivity.Setting_interface,MyCalibrateView.CalibrateMode{
        MainActivity.About_interface ,MainActivity.Setting_interface,MainActivity.Signal_interface_to_touchManager{

    public static MainActivity mainActivity;
    //找到的USB设备
    public static UsbDevice myTouchDevice;

//    private Touch_device touch_device;

    private CommandThread commandThread;
    public GetSignalThread getSignalThread;
    public static boolean signalIsRunning = false;
    public static boolean signalFinshed = false;
    public static boolean settingFinsh = false;


    private UpgradeThread upgradeThread;
    private TestThread testThread;
    private RefreshSettingThread refreshSettingThread;
    public CalibrationCaptureThread calibrationCaptureThread;
    static private Touch_info touchInfo = null;

    private static final String TAG = "myText";
    private static final String TAGTest = "testText";
    private static final String TAGTestData = "TAGTestData";
    static boolean bootloader = true;

    String[] signal_test_name ;
    static volatile boolean firstTime = true;
    // config
    static boolean mShowTestData = false;
    static boolean mIgnoreFailedTestItem = true;

    public static boolean testRunning = false;
    public static boolean upgradeRunning = false;
    public static String path = "";

    List<CalibrationData> oldListCalibrateData;

    public void setCalibrate_interface(Calibrate_Interface calibrate_interface) {
        this.calibrate_interface = calibrate_interface;
    }

    private Calibrate_Interface calibrate_interface;

    public TouchManager(MainActivity _mainActivity, Touch_info _touchInfo) {

        mainActivity = _mainActivity;
        if(_touchInfo != null)
        {
            myTouchDevice = _touchInfo.dev;
            touchInfo = _touchInfo;
        }
        else
        {
            myTouchDevice = null;
            touchInfo = null;
        }


        signal_test_name = mainActivity.getResources().getStringArray(R.array.signal_test_name);
        commandThread = new CommandThread();
        commandThread.start();
    }

    //升级部分
    @Override
    public void startUpgrade() {
        mainActivity.setTestInProgress(0);
        upgradeRunning = true;
        upgradeThread = new UpgradeThread();
        upgradeThread.start();
    }
    @Override
    public void cancelUpgrade() {
        upgradeThread.setCanceled(true);
    }

    public static Semaphore reset_sem = new Semaphore(0);
    public static volatile boolean upgradeReset = false;
    class UpgradeThread extends Thread{
        private boolean running = true;
        private boolean canceled = false;

        public boolean isCanceled() {
            return canceled;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
            if (canceled)
                this.running = false;
        }

        @Override
        public void run() {
            firstTime = true;
            Fireware fireware = new Fireware(path);
            FirewareHeader firewareHeader = new FirewareHeader();
            FirewareFileHeader fileHeader = new FirewareFileHeader();
            FirewarePackage firewarePackage;
            boolean result = true;
            Touch_device dev;
            String info = "";
            int pIndex = 0, fIndex = 0;
            int ret = 0;
            byte[] buf = new byte[64];
            int packageCount = 0;
            int firewareCount = 0;
            int allPackageIndex = 0;
            int allPackageCount = 0;
            int precent = 0, tmpPrecent;
            long waitBootloaderTime = 10 * 1000;
            boolean gotoEnd = false;
            boolean retBoolean = true;
            do {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(firstTime)
                {
                    firstTime = false;
                    synchronized (TouchManager.class)
                    {
                        if(myTouchDevice == null)
                        {
                            Log.d(TAGTest, "upgrade: " + getResString(R.string.no_touch_device) );
//                            mainActivity.setTestTextView(getResString(R.string.no_touch_device));
                            mainActivity.setImageInfo(0,6,getResString(R.string.no_touch_device));
                        }
                        else if(!touchInfo.noTouch && touchInfo.filePath == null)
                        {
                            Log.d(TAGTest, "upgrade: " + getResString(R.string.no_hidraw) );
//                            mainActivity.setTestTextView(getResString(R.string.no_touch_device));
                            mainActivity.setImageInfo(0,8,getResString(R.string.no_hidraw));
                        }
                        else if(!touchInfo.noTouch && touchInfo.filePath.equals("existHidraw"))
                        {
                            Log.d(TAGTest, "upgrade: " + getResString(R.string.no_permission) );
//                            mainActivity.setTestTextView(getResString(R.string.no_touch_device));
                            mainActivity.setImageInfo(0,8,getResString(R.string.no_permission));
                        }
                        else if(touchInfo.noTouch && (touchInfo.usbEpOut == null || touchInfo.mDeviceConnection == null ||
                                touchInfo.usbEpIn == null))
                        {
                            Log.d(TAGTest, "upgrade: " + getResString(R.string.no_touch_permission) );
                            mainActivity.setImageInfo(0,8,getResString(R.string.no_touch_permission));
                        }

                    }

                }
                if(isCanceled())
                {
                    Log.d(TAGTest, "run: " + getResString(R.string.cancel_upgrade));
                    mainActivity.setUpgradeTextStr(getResString(R.string.cancel_upgrade));
                    mainActivity.setImageInfo(0,7,getResString(R.string.cancel_upgrade));
                    mainActivity.setUpgradestatus(getResString(R.string.upgrade),true);
//                    mainActivity.setTestBtnStatus(String.valueOf(R.string.test) ,false);
                    upgradeRunning = false;
                    return;
                }
            }while (running && (myTouchDevice == null || (!touchInfo.noTouch && touchInfo.filePath == null) ||
                    (touchInfo.noTouch && (touchInfo.usbEpIn == null || touchInfo.usbEpOut == null || touchInfo.mDeviceConnection == null))));

            mainActivity.setUpgradestatus(getResString(R.string.upgrade_running),false);
            mainActivity.setImageInfo(0,5,getResString(R.string.upgrade_disconnect_device));
            if (!fireware.isReady()) {
                result = false;
                info = "固件出错";
                Log.d(TAG, "upgrade: 固件出错");
                gotoEnd = true;
            }
            if(!gotoEnd)
            {
                fileHeader = fireware.getFileHeader();
                firewareCount = fileHeader.firewareCount;
                for (fIndex = 0; fIndex < firewareCount; fIndex++) {
                    firewarePackage = fireware.getFirewarePackage(fIndex);
                    allPackageCount += firewarePackage.header.packCount;
                }
                allPackageCount += 10;

                precent = 1;
                mainActivity.setUpgradeInProgress(precent);
                mainActivity.setUpgradeTextStr(getResString(R.string.downloader_fireware_successful));

                Log.d(TAG, "upgrade: fireware is ok, start wait bootloader");
            }

            if(!gotoEnd && getTouchDevice() != null && getTouchDevice().bootloader == 0)
            {
                if (!(retBoolean = reset(getResInteger(R.integer.RESET_DST_BOOLOADER), 1))) {
                    Log.e(TAG, "upgrade: 复位出错");
                    gotoEnd = true;
                }
                Log.e(TAG, "run: 复位完成，等待BootLoader连接");
                try {
                    sleep(2000); // wait for reset
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                if(MainActivity.noTouch)
                {
                    try {
                        Log.e(TAG, "sem:acquire noTouch_sem 当前值 = " + MainActivity.noTouch_sem_value);
                        MainActivity.noTouch_sem.tryAcquire(1,5000,TimeUnit.MILLISECONDS);
                        Log.e(TAG, "sem:等待完成 noTouch_sem = " + MainActivity.noTouch_sem_value);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else
                {
                    try {
                        reset_sem.tryAcquire(1,5000, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if(getTouchDevice() != null && getTouchDevice().bootloader == 1 && ((!MainActivity.noTouch && getTouchDevice().filePath.contains("/dev/hidraw")) ||
                        (MainActivity.noTouch && (getTouchDevice().mDeviceConnection != null || getTouchDevice().usbEpIn != null || getTouchDevice().usbEpOut != null))))
                {

                    Log.d(TAG, "run: 继续升级");
                }
                else
                {
                    result = false;
                    info = "切换到BootLoader失败";
                    gotoEnd = true;
                }
            }
            if(!gotoEnd)
            {
                precent = 2;
                mainActivity.setUpgradeInProgress(precent);
                mainActivity.setUpgradeTextStr(getResString(R.string.restart_device_successful));

//                long oldTime = System.currentTimeMillis();
//                while (!isBootloaderDevice()) {
//                    if ((System.currentTimeMillis() - oldTime) > waitBootloaderTime)
//                    {
//                        break;
//                    }
//
//                    try {
//                        sleep(100);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
            }
            if(!gotoEnd)
            {
                precent = 3;
                mainActivity.setUpgradeInProgress(precent);
                mainActivity.setUpgradeTextStr(getResString(R.string.enter_downloader_mode));
                if (!isBootloaderDevice()) {
                    result = false;
                    info = "切换到升级模式下失败";
                    gotoEnd = true;
                }
            }
            for (fIndex = 0; fIndex < firewareCount && !gotoEnd; fIndex++) {
                firewarePackage = fireware.getFirewarePackage(fIndex);
//                Log.e(TAG, String.format("upgrade: findex = %d,firewarePackage.header.packSize = %d," +
//                        "firewarePackage.header.packCount = %d",fIndex,firewarePackage.header.packSize,firewarePackage.header.packCount));
                firewareHeader = firewarePackage.header;
                if (firewareHeader.packSize + 4 + 1 > 64) {
                    Log.d(TAG, "upgrade:package size is bigger than report length");
                    result = false;
                    info = "固件包大小比report包大";
                    gotoEnd = true;
                }
                if(!gotoEnd)
                {
                    Log.d(TAG, "upgrade: start IAP");
                    ret = startIAP(firewareHeader);
                    if (ret < 0) {
                        Log.e(TAG, String.format("upgrade:startIAP failed, return %d",ret));
                        result = false;
                        info = "IAP失败";
                        gotoEnd = true;
                    }
                }

                packageCount = firewarePackage.header.packCount;
//                mainActivity.setUpgradeTextStr("固件个数：" + packageCount);
                for (pIndex = 0; pIndex < packageCount && !gotoEnd; pIndex++) {
                    for(int i = 0; i < firewarePackage.header.packSize;i++)
                    {
                        buf[i] = firewarePackage.data[pIndex * firewarePackage.header.packSize + i];
                    }
//                    if(pIndex < 100)
//                    {
//                        mainActivity.setUpgradeTextStr(String.format("开始下载第%d个数据包",pIndex));
//                    }
                    ret = IAPDownload(pIndex, buf, firewarePackage.header.packSize);
//                    if(pIndex < 100)
//                    {
//                        mainActivity.setUpgradeTextStr(String.format("下载第%d个数据包完成",pIndex));
//                    }
//                    Log.d(TAG, "upgrade: IAP download " + pIndex);
                    if (ret != 0) {
                        Log.e(TAG, "upgrade: "+String.format("IAP Download %d packaged failed", pIndex));
                        result = false;
                        info = String.format("下载第%d个固件时失败", pIndex);
                        gotoEnd = true;
                    }
                    allPackageIndex++;
                    tmpPrecent = (int)(allPackageIndex * 97 / allPackageCount) + 3;
                    if (precent != tmpPrecent) {
                        precent = tmpPrecent;
                        mainActivity.setUpgradeInProgress(precent);
                    }
                }

                if(!gotoEnd)
                {
//                    mainActivity.setUpgradeTextStr("下载完成,开始校验固件");
                    ret = IAPVerify(firewarePackage.header.packCount * firewarePackage.header.packSize,
                            firewarePackage.header.verifyCodeSize, firewarePackage.header.verifyCode);
                    if (ret != 0) {
                        Log.e(TAG, "upgrade: "+String.format("IAP Verify %d packaged failed", pIndex) );
                        result = false;
                        info = String.format("校验第%d个固件时失败", pIndex);
                        gotoEnd = true;
                    }
                }

            }
            if(!gotoEnd)
            {
                Log.d(TAG, "upgrade:IAP set finished");
                ret = IAPSetFinished();
                if (ret != 0) {
                    Log.e(TAG, "upgrade: IAP Set Finished failed");
                    result = false;
                    info = "IAP FINISHED 失败";
                    gotoEnd = true;
                }
            }
            if(!gotoEnd)
            {
                Log.d(TAG, "upgrade:reset to app");
                reset(getResInteger(R.integer.RESET_DST_APP), 1);
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(MainActivity.noTouch)
                {
                    try {
                        Log.e(TAG, "sem:acquire noTouch_sem 当前值 = " + MainActivity.noTouch_sem_value);
                        MainActivity.noTouch_sem.tryAcquire(1,5000,TimeUnit.MILLISECONDS);
                        Log.e(TAG, "sem:等待完成 noTouch_sem = " + MainActivity.noTouch_sem_value);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }

            //upgrade done
            onUpgradeDone(result,info);
            mainActivity.setUpgradeInProgress(100);

            upgradeRunning = false;


            Log.d(TAG, "upgrade: 升级完成");
            //自动升级
            MainActivity.autoUpgrade = true;
        }
    }
    public int getStringInfo(int type, byte[] str, int max)
    {
        if (myTouchDevice == null ||str == null ) {
            return -1;
        }

        Touch_package require = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_DEVICE_INFO) & 0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_GET_STRING_INFO) & 0xff);
        require.data_length = 1;
        require.data[0] = (byte)type;

        Touch_info touch_info = touchInfo;
        Touch_package reply = new Touch_package();
        reply = commandThread.addCommandToQueue(touch_info,require);
        if(!isCommandReplySuccessful(require,reply,getFunctionName())){
            Log.d(TAG, "getStringInfo failed!");
            return -2;
        }
        int strLen = reply.data[1] > max ? max:reply.data[1];
        byteArrayCopy(str,0,reply.data,2,strLen);
        Log.e(TAG, "getStringInfo: "+new String(str));
        return strLen;
    }
    public boolean reset(int dst, int delay)
    {
        if (myTouchDevice == null) {
            return false;
        }
        byte[] data = new byte[2];
        data[0] = (byte)(dst & 0xff);
        data[1] = (byte)(delay & 0xff);
        Touch_package require = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_FIREWARE_UPGRADE) & 0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_RESET_DEVICE) & 0xff);
        require.data_length = 2;
        require.data = data;

        Touch_info touch_info = getTouchDevice();
        Touch_package reply = new Touch_package();
        reply = commandThread.addCommandToQueue(touch_info,require);
        if(!isCommandReplySuccessful(require,reply,getFunctionName())){
            Log.d(TAG, "upgrade:reset failed!");
            return false;
        }

        return true;
    }
    public boolean reconnectUSB(int disconnectDelay,int connectDelay)
    {
        if (myTouchDevice == null) {
            return false;
        }
        byte[] data = new byte[2];
        data[0] = (byte)(disconnectDelay & 0xff);
        data[1] = (byte)(connectDelay & 0xff);
        Touch_package require = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_HARDWARE) & 0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_RECONNECT_USB) & 0xff);
        require.data_length = 2;
        require.data = data;

        Touch_info touch_info = getTouchDevice();
        Touch_package reply = new Touch_package();
        reply = commandThread.addCommandToQueue(touch_info,require);
        if(!isCommandReplySuccessful(require,reply,getFunctionName())){
            Log.d(TAG, "reconnectUSB failed!");
            return false;
        }
        return true;
    }
    private boolean isBootloaderDevice()
    {
        for(int i = 0; i < MainActivity.touch_info_list.size();i++)
        {
            if(MainActivity.touch_info_list.get(i).bootloader == 1)
            {
                touchInfo = MainActivity.touch_info_list.get(i);
                myTouchDevice = touchInfo.dev;
                return true;
            }
        }
        return false;
//        if (touchInfo == null)
//            return false;
//        return touchInfo.bootloader == 1;
    }
    private int startIAP(FirewareHeader header)
    {
        byte[] data = new byte[44];
        initByteArray(data);
        if (myTouchDevice == null || header == null)
            return -1;
        shortToByte_LH(header.deviceIdRangeStart,data,0);
        shortToByte_LH(header.deviceIdRangeEnd,data,2);
        intToByte_LH(header.packSize,data,4);
        intToByte_LH(header.packCount,data,8);
        for(int i = 0;i < header.handShakeCode.length && (i + 12) < data.length;i++)
        {
            data[12 + i] = header.handShakeCode[i];
        }

        Touch_package require = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_FIREWARE_UPGRADE) & 0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_IAP_START) & 0xff);
        require.data_length = (byte)data.length;
        require.data = data;

        Touch_info touch_info = getTouchDevice();
        Touch_package reply = new Touch_package();
        reply = commandThread.addCommandToQueue(touch_info,require);
        if(!isCommandReplySuccessful(require,reply,getFunctionName())){
            Log.d(TAG, "upgrade:startIAP failed!");
            return -2;
        }
        return 0;
    }
    private int IAPDownload(int index, byte[]data, int count)
    {
        if (myTouchDevice == null || data == null || index < 0 || count < 0)
            return -1;
        byte[] buf = new byte[64];
        initByteArray(buf);
        intToByte_LH(index,buf,0);
        for(int i = 0;i < count;i++)
        {
            buf[4 + i] = data[i];
        }
        Touch_package require = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_FIREWARE_UPGRADE) & 0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_IAP_DOWNLOAD) & 0xff);
        require.data_length = (byte)(count + 4);
        require.data = buf;

        Touch_info touch_info = getTouchDevice();
        Touch_package reply = new Touch_package();
        reply = commandThread.addCommandToQueue(touch_info,require);
        if(!isCommandReplySuccessful(require,reply,getFunctionName())){
            Log.d(TAG, "upgrade:IAPDownload failed!");
            return -2;
        }
        return 0;
    }
    private int IAPVerify(int dataLength, byte verifyLength, byte[]verifyData)
    {
        if (myTouchDevice == null || verifyData == null || dataLength < 0 || verifyLength < 0)
            return -1;
        byte[] buf = new byte[64];
        initByteArray(buf);
        intToByte_LH(dataLength, buf,0);
        buf[4] = verifyLength;
        for(int i = 0;i < verifyLength;i++)
        {
            buf[5 + i] = verifyData[i];
        }
        Touch_package require = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_FIREWARE_UPGRADE) & 0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_IAP_VERIFY) & 0xff);
        require.data_length = (byte)(verifyLength + 5);
        require.data = buf;

        Touch_info touch_info = getTouchDevice();
        Touch_package reply = new Touch_package();
        reply = commandThread.addCommandToQueue(touch_info,require);
        if(!isCommandReplySuccessful(require,reply,getFunctionName())){
            Log.d(TAG, "upgrade:IAPVerify failed!");
            return -2;
        }
        return 0;
    }

    private int IAPSetFinished()
    {
        if (myTouchDevice == null)
            return -1;
        Touch_package require = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_FIREWARE_UPGRADE) & 0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_IAP_FINISHED) & 0xff);
        require.data_length = 0;
        require.data = null;

        Touch_info touch_info = getTouchDevice();
        Touch_package reply = new Touch_package();
        reply = commandThread.addCommandToQueue(touch_info,require);
        if(!isCommandReplySuccessful(require,reply,getFunctionName())){
            Log.d(TAG, "upgrade:IAPSetFinished failed!");
            return -2;
        }
        return 0;
    }
    private void onUpgradeDone(boolean result, String message)
    {
//        Log.d(TAG, "onUpgradeDone: " + String.format("upgrade result %s", message));
        if (result) {
            mainActivity.setImageInfo(0,1,getResString(R.string.upgrade_successful));
            mainActivity.setUpgradeTextStr(getResString(R.string.upgrade_successful));
        } else {
            mainActivity.setImageInfo(0,2,(getResString(R.string.upgrade_error) + "!\n" + message));
            mainActivity.setUpgradeTextStr(getResString(R.string.upgrade_error) +  "!" + message);
        }
        mainActivity.setUpgradestatus(getResString(R.string.upgrade),true);

    }

    //测试部分
    @Override
    public void startTest() {
        testThread = new TestThread();
        mainActivity.setTestInProgress(0);
        testThread.start();
    }
    @Override
    public void cancelTest() {
        testThread.setCanceled(true);
    }

    class TestThread extends Thread{
        private boolean running = true;

        public boolean isCanceled() {

            return canceled;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
            if(canceled)
                this.running = false;
        }

        private volatile boolean canceled = false;
        public boolean isRunning() {
            return running;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        @Override
        public void run() {
            //开始测试线程
            boolean finalResult = true;

            boolean gotoEnd = false;
            String info = "";
            int testCount = 0;
            byte[] items = new byte[128];
            int tryCount = 4;
            byte usb_status = -1;
            byte serial_status = -1;
            int testNo;
            boolean result = true;
            String errTmp = "";
            firstTime = true;
            testRunning = true;
            int maxTestItem = signal_test_name.length ;
            int mode = getResInteger(R.integer.STE_ALL_ITEMS);;
            Touch_info touchInfo;
            do {
                touchInfo = MainActivity.firstDevice();
                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(firstTime)
                {
                    firstTime = false;
                    synchronized (TouchManager.class)
                    {
                        if(touchInfo == null || touchInfo.dev == null)
                        {
                            Log.d(TAGTest, "run: " + getResString(R.string.no_touch_device) );
//                            mainActivity.setTestTextView(getResString(R.string.no_touch_device));
                            mainActivity.setImageInfo(1,6,getResString(R.string.no_touch_device));
                        }
                        else if(!touchInfo.noTouch && touchInfo.filePath == null)
                        {
                            Log.d(TAGTest, "run: " + getResString(R.string.no_hidraw) );
//                            mainActivity.setTestTextView(getResString(R.string.no_touch_device));
                            mainActivity.setImageInfo(1,8,getResString(R.string.no_hidraw));
                        }
                        else if(!touchInfo.noTouch && touchInfo.filePath.equals("existHidraw"))
                        {
                            Log.d(TAGTest, "run: " + getResString(R.string.no_permission) );
                            mainActivity.setImageInfo(1,8,getResString(R.string.no_permission));
                        }
                        else if(touchInfo.noTouch && (touchInfo.usbEpIn == null || touchInfo.usbEpOut == null || touchInfo.mDeviceConnection == null))
                        {
                            Log.d(TAGTest, "run: " + getResString(R.string.no_touch_permission) );
                            mainActivity.setImageInfo(1,8,getResString(R.string.no_touch_permission));
                        }
                    }

                }
                if(isCanceled())
                {
                    Log.d(TAGTest, "run: " + getResString(R.string.cancel_test));
                    mainActivity.setTestTextView(getResString(R.string.cancel_test));
                    mainActivity.setImageInfo(1,7,getResString(R.string.cancel_test));
                    mainActivity.setTestBtnStatus(getResString(R.string.test),true);
//                    mainActivity.setTestBtnStatus(String.valueOf(R.string.test) ,false);
                    testRunning = false;
                    return;
                }

            }while (running && (touchInfo == null || touchInfo.dev == null  || touchInfo.bootloader == 1 ||
                    (!touchInfo.noTouch && touchInfo.filePath == null) ||
                    (touchInfo.noTouch && (touchInfo.usbEpIn == null || touchInfo.usbEpOut == null || touchInfo.mDeviceConnection == null))));
            mainActivity.setTestTextView(getResString(R.string.start_test));
            mainActivity.setImageInfo(1,3,getResString(R.string.test_no_touch));
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.d(TAGTest, "run: " + e.toString());
            }

            if(!gotoEnd)
            {
                Touch_fireware_info fireware_info = getFirewareInfo();
                if(fireware_info == null)
                {
//                Log.d(TAG, "run: myTouchDevice.getProductName() = " + myTouchDevice.getProductName());
//                Log.d(TAG, "run: usbEpOut.toString() = " + usbEpOut.toString());
//                Log.d(TAG, "run: usbEpIn.toString() = " + usbEpIn.toString());
//                Log.d(TAG, "run: bootloader = " + Boolean.toString(bootloader));

                    mainActivity.setTestTextView(getResString(R.string.get_fireware_info_error));
                    Log.d(TAGTest, "run: 固件信息获取失败！！！");
                    finalResult = false;
                    gotoEnd = true;
                }
                else
                {
                    String firewareInfostr = String.format("固件版本: 0x%04X   ",byteToChar(fireware_info.version_l,fireware_info.version_h));
                    firewareInfostr += String.format("固件验证码: 0x%04X", byteToChar(fireware_info.checksum_l, fireware_info.checksum_h));
                    Log.d(TAGTest, "run：固件信息：" + firewareInfostr);
                    mainActivity.setTestTextView(firewareInfostr);
                }
            }


            if(!gotoEnd)
            {

                switch (mainActivity.getAppType()) {
                    case APP_CLIENT:
                        Log.d(TAGTest, "run: 客户版........................");
                        mode = getResInteger(R.integer.STE_END_USER_TEST);
                        break;
                    case APP_FACTORY:
                        Log.d(TAGTest, "run: 工厂版........................");
                        mode = getResInteger(R.integer.STE_FACTORY_TEST);
                        break;
                    case APP_RD:
                        Log.d(TAGTest, "run: 研发版........................");
                        mode = getResInteger(R.integer.STE_DEV_TEST);
                        break;
                    case APP_PCBA:
                        Log.d(TAGTest, "run: PCBA版........................");
                        mode = getResInteger(R.integer.STE_PCBA_CUSTOMER_TEST);
                        break;
                }
            }


//            Log.d(TAG, "run: 判断是否支持板载测试");
//            byte[] buffer = new byte[6];
//            initByteArray(buffer);
//            buffer[0] = (byte)(getResInteger(R.integer.ONBOARD_TEST_SWITCH_START) & 0xff);
//            buffer[1] = (byte)(getResInteger(R.integer.ONBOARD_TEST_MODE_CLOSE) & 0xff);;
//            intToLittleEndian(mode,buffer);
//            boolean isSupport = setOnboardTeststatus(buffer);
//            if(isSupport)
//            {
//                Log.d(TAG, "run: 支持板载测试");
////                支持板载测试的情况
////                if(!mtestStop)
////                    mTestListener->setNewWindowVisable();
//            }
//            else
//            {
//                Log.d(TAG, "run: 不支持板载测试");
//            }
            if(!gotoEnd)
            {
                if(setTestingMode(1) < 0){
                    mainActivity.setTestTextView("进入测试模式失败！！");
                    finalResult = false;
                    gotoEnd = true;
                }
            }

            if(!gotoEnd)
            {
                //信号初始化
                int ret = signalInit((byte)(getResInteger(R.integer.SIGNAL_INIT_COMPLETE) & 0xff));
                if(ret == 0)
                {
                    Log.d(TAGTest, "run: 信号初始化成功..");
                }
                else {
                    mainActivity.setTestTextView("信号初始化失败！！");
                    gotoEnd = true;
                    finalResult = false;
                }
            }
            if(!gotoEnd)
            {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //获取无触摸信号测试信号项

                Log.d(TAGTest, "run: 获取测试信号项......");
                tryCount = 4;
                while(tryCount > 1 && running)
                {
                    testCount = getSignalTestItems(items,items.length,mode);
                    if(testCount < 0)
                    {
                        tryCount--;
                    }
                    else
                    {
                        break;
                    }
                }
                if(testCount < 0)
                {
                    mainActivity.setTestTextView("获取信号项失败");
                    finalResult = false;
                    gotoEnd = true;
                }

                if(!gotoEnd)
                {
                    Log.d(TAGTest, "run: " + String.format("getSignalTestItems 测试项个数 = %d",testCount));
//                    Log.d(TAGTest, "run: 数据项序号 = " + Arrays.toString(items));
                }

            }

            if(!gotoEnd)
            {
                //获取USB坐标使能状态
                for (int i = 0;i < 3;i++)
                {
                    usb_status = getCoordsEnabled((byte)(getResInteger(R.integer.COORDS_CHANNEL_USB)&0xff));
                    if (byteToInt(usb_status) < 0) {
                        info = "USB获取坐标状态失败";
                        finalResult = false;
                        usb_status = -1;
                        gotoEnd = true;
                    }
                    else {
//                        gotoEnd = false;
                        break;
                    }
                }
                //获取Serial坐标使能状态
                for (int i = 0;i < 3;i++)
                {
                    serial_status = getCoordsEnabled((byte)(getResInteger(R.integer.COORDS_CHANNEL_SERIAL)&0xff));
                    if (byteToInt(serial_status) < 0) {
                        info = "serial获取坐标状态失败";
                        finalResult = false;
                        serial_status = -1;
//                        gotoEnd = true;
                    }
                    else {
//                        gotoEnd = false;
                        break;
                    }
                }
            }

//            Log.d(TAGTest, "run: get coords status, usb status = "+ usb_status + " serial status = "+ serial_status);
//            mainActivity.setTestTextView("run: get coords status, usb status = "+ usb_status + " serial status = "+ serial_status);
            if(!gotoEnd && running){
                if(enableCoords(false) == 0)
                {
                    Log.d(TAGTest, "run: 关闭坐标通道成功！！");
                }
                else
                {
                    Log.d(TAGTest, "run: 关闭坐标通道失败 ！！");
//                    gotoEnd = true;
                }
            }

            if(!gotoEnd)
            {
                for (int index = 0; index < testCount && running && !gotoEnd; index++) {
                    testNo = byteToInt(items[index]);
                    if (mIgnoreFailedTestItem) {
                        result = testSignal(testNo,mode);
                        if (finalResult && !result)
                            finalResult = result;
                        if (!result) {
                            if (testNo >= 0 && testNo < maxTestItem) {
                                errTmp = String.format("%s 测试失败",
                                        (signalIndexToString(testNo)));
                                Log.d(TAGTest, "run: " + errTmp);
                                mainActivity.setTestTextView(errTmp);
                            } else {
                                info = String.format("找不到0x%x测试项, 最大0x%x", testNo, maxTestItem);
                                Log.d(TAGTest, "run: " + info);
                                mainActivity.setTestTextView(info);
                            }
                        } else  {
//                            mainActivity.setTestTextView(String.format("%s 测试项成功",signalIndexToString(testNo) ));
                            Log.d(TAGTest, "run: " + String.format("%s 测试项成功",signalIndexToString(testNo)));
//                            mainActivity.setTestTextView(String.format("%s 测试项成功",signalIndexToString(testNo)));
                            errTmp = "";
                        }
                        mainActivity.setTestInProgress((index + 1) * 100 / testCount);

//                        if (mTestListener != NULL)
//                        {
//                            if(isSupport)
//                                mTestListener->inProgress((index + 1) * 90 / testCount, errTmp);
//                            else
//                                mTestListener->inProgress((index + 1) * 100 / testCount, errTmp);
//                        }

                    } else {
                        if (testSignal(testNo, mode)) {
//                            if (mTestListener != NULL)
//                            {
//                                if(isSupport)
//                                    mTestListener->inProgress((index + 1) * 90 / testCount, NULL);
//                                else
//                                    mTestListener->inProgress((index + 1) * 100 / testCount, NULL);
//                            }
                            mainActivity.setTestInProgress((index + 1) * 100 / testCount);


                        } else {
                            if (testNo >= 0 && testNo < maxTestItem) {
                                info = String.format("%s 测试失败",
                                        (signalIndexToString(testNo)));
                            } else {
                                info = String.format("找不到0x%x测试项, 最大0x%x", testNo, maxTestItem);
                            }
                            Log.d(TAGTest, "run: " + String.format("Test %d faild", testNo));
                            Log.d(TAGTest, "run: " + info);
                            mainActivity.setTestTextView(info);
                            finalResult = false;

                            gotoEnd = true;
                            break;
                        }
                    }

                }
            }

            setCoordsEnabled((byte)(getResInteger(R.integer.COORDS_CHANNEL_USB)&0xff), usb_status);
            setCoordsEnabled((byte)(getResInteger(R.integer.COORDS_CHANNEL_SERIAL)&0xff), serial_status);
//            Log.d(TAG, "run: 测试完成.....");
            if(canceled)
            {
                mainActivity.setImageInfo(1,7,getResString(R.string.cancel_test));
                mainActivity.setTestTextView(getResString(R.string.cancel_test));
            }
            else if(finalResult)
            {
                mainActivity.setImageInfo(1,1,getResString(R.string.test_successful));
                mainActivity.setTestTextView(getResString(R.string.test_successful));
            }
            else{
                mainActivity.setImageInfo(1,2,getResString(R.string.test_error));
                mainActivity.setTestTextView(getResString(R.string.test_error));

            }
//            mainActivity.setTestTextView(finalResult ? getResString(R.string.test_successful):getResString(R.string.test_error));

            mainActivity.setTestBtnStatus(getResString(R.string.test) ,true);
            setTestingMode(0);
            testRunning = false;
            MainActivity.autoTest = true;
        }

    }
    public static void initDeviceData(Touch_info _touchInfo){
        synchronized (TouchManager.class)
        {
            if(_touchInfo != null)
            {
                myTouchDevice = _touchInfo.dev;
                touchInfo = _touchInfo;
                if(_touchInfo.bootloader == 0)
                {
                    bootloader = false;
                }else
                {
                    bootloader = true;
                }
            }
            else
            {
                bootloader = true;
                myTouchDevice = null;
                touchInfo = null;
            }
            firstTime = true;

        }

    }
    @Override
    public Touch_package getStringInfo(int type) {

        if (myTouchDevice == null) {
            Toast.makeText(mainActivity,"没有连接的设备...",Toast.LENGTH_SHORT).show();
            return null;
        }
        Log.d(TAG, "getStringInfo: 获取字符串");
        Touch_package require = new Touch_package();
        require.master_cmd = 0x01;
        require.sub_cmd = 0x04;
        require.data_length = 0x01;
        require.data[0] = (byte) type;

        Touch_info touch_info = new Touch_info();
        Touch_package reply = new Touch_package();
        Log.d(TAG, "getStringInfo: 添加命令到线程中");
        reply = commandThread.addCommandToQueue(touch_info,require);
        if(reply == null)
        {
            Log.d(TAG, "getStringInfo: 字符串获取失败...");
            return null;
        }

        Log.d(TAG, "getStringInfo: 命令线程执行完毕");
        byte[] reply_array = new byte[256];
        packageClassToArray(reply,reply_array);
        String string = Arrays.copyOfRange(reply_array,2,reply_array.length).toString();
        Toast.makeText(mainActivity,string+"已连接",Toast.LENGTH_SHORT);
        Log.d(TAG, "getStringInfo: "+string +"设备已连接");
        return reply;
    }

    @Override
    public Touch_fireware_info getFirewareInfo() {
        if (myTouchDevice == null) {
            return null;
        }
        Log.d(TAG, "getFirewareInfo: 获取固件信息");
        Touch_package require = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_DEVICE_INFO) & 0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_GET_FIRMWARE_INFO) & 0xff);

//        Touch_info touch_info = new Touch_info();

        Touch_package reply = new Touch_package();
        reply = commandThread.addCommandToQueue(getTouchDevice(),require);
        if(!isCommandReplySuccessful(require,reply,getFunctionName()))
        {
            Log.d(TAG, "getFirewareInfo: 固件信息获取失败");
            return null;
        }
        Touch_fireware_info fireware_info = new Touch_fireware_info();
        byte[] reply_array = new byte[256];
        packageClassToArray(reply,reply_array);
        ArrayToTouchFirewareInfoClass(reply.data,fireware_info);
        return fireware_info;
    }
    public  int byteToChar(byte l,byte h){
        if(l == 0 && h == 0)
        {
            return 0;
        }
        int a = 0xff & l;
        int b = 0xff & h;
        return 0xffff & ((b << 8) | a);
//        char ch = (char)((char)(h << 8) | (char)l);
////        Log.d(TAG, String.format("byteToChar: char = %x" + ch));
//        return ch;
    }
    public Touch_package replyArrayToClass(byte[] reply_array){
        Touch_package reply = new Touch_package();
        reply.report_id = reply_array[0];
        reply.version = reply_array[1];
        reply.magic = reply_array[2];
        reply.flow = reply_array[3];
        reply.reserved1 = reply_array[4];
        reply.master_cmd = reply_array[5];
        reply.sub_cmd = reply_array[6];
        reply.reserved2 = reply_array[7];
        reply.data_length = reply_array[8];
        reply.data = Arrays.copyOfRange(reply_array,9,reply_array.length);
        return reply;
    }
    void packageClassToArray(Touch_package require,byte[] require_array){
        if(require == null || require_array == null)
        {
            return;
        }
        require_array[0] = require.report_id ;
        require_array[1] = require.version  ;
        require_array[2] = require.magic ;
        require_array[3] = require.flow ;
        require_array[4] = require.reserved1 ;
        require_array[5] = require.master_cmd ;
        require_array[6] = require.sub_cmd ;
        require_array[7] = require.reserved2 ;
        require_array[8] = require.data_length ;

        for(int i = 0;require.data != null && i < require.data.length && i < (require_array.length - 9);i++){
            require_array[9 + i] = require.data[i];
        }


    }
    public void ArrayToTouchFirewareInfoClass(byte[] data,Touch_fireware_info info){
        if(data == null)
        {
            return;
        }
        if(data.length < 13)
        {
            info = null;
            return;
        }

        info.type_l = data[0];
        info.type_h = data[1];
        info.version_l = data[2];
        info.version_h = data[3];
        info.command_protocol_version = data[4];
        info.serial_protocol_version = data[5];
        info.checksum_l = data[6];
        info.checksum_h = data[7];
        info.touch_point = data[8];
        info.usb_vid_l = data[9];
        info.usb_vid_h = data[10];
        info.usb_pid_l = data[11];
        info.usb_pid_h = data[12];
    }



    public String getResString(int type){
        return mainActivity.getString(type);
    }
    public static int getResInteger(int type){
        return mainActivity.getResources().getInteger(type);
    }
    public boolean setOnboardTeststatus(byte[] buffer){
        if (myTouchDevice == null) {
            return false;
        }
        Touch_package require = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_HARDWARE) & 0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_START_ONBOARD_TEST) & 0xff);
        require.data_length = 6;
        require.data = buffer;

        Touch_info touch_info = getTouchDevice();
        Touch_package reply = new Touch_package();
        Log.d(TAG, "setOnboardTeststatus: 添加命令到线程中");
        reply = commandThread.addCommandToQueue(touch_info,require);
        if(!isCommandReplySuccessful(require,reply,getFunctionName())){
            Log.d(TAG, "setOnboardTeststatus failed!");
            return false;
        }

        return true;
    }

    public int signalInit(byte mode){
        Touch_package require = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_HARDWARE) & 0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_SIGNAL_INITIAL) & 0xff);
        require.data_length = 1;
        require.data[0] = mode;

        Touch_package reply = new Touch_package();
        reply = commandThread.addCommandToQueue(getTouchDevice(),require);
        if(!isCommandReplySuccessful(require,reply,getFunctionName())){
            Log.d(TAG, "signalInit: 信号初始化失败");
            return -1;
        }
        return 0;
    }
    public int getSignalTestItems(byte[]items,int max,int mode){
        if(myTouchDevice == null )
        {
            return -1;
        }
        byte[] buffer = new byte[6] ;
        int remain ;
        int count = 0;
        int perGetCount = 50;
        int ret;
        initByteArray(buffer);
        buffer[0] = 0;
        buffer[1] = (byte)(perGetCount & 0xff) ;
        intToByte_LH(mode,buffer,2);
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();

        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_DEVICE_INFO) & 0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_GET_SIGNAL_TEST_ITEM) & 0xff);
        require.data_length = 6;
        require.data = buffer;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);
        if(!isCommandReplySuccessful(require,reply,getFunctionName())){
            return -2;
        }
//        items = Arrays.copyOfRange(reply.data,3,reply.data[2]);
        for(int i = 0;i < reply.data[2];i++)
        {
            items[i] = reply.data[3 + i];
        }
        remain = max > reply.data[0] ? reply.data[0] : max;
        remain -= reply.data[2];
        count += reply.data[2];

        for (int index = count; remain > 0 && testThread.running;) {
            initByteArray(buffer);
            reply = null;
            buffer[0] = (byte)(index & 0xff);
            buffer[1] = (byte)(perGetCount & 0xff) ;
            intToByte_LH(mode,buffer,2);
            require.data = buffer;
            reply = commandThread.addCommandToQueue(getTouchDevice(),require);
            if(!isCommandReplySuccessful(require,reply,getFunctionName())){
                return -2;
            }
            remain -= reply.data[2];
//            byte[] tmpdata = new byte[reply.data[2] + 1];
//            tmpdata = Arrays.copyOfRange(reply.data,3,reply.data[2]);
            for(int i = 0; i < reply.data[2];i++)
            {
                items[i + count] = reply.data[3 + i];
            }
            index += reply.data[2];
            count += reply.data[2];
        }
        Log.d(TAG, "getSignalTestItems: ###########获取测试项的个数为 = "+count);
        Log.d(TAG, "getSignalTestItems: items array = " +Arrays.toString(items) );
//        mainActivity.setTestTextView("items array = " +Arrays.toString(items));
        return count;
    }

    public static void sendPackage(Touch_package require,byte master_cmd,byte sub_cmd,
                                   byte length,byte[] data)
    {
        require.master_cmd = master_cmd;
        require.sub_cmd = sub_cmd;
        require.data_length = length;
        require.data = data;
    }



    public  boolean isCommandReplySuccessful(Touch_package require, Touch_package reply,String func){

        if (!checkCommandReply(require, reply)) {
            Log.d(TAG, "isCommandReplySuccessful: 命令执行失败...");
            Log.d(TAG, String.format(": 失败的命令：require.master_cmd = %d,require.sub_cmd = %d" ,require.master_cmd,require.sub_cmd ));
            byte[] buffer1 = new byte[64];
            packageClassToArray(require,buffer1);
            Log.d(TAG, func + "function error: 发送数据 = "+Arrays.toString(buffer1));

            byte[] buffer2 = new byte[64];
            packageClassToArray(reply,buffer2);
            Log.d(TAG, func + "function error: 接收数据 = "+Arrays.toString(buffer2));
            return false;
        }
//        Log.d(TAG, func + " function 命令执行成功...");
        return true;
    }

    public static boolean checkCommandReply(Touch_package require, Touch_package reply){
        if(reply == null || require == null)
        {
            return false;
        }
        if ((reply.master_cmd == (byte)(getResInteger(R.integer.TOUCH_M_CMD_RESPONSE) & 0xff) && reply.sub_cmd == (byte)(getResInteger(R.integer.TOUCH_S_CMD_SUCCEED)&0xff)) ||
                (require.master_cmd == reply.master_cmd &&
                        require.sub_cmd == reply.sub_cmd)) {
            return true;
        }
        return false;
    }
    public static Touch_info getTouchDevice(){
        Touch_info touch_info = new Touch_info();
        touch_info = mainActivity.firstDevice();
        return touch_info;
    }
    public byte getCoordsEnabled(byte channel)
    {
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_COORDS)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_GET_COORDS_ENABLED)&0xff);
        require.data_length = 1;
        require.data[0] = channel;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.d(TAG, "getCoordsEnabled: 坐标通道使能获取失败");
            return -2;
        }
        byte mode = reply.data[1];
//        Log.d(TAG, "getCoordsEnabled: 坐标通道使能获取成功 stadus = " + mode);
//        mainActivity.setTestTextView("getCoordsEnabled: 坐标通道使能获取成功 stadus = " + mode);
        return mode;
    }
    public int enableCoords(boolean enable)
    {
        int ret = -1;
        byte mode = enable ? (byte)(getResInteger(R.integer.COORDS_CHANNEL_ENABLE) & 0xff): (byte)(getResInteger(R.integer.COORDS_CHANNEL_DISABLE) & 0xff);
        ret = setCoordsEnabled((byte)(getResInteger(R.integer.COORDS_CHANNEL_USB) & 0xff), mode);
        ret = setCoordsEnabled((byte)(getResInteger(R.integer.COORDS_CHANNEL_SERIAL) & 0xff) , mode);
        return ret;
    }
    public int setCoordsEnabled(byte channel,byte enable)
    {
        String str1 = "";
        String str2 = "";
        if(channel == 0x01)
            str1 = "USB channel";
        else if(channel == 0x02)
            str1 = "Serial channel";
        else
            str1 = "channel choose error!";

        if(enable == 0x00)
            str2 = " disenable ";
        else if(enable == 0x01)
            str2 = " enable ";

        byte[] data = new byte[2];
        data[0] = channel;
        data[1] = enable;
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_COORDS)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_SET_COORDS_ENABLED)&0xff);
        require.data_length = 2;
        require.data = data;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.d(TAG, "setCoordsEnabled: 设置通道使能获取失败");
            return -2;
        }
        Log.d(TAG, "setCoordsEnabled: " + str1 + str2 + " successful");
        return 0;
    }
    public boolean testSignal(int testNo,int mode)
    {
        boolean gotoEnd = false;
        int dataCount = 0;
        int readDataCount = 0;
        Touch_test_standard standard = new Touch_test_standard();
        final int testDataMax = 256;
        byte[] testData = new byte[testDataMax];
        boolean result = true, allTestResult = true;
        int  standardMin = 0, standardMax = 0;
        int retVal = -1;
        int tledState = isTLedOn();
        if (tledState < 0)
            tledState = 1;

        int tryCount = 4;
        while (tryCount > 1)
        {
            retVal = getSignalTestStandard((byte)(testNo & 0xff), standard, mode);
            if(retVal < 0){
                gotoEnd = true;
                tryCount--;
            }
            else
            {
                gotoEnd = false;
                break;
            }
        }
        if(gotoEnd)
        {
            Log.d(TAGTest, "testSignal: 获取信号测试项标准失败,测试项 = " + signalIndexToString(testNo));
            return false;
        }
//        Log.d(TAGTest, "testSignal: testNo = " + testNo);



        int setTled = isTestTledOn(standard);
        int isRepeat = isTestRepeat(standard);
        int testMethod = getTestMethod(standard);
        int testSwitch = getTestSwitch(standard);

//        Log.d(TAGTest, "testSignal: " + String.format("test 0x%02x: method enum= %d, test switch = 0x%x, test method = %d",
//                testNo, standard.method_enum, testSwitch, testMethod));

        if(!gotoEnd)
        {
            if (retVal >= 0) {
                if (standard.max == 0) {
                    if(mode == getResInteger(R.integer.STE_END_USER_TEST))
                    {
                        standardMax = byteToInt(standard.client_max);
                        standardMin = byteToInt(standard.client_min);
                    }
                    else
                    {
                        standardMax = byteToInt(standard.factory_max);
                        standardMin = byteToInt(standard.factory_min);
                    }
                } else {
                    standardMin = byteToInt(standard.min);
                    standardMax = byteToInt(standard.max);
                }
                dataCount = toWord(standard.count_l,standard.count_h);
//                mainActivity.setTestTextView("testCount = " + dataCount);
//                Log.d("testStander", "测试标准: " + String.format("Test Number=0x%02d,name = %s, Count=%d, standard[%d - %d]", testNo,
//                        signalIndexToString(testNo),dataCount,
//                        standardMin, standardMax));

            }
            // old method
            if (standard.method_enum == 0 && testThread.running && !gotoEnd) {
                if (isSingleSignalTest(testNo)) {
                    setTLed(1);
                    result = true;

                    readDataCount = getSignalTestData((byte)(testNo & 0xff), testData, dataCount, readDataCount);
                    if (readDataCount < 0) {
                        Log.d(TAGTest, "2222testSignal: " + String.format("test no 0x%02x, get datas failed[%d]", testNo, readDataCount));
                        result = false;
                        gotoEnd = true;
                        allTestResult = false;
                    }
                    for (int i = 0; i < readDataCount && testThread.running && !gotoEnd; i++) {
                        if (byteToInt(testData[i]) < standardMin || byteToInt(testData[i]) > standardMax) {
                            result = false;
                            if (mIgnoreFailedTestItem == false) {
                                Log.d(TAGTest, "2222testSignal: " + String.format("test number 0x%02x %s, min: 0x%02x, max: 0x%02x, read %d val: 0x%02x",
                                        testNo, (result ? "pass" : "failed"),
                                        standardMin, standardMax, i, testData[i]));
                                gotoEnd = true;
                                break;
                            } else {
                                // 1 failed means all failed
                                if (result == false)
                                    allTestResult = false;
                            }
                        }
                        Log.d(TAGTest, "2222testSignal: "+String.format("test number 0x%02x %s, min: 0x%02x, max: 0x%02x, read %d val: 0x%02x",
                                testNo, (result ? "pass" : "failed"),
                                standardMin, standardMax, i, testData[i]));

                    }
                } else if (isUNT(testNo) && testThread.running && !gotoEnd) {
                    setTLed(1);

                    final int TEST_COUNT = 25;
                    byte[] bufData = new byte[TEST_COUNT * dataCount];

                    int index = 0;
                    // get all datas, TEST_COUNT * dataCount
                    for (int ti = 0; ti < TEST_COUNT && testThread.running && !gotoEnd; ti++) {
//                        untData = oneBuf + (dataCount * ti);
                        byte[] untData = new byte[256];
                        readDataCount = getSignalTestData((byte)(testNo & 0xff), untData, dataCount, readDataCount);
                        if (readDataCount < 0) {
                            Log.d(TAGTest, "2222testSignal: " + String.format("test number 0x%02x, get data command failed[%d]", testNo, retVal));
                            result = false;
                            gotoEnd = true;
                            break;
                        } else {
                            for(int i = 0;i  < readDataCount;i++)
                            {
                                bufData[index + i] = untData[i];
                            }
                            index += readDataCount;

                        }
                    }

                    if(!gotoEnd)
                    {
                        // delta
                        for (int ti = 0; ti < dataCount && testThread.running && !gotoEnd; ti++) {
                            int vMax = 0, vMin = 0xff;
                            byte tmpVal = 0;
                            // find dataCount max, min
                            for (int i = 0; i < TEST_COUNT; i++) {
                                tmpVal = bufData[i * dataCount + ti];
                                //TEST_DEBUG("test number 0x%02x, index %d: 0x%02x", testNo, i, tmpVal);
                                if (byteToInt(tmpVal) > vMax)
                                    vMax = byteToInt(tmpVal);
                                else if (byteToInt(tmpVal) < vMin)
                                    vMin = byteToInt(tmpVal);
                            }

                            int delta = vMax - vMin;
                            if (delta < standardMin || delta > standardMax) {
                                result = false;
                            }

//                        debugInfo += QString().sprintf("UNT test number 0x%02x %s, min: 0x%02x, max: 0x%02x, Delta value[%d]: 0x%02x\n",
//                                testNo, (result ? "pass" : "failed"),
//                                standardMin, standardMax,
//                                ti, delta);
                            if (!mShowTestData && result == false) {
                                Log.d(TAGTest, "2222testSignal: " + String.format("UNT test number 0x%02x %s, min: 0x%02x, max: 0x%02x, Delta val: 0x%02x",
                                        testNo, (result ? "pass" : "failed"),
                                        standardMin, standardMax, delta));
                            }
                            if (mIgnoreFailedTestItem == false) {
                                if (result == false) {
                                    gotoEnd = true;
                                    break;
                                }
                            } else {
                                // 1 failed means all failed
                                if (result == false)
                                    allTestResult = false;
                            }
                        }
                    }

                } else if (isRVR(testNo) &&  testThread.running && !gotoEnd) {
                    setTLed(0);

                    readDataCount = getSignalTestData((byte)(testNo & 0xff), testData, dataCount, readDataCount);
                    if (readDataCount < 0) {
                        Log.d(TAGTest, "testSignal: "+String.format("RVR test no 0x%02x, get datas failed[%d]", testNo, retVal));
                        result = false;
                        gotoEnd = true;
                    }
                    if(!gotoEnd)
                    {
                        for (int i = 0; i < readDataCount; i++) {
                            if (byteToInt(testData[i]) < standardMin || byteToInt(testData[i]) > standardMax) {
                                result = false;
                                if (mIgnoreFailedTestItem == false) {
                                    Log.d(TAGTest, "testSignal: " + String.format("RVR test number 0x%02x %s, min: 0x%02x, max: 0x%02x, read %d val: 0x%02x",
                                            testNo, (result ? "pass" : "failed"),
                                            standardMin, standardMax, i, testData[i]));
                                    gotoEnd = true;
                                    break;
                                } else {
                                    // 1 failed means all failed
                                    if (result == false)
                                        allTestResult = false;
                                }
                            }
                            Log.d(TAGTest, "testSignal: " + String.format("RVR test number 0x%02x %s, min: 0x%02x, max: 0x%02x, read %d val: 0x%02x",
                                    testNo, (result ? "pass" : "failed"),
                                    standardMin, standardMax, i, testData[i]));
                        }
                    }

                } else {
                    Log.d(TAGTest, "testSignal: "+String.format("Unknown test no 0x%02x", testNo));
                    result = false;

                }
            } else {
                if(!gotoEnd)
                {
                    // new method
                    setTLed(setTled);
                    switch (testMethod) {
                        case  0x01: {
//                        getResInteger(R.integer.STM_RAW_RANGE) == 0x01;
                            int testCount = (isRepeat == 1 ? 25 : 1);
                            for (int tc = 0; tc < testCount &&  testThread.running && !gotoEnd; tc++) {
//                                initByteArray(testData);
                                readDataCount = getSignalTestData((byte)(testNo & 0xff), testData, dataCount, readDataCount);
//                                Log.d("testStander", "测试项 = " + signalIndexToString(testNo));
//                                Log.d("testStander", "该项的测试数据个数 = " + readDataCount);
//                                Log.d("testStander", "测试数据 = " + Arrays.toString(testData));
                                if (readDataCount < 0) {
                                    Log.d(TAGTestData, "testSignal: 数据返回出问题");
                                    Log.d(TAGTestData, "测试项 = " + signalIndexToString(testNo));
                                    Log.d(TAGTestData, "该项的测试数据个数 = " + readDataCount);
                                    Log.d(TAGTestData, "测试数据 = " + Arrays.toString(testData));
//                                    Log.d(TAG, "获取testSignal: 数据项 = " +signalIndexToString(testNo));
//                                    Log.d(TAGTest, "5555testSignal: " + String.format("RAW RANGE test no 0x%02x, get datas failed[%d]", testNo, retVal));
                                    result = false;
                                    allTestResult = false;
                                    gotoEnd = true;
                                    break;
                                }
                                else {
//                                    Log.d(TAGTest, "5555getSignalTestData: " + String.format("信号数据获取成功 readDataCount = %d",readDataCount) );
//                                    Log.d(TAG, "testSignal: 数据 = " + Arrays.toString(testData));
                                }
                                for (int i = 0; i < readDataCount && testThread.running && !gotoEnd; i++) {
                                    if (byteToInt(testData[i]) < standardMin || byteToInt(testData[i]) > standardMax) {
                                        result = false;
                                        Log.d("testStander", "testSignal: 比较出问题");
                                        Log.d("testStander", "测试项 = " + signalIndexToString(testNo));
                                        Log.d("testStander", "该项的测试数据个数 = " + readDataCount);
                                        Log.d("testStander", "测试数据 = " + Arrays.toString(testData));
                                        Log.d("testStander", "testSignal: "+String.format("RAW RANGE test number 0x%02x %s, min: 0x%02x, max: 0x%02x, read %d val: 0x%02x",
                                                testNo, (result ? "pass" : "failed"),
                                                standardMin, standardMax, i, testData[i]));;
                                        if (mIgnoreFailedTestItem == false) {
                                            Log.d(TAG, "比较testSignal: 数据项 = " +signalIndexToString(testNo));
                                            Log.d(TAGTest, "5555testSignal: "+String.format("RAW RANGE test number 0x%02x %s, min: 0x%02x, max: 0x%02x, read %d val: 0x%02x",
                                                    testNo, (result ? "pass" : "failed"),
                                                    standardMin, standardMax, i, testData[i]));

                                            gotoEnd = true;
                                            break;
                                        } else {
                                            // 1 failed means all failed
                                            if (result == false)
                                            {
                                                allTestResult = false;
//                                                Log.d(TAGTest, "testSignal: "+String.format("RAW RANGE test number 0x%02x %s, min: 0x%02x, max: 0x%02x, read %d val: 0x%02x",
//                                                        testNo, (result ? "pass" : "failed"),
//                                                        standardMin, standardMax, i, testData[i]));;
                                                break;
                                            }

                                        }
                                    }

                                }
                                if(gotoEnd)
                                {
                                    break;
                                }
                            }
                        }
                        break;
                        case 0x02:{
//                            Log.d(TAGTest, "testSignal: 1111111111111111111111111111111");
//                        getResInteger(R.integer.STM_MAX_MIN_DIFF) == 0x02
                            final int TEST_COUNT = (isRepeat == 1 ? 25 : 1);
                            byte[] bufData = new byte[TEST_COUNT * dataCount];
                            int index = 0;


                            // get all datas, TEST_COUNT * dataCount
                            for (int ti = 0; ti < TEST_COUNT && testThread.running && !gotoEnd; ti++) {
                                byte[] untData = new byte[256];
                                readDataCount = getSignalTestData((byte)(testNo&0xff), untData, dataCount,readDataCount);
                                if (readDataCount < 0) {
                                    Log.d(TAGTest, "1111testSignal: " + String.format("test number 0x%02x, get data command failed[%d]", testNo, retVal));
                                    result = false;
                                    gotoEnd = true;
                                    break;
                                }
                                else
                                {
                                    for(int i = 0;i  < readDataCount;i++)
                                    {
                                        bufData[index + i] = untData[i];
                                    }
                                    index += readDataCount;

                                }
                            }

                            // delta
                            for (int ti = 0; ti < dataCount && testThread.running && !gotoEnd; ti++) {
                                int vMax = 0, vMin = 0xff;
                                byte tmpVal = 0;
                                // find dataCount max, min
                                for (int i = 0; i < TEST_COUNT; i++) {
                                    tmpVal = bufData[i * dataCount + ti];
                                    //TEST_DEBUG("test number 0x%02x, index %d: 0x%02x", testNo, i, tmpVal);
                                    if (byteToInt(tmpVal) > vMax)
                                        vMax = byteToInt(tmpVal);
                                    // else if (tmpVal < vMin). for repeat off
                                    if (byteToInt(tmpVal) < vMin)
                                        vMin = byteToInt(tmpVal);
                                }

                                int delta = vMax - vMin;
                                if (delta < standardMin || delta > standardMax) {
                                    result = false;
                                }

                                Log.d(TAGTest, "1111testSignal: "+String.format("UNT test number 0x%02x %s, min: 0x%02x, max: 0x%02x, Delta value[%d]: 0x%02x\n",
                                        testNo, (result ? "pass" : "failed"),
                                        standardMin, standardMax,
                                        ti, delta));
                                if (!mShowTestData && result == false) {
                                    Log.d(TAGTest, "1111testSignal: "+String.format("UNT test number 0x%02x %s, min: 0x%02x, max: 0x%02x, Delta val: 0x%02x",
                                            testNo, (result ? "pass" : "failed"),
                                            standardMin, standardMax, delta));
                                }
                                if (mIgnoreFailedTestItem == false) {
                                    if (result == false) {
                                        gotoEnd = true;
                                        break;
                                    }
                                } else {
                                    // 1 failed means all failed
                                    if (result == false)
                                        allTestResult = false;
                                }
                            }
                        }
                        break;
                        default: {
                            Log.d(TAGTest, "33333testSignal: " + String.format("Unknown test no 0x%02x", testNo));
                            result = false;
                            gotoEnd = true;
                        }
                    }
                }

            }

        } else {
            Log.d(TAGTest, "44444testSignal: "+String.format("test no 0x%02x, get standard failed", testNo));
            result = false;
            gotoEnd = true;

        }
        setTLed(tledState);

        if (mIgnoreFailedTestItem) {

            result = allTestResult;
            Log.d(TAGTestData, "testSignal: result = " + Boolean.toString(result));
            Log.d(TAGTestData, "testSignal: allTestResult = " + Boolean.toString(allTestResult));
        }
        return result;
    }
    public int isTLedOn()
    {
        Touch_package require = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_HARDWARE) & 0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_GET_TLED) & 0xff);
        require.data_length = 0;
        require.data = null;

        Touch_info touch_info = getTouchDevice();
        Touch_package reply = new Touch_package();
        reply = commandThread.addCommandToQueue(touch_info,require);
        if(!isCommandReplySuccessful(require,reply,getFunctionName())){
            return -2;
        }

        return reply.data[0] == (byte)(getResInteger(R.integer.TLED_ON) & 0xff) ? getResInteger(R.integer.TLED_ON) : getResInteger(R.integer.TLED_OFF);
    }
    public int getSignalTestStandard(byte index, Touch_test_standard standard, int mode)
    {
        if (myTouchDevice == null || standard == null) {
            return -1;
        }
        byte[] buf = new byte[5];
        initByteArray(buf);
        int ret;
        buf[0] = index;
        intToByte_LH(mode,buf,1);
        Touch_package require = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_DEVICE_INFO) & 0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_GET_SIGNAL_TEST_STAN) & 0xff);
        require.data_length = 5;
        require.data = buf;

        Touch_package reply = new Touch_package();
        reply = commandThread.addCommandToQueue(getTouchDevice(),require);
        if(!isCommandReplySuccessful(require,reply,getFunctionName())){
            return -2;
        }

        Array_to_touch_test_standard_class(reply.data,standard);

        if (reply.data_length < 18) {
            standard.method_enum = 0;
        } else {
            standard.method_enum = reply.data[17];
        }
//        Log.d(TAG, "getSignalTestStandard: 测试标准 = "+ Arrays.toString(reply.data));
        Log.d(TAG, "getSignalTestStandard: 获取信号测试项标准成功");
        return 0;
    }
    public boolean isSingleSignalTest(int testNo)
    {
        if (isUNT(testNo) || isRVR(testNo))
            return false;
//    if (testNo >= STI_ADC_X_TB && testNo <= STI_AGC_Y_TF)
//        return 1;
//    else if (testNo >= STI_ADC_X_TB_ALL && testNo <= STI_AGC_Y_TF_ALL)
//        return 1;
        return true;
    }
    public boolean isUNT(int testNo)
    {
        if (testNo >= getResInteger(R.integer.STI_UNT_X_TB) && testNo <= getResInteger(R.integer.STI_UNT_Y_TF))
            return true;
        else
            return false;
    }

    public boolean isRVR(int testNo)
    {
        if (testNo >= getResInteger(R.integer.STI_RVR_X_TB) && testNo <= getResInteger(R.integer.STI_RVR_Y_TF))
            return true;
        else
            return false;
    }
    public int setTLed(int on)
    {
        byte data = (on == getResInteger(R.integer.TLED_ON) || on == 1) ? (byte)(getResInteger(R.integer.TLED_ON)&0xff) : (byte)(getResInteger(R.integer.TLED_OFF)&0xff);

        Touch_package require = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_HARDWARE) & 0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_SET_TLED) & 0xff);
        require.data_length = 1;
        require.data[0] = data;

        Touch_package reply = new Touch_package();
        reply = commandThread.addCommandToQueue(getTouchDevice(),require);
        if(!isCommandReplySuccessful(require,reply,getFunctionName())){
            return -2;
        }
        return 0;
    }
    public int getSignalTestData(byte testIndex, byte[] data, int count, int actualCount)
    {
        int ret;
        if (myTouchDevice == null)
        {
            Log.d(TAGTestData, "请检查设备是否连接以及是否给以权限.... ");
            return -1;
        }

        int perCount ;
        int remain = count, retCount, cpyCount;
        short index = 0;

        int tryTime = 5;

        Touch_package require = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_DEVICE_INFO) & 0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_GET_SIGNAL_DATA) & 0xff);
        require.data_length = 5;
        require.data[0] = testIndex;
        Log.d("testStander", String.format("序号 = %d, 信号测试项 = %s",testIndex,signalIndexToString(byteToInt(testIndex))));
        Log.d("testStander", "测试数据个数 = " + count);
        perCount = 40;
        Touch_package reply = new Touch_package();


        while (remain > 0) {
            require.data[1] = (byte) (index & 0xff);
            require.data[2] = (byte) ((index >> 8) & 0xff);
            require.data[3] = (byte) (perCount  & 0xff);
            require.data[4] = (byte) ((perCount >> 8) & 0xff);

            reply = commandThread.addCommandToQueue(getTouchDevice(),require);
            if(byteToInt(testIndex) == 2)
            {
                byte[] buffer = new byte[64];
                packageClassToArray(reply,buffer);
//                Log.d("testStander", "接收数据: "+ Arrays.toString(buffer));
            }
            if(!isCommandReplySuccessful(require,reply,getFunctionName())){
                byte[] buffer1 = new byte[64];
                packageClassToArray(require,buffer1);
                Log.d(TAGTestData, "error:发送数据 = "+Arrays.toString(buffer1));

                byte[] buffer2 = new byte[64];
                packageClassToArray(require,buffer2);
                Log.d(TAGTestData, " error: 接收数据 = "+Arrays.toString(buffer2));
                return -2;
            }

            if (index != toWord(reply.data[1], reply.data[2])) {
                if (tryTime > 0) {
                    tryTime--;
                    continue;
                }
                Log.d(TAGTestData, "起始下标不相等 index = " + index + "toWord(reply.data[1], reply.data[2]) = "
                                                                    + toWord(reply.data[1], reply.data[2]));
                return -3;
            }
            retCount = toWord(reply.data[3], reply.data[4]);
            cpyCount = retCount > remain ? remain : retCount;
//            byte[] dst = new byte[cpyCount];
//            dst = Arrays.copyOfRange(reply.data,5,5+cpyCount);
            for(int i = 0;i < cpyCount;i++)
            {
                data[index + i] = reply.data[5 + i];
            }
//            dst += cpyCount;
            index += cpyCount;
            remain -= cpyCount;
            if (cpyCount < perCount) {
                // all is done
                break;
            }
        }
        actualCount  = (int)(index & 0xffff);
        Log.d(TAGTestData, "getSignalTestData 函数以获取的个数 = " + index);
        return actualCount;
    }
    public int setTestingMode(int on)
    {
        if(myTouchDevice == null)
        {
            return  -1;
        }
        byte data = (on == getResInteger(R.integer.TESTING_ON) || on == 1) ? (byte)(getResInteger(R.integer.TESTING_ON) & 0xff) : (byte)(getResInteger(R.integer.TESTING_OFF) & 0xff);
        Touch_package require = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_HARDWARE) & 0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_SET_TESTING) & 0xff);
        require.data_length = 1;
        require.data[0] = data;

        Touch_package reply = new Touch_package();
        reply = commandThread.addCommandToQueue(getTouchDevice(),require);
        if(!isCommandReplySuccessful(require,reply,getFunctionName())){
            return -2;
        }
        return 0;
    }

    public int isTestTledOn(Touch_test_standard tts){
        return (toDWord(tts.method_switch_0, tts.method_switch_1, tts.method_switch_2, tts.method_switch_3)
                & getResInteger(R.integer.STM_TLED_ON)) == 0 ? 0 : 1;
    }
    public int isTestRepeat(Touch_test_standard tts) {
        return (toDWord(tts.method_switch_0, tts.method_switch_1, tts.method_switch_2, tts.method_switch_3)
                & getResInteger(R.integer.STM_REPEAT)) == 0 ? 0 : 1;
    }
    public int getTestSwitch(Touch_test_standard tts) {
        return toDWord(tts.method_switch_0, tts.method_switch_1, tts.method_switch_2, tts.method_switch_3);
    }
    public int getTestMethod(Touch_test_standard tts) {
        return byteToInt(tts.method_enum);
    }
    public String signalIndexToString(int index){
        int max = signal_test_name.length;
        if (index < 0 || (index >= max)) {
            Log.d(TAG, "error signalIndexToString: "+String.format("index=%d, max=%d", index, max));
            return "";
        }
        return signal_test_name[index];
    }
    //关于界面
    public String about_text_str(Touch_info touch_info)
    {
        String str = null;
        String deviceStr = null;
        String softwareStr = null;
        if(touch_info == null)
        {
            deviceStr =  getResString(R.string.no_device) + "\n\n";
        }
        else if(!touch_info.noTouch && (touch_info.filePath == null))
        {
            deviceStr = getResString(R.string.no_hidraw) + "\n\n";
        }
        else if(!touch_info.noTouch && (touch_info.filePath.equals("existHidraw") || touch_info.filePath.equals("noPermission")))
        {
            deviceStr = getResString(R.string.no_permission) + "\n\n";
        }
        else if(touch_info.noTouch && (touch_info.mDeviceConnection == null ||
                touch_info.usbEpOut == null || touch_info.usbEpIn == null))
        {
            deviceStr = getResString(R.string.no_touch_permission) + "\n\n";
        }
        else
        {
            deviceStr = getResString(R.string.device_infomation) + "\n";
            deviceStr += getResString(R.string.device_name) + ":\n";
            deviceStr += getResString(R.string.fireware_version) + ":\n";
            deviceStr += getResString(R.string.fireware_checksum) + ":\n";
            deviceStr += getResString(R.string.support_touch_number) + ":\n";
            deviceStr += "USB VID:\n";
            deviceStr += "USB PID:\n\n";
        }
        softwareStr = getResString(R.string.software_information) + "\n";
        softwareStr += getResString(R.string.software_name) + ":\n";
        softwareStr += getResString(R.string.software_version) + ":\n";
        softwareStr += getResString(R.string.OS_name) + ":\n";
        softwareStr += getResString(R.string.OS_version) + ":\n";

        str = deviceStr + softwareStr;
        return str;
    }
    public String about_data_str(Touch_info touch_info)
    {
        String str = null;
        String deviceStr = null;
        String softwareStr = null;
        if(touch_info == null)
        {
            deviceStr = "\n\n";
        }
        else if(!touch_info.noTouch && (touch_info.filePath == null))
        {
            deviceStr = "\n\n";
        }
        else if(!touch_info.noTouch && (touch_info.filePath.equals("existHidraw") || touch_info.filePath.equals("noPermission")))
        {
            deviceStr = "\n\n";
        }
        else if(touch_info.noTouch && (touch_info.mDeviceConnection == null ||
                touch_info.usbEpOut == null || touch_info.usbEpIn == null))
        {
            deviceStr = "\n\n";
        }
        else
        {

            deviceStr ="\n";
            deviceStr += touch_info.model + "\n";
            Touch_fireware_info touch_fireware_info = getFirewareInfo();
            if(touch_fireware_info != null)
            {
                int fireware_version =  byteToChar(touch_fireware_info.version_l,touch_fireware_info.version_h);
                int fireware_checkSum = byteToChar(touch_fireware_info.checksum_l,touch_fireware_info.checksum_h);
                int support_number = touch_fireware_info.touch_point;
                int usb_vid =  byteToChar(touch_fireware_info.usb_vid_l,touch_fireware_info.usb_vid_h);
                int usb_pid = byteToChar(touch_fireware_info.usb_pid_l,touch_fireware_info.usb_pid_h);

                deviceStr += String.format("0x%04X",fireware_version) + "\n";
                deviceStr += String.format("0x%04X",fireware_checkSum) + "\n";
                deviceStr += Integer.toString(support_number) + "\n";
                deviceStr += String.format("0x%04X",usb_vid) + "\n";
                deviceStr += String.format("0x%04X",usb_pid) +"\n\n";
            }

        }
        softwareStr = "\n";
        softwareStr += "TouchAssistant\n";
        softwareStr += MainActivity.softwareVersion;
        switch (mainActivity.getAppType()) {
            case APP_CLIENT:
                softwareStr += ".C\n";
                break;
            case APP_FACTORY:
                softwareStr += ".F\n";
                break;
            case APP_RD:
                softwareStr += ".R\n";
                break;
            case APP_PCBA:
                softwareStr += ".P\n";
                break;
        }


        switch (Build.VERSION.SDK_INT)
        {
            case 29:
                softwareStr += "Android 10.0\n";
                break;
            case 28:
                softwareStr += "Android 9.0\n";
                break;
            case 27:
                softwareStr += "Android 8.1\n";
                break;
            case 26:
                softwareStr += "Android 8.0\n";
                break;
            case 25:
                softwareStr += "Android 7.1\n";
                break;
            case 24:
                softwareStr += "Android 7.0\n";
                break;
            case 23:
                softwareStr += "Android 6.0\n";
                break;
            case 22:
                softwareStr += "Android 5.1\n";
                break;
            case 21:
                softwareStr += "Android 5.0\n";
                break;
            case 19:
                softwareStr += "Android 4.4\n";
                break;
            case 18:
                softwareStr += "Android 4.3\n";
                break;
            case 17:
                softwareStr += "Android 4.2\n";
                break;
            case 16:
                softwareStr += "Android 4.1\n";
                break;
            case 15:
                softwareStr += "Android 4.0\n";
                break;
            case 13:
                softwareStr += "Android 3.2\n";
                break;
            case 12:
                softwareStr += "Android 3.1\n";
                break;
            case 11:
                softwareStr += "Android 3.0\n";
                break;
            case 10:
                softwareStr += "Android 2.3\n";
                break;
            default:
                softwareStr += "Android\n";
                break;
        }
//        softwareStr +=  System.getProperty("os.name") + "\n";  //内核
//        softwareStr += System.getProperty("os.version") + "\n";

        softwareStr += Build.VERSION.RELEASE + "\n";

        str = deviceStr + softwareStr;
        return str;
    }
    //设置部分
    public void startRefreshSetting()
    {
        refreshSettingThread = new RefreshSettingThread();
        refreshSettingThread.start();
    }
    class RefreshSettingThread extends Thread{
        @Override
        public void run() {
            Log.e(TAG, "setting: 开始刷新设置界面");
            settingFinsh = false;
            Map<String,Byte> map = getSettingsInfos();
            CalibrationSettings settings = new CalibrationSettings();
            int ret = getCalibrationSettings(settings);
            if(ret < 0)
            {
                Log.e(TAG, "refreshCalibrationData fail");
//                return;
            }
            List<CalibrationData> calibrationDataList = new ArrayList<>();
            for (int i = 0; i < settings.pointCount; i++) {
                CalibrationData data = new CalibrationData();
                ret = getCalibrationPointData((byte) 1,(byte)i,data);
                if(ret < 0)
                    return;
                calibrationDataList.add(data);
            }
            mainActivity.setting_fragment_interface.refreshAllUiInfo(map,calibrationDataList);
            Log.e(TAG, "setting: 设置界面刷新结束");
            settingFinsh = true;
        }
    }
    //校准部分
    private int[] circle1 = new int[4];//x,y,bigR,smallR
    private int[] circle2 = new int[4];
    private int[] circle3 = new int[4];
    private int[] circle4 = new int[4];
    private float[] rect = new float[4]; //左上角(x,y),右下角(x,y)
    private float[] drawLines1 = new float[8];//直线组：直线1与直线2的X轴Y轴坐标
    private float[] drawLines2 = new float[8];//
    private float[] drawLines3 = new float[8];//
    private float[] drawLines4 = new float[8];//
    private int width;
    private int height;
    public void initCalibrateData(int[] c1,int[] c2,int[] c3,int[] c4,float[] r,
                                  float[] d1,float[] d2,float[] d3,float[] d4,int screenWidth,int screenHeight){

        circle1 = Arrays.copyOf(c1,c1.length);
        circle2 = Arrays.copyOf(c2,c2.length);
        circle3 = Arrays.copyOf(c3,c3.length);
        circle4 = Arrays.copyOf(c4,c4.length);
        rect = Arrays.copyOf(r,r.length);
        drawLines1 = Arrays.copyOf(d1,d1.length);
        drawLines2 = Arrays.copyOf(d2,d2.length);
        drawLines3 = Arrays.copyOf(d3,d3.length);
        drawLines4 = Arrays.copyOf(d4,d4.length);
        width = screenWidth;
        height = screenHeight;
    }
    public void startGetCalibrationCapture(){
        calibrationCaptureThread = new CalibrationCaptureThread();
        calibrate_running = true;
        calibrationCaptureThread.start();
    }
    public void stopGetCalibrationCapture(){
        calibrationCaptureThread.setRunning(false);
        calibrate_running = false;
    }
    public interface Calibrate_Interface{
        void saveOldCalibratedata(List<CalibrationData> oldListCalibrateData);
        void refreshCollectionSchedule(CalibrationCapture calibrationCapture);
        void calibrateFinshed();
        void overCalivrate();
    }
    public static boolean firstTimeCalibrate = true;
    public boolean calibrate_running = true;
    class CalibrationCaptureThread extends Thread{

        private boolean running = true;

        public void setRecalirate(boolean recalirate) {
            this.recalirate = recalirate;
        }

        private boolean recalirate = false;

        public void setRunning(boolean running) {
            this.running = running;
        }

        public void setExitCalibrate(boolean exitCalibrate) {
            this.exitCalibrate = exitCalibrate;
        }

        private boolean exitCalibrate = false;

        private CountDownTimer timer;
        public CalibrationCaptureThread(){
            timer = new CountDownTimer(60000,1000){
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    synchronized (this)
                    {
                        running = false;
                        calibrate_running = false;
                        exitCalibrationMode();
                        restoreOldCalibrateData();
                        calibrate_interface.overCalivrate();
                    }

                }
            };
        }

        @Override
        public void run() {
            Log.d(TAG, "getCalibrateDataRun:开始 ");
            timer.start();
            if(firstTimeCalibrate)
            {
                //保存原始数据
                copyOldCalibrateData();
                enterCalibrationMode();
                firstTimeCalibrate = !firstTimeCalibrate;
            }

            //开始工作
            startWork();
            while(running && calibrate_running)
            {
                Touch_info dev = MainActivity.firstDevice();
                if(dev == null || dev.bootloader == 1 || (!touchInfo.noTouch && touchInfo.filePath == null) ||
                        (touchInfo.noTouch && (touchInfo.usbEpIn == null || touchInfo.usbEpOut == null || touchInfo.mDeviceConnection == null)) ||
                        dev.model == null || dev.model.equals(""))
                {
                    timer.cancel();
                    calibrate_interface.overCalivrate();
                    calibrate_running = false;
                    break;
                }

                if(recalirate)
                {
                    timer.cancel();
                    timer.start();
                    recalirate = false;
                    restoreOldCalibrateData();
                    setPointActive(0);
                }
                if(exitCalibrate)
                {
                    timer.cancel();
                    calibrate_running = false;
                    restoreOldCalibrateData();
                    exitCalibrationMode();
                    calibrate_interface.overCalivrate();
                    break;
                }

                //访问设备采集进度
                CalibrationCapture calibrationCapture = new CalibrationCapture();
                int ret = getCalibrationCapture(calibrationCapture);
                if(ret < 0)
                {
//                    Log.e(TAG, "setPointActive: 访问设备采集进度失败" );
//                    timer.cancel();
//                    calibrate_interface.overCalivrate();
//                    break;
                    continue;
                }
                synchronized (this)
                {
                    if(!running || !calibrate_running)
                        break;
                    calibrate_interface.refreshCollectionSchedule(calibrationCapture);
                }

                if (calibrationCapture.count > 0 && calibrationCapture.finished == calibrationCapture.count)
                {
                    if(calibrationCapture.index == 3)
                    {
                        Log.e(TAG, "run: 校准完成" );
                        //校准完成
                        calibrate_running = false;
                        timer.cancel();
                        saveCalibrationData();
//                        exitCalibrationMode();
                        calibrate_interface.calibrateFinshed();
                        break;
                    }
                    int index = calibrationCapture.index + 1;
                    setPointActive(index);
                }
                if(calibrationCapture.finished > 0)
                {
                    timer.cancel();
                    timer.start();
                }

                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(!running || !calibrate_running)
            {
                Log.d(TAG, "getCalibrateDataRun:结束 ");
                calibrate_running = false;
                timer.cancel();
            }
        }
    }
    public void copyOldCalibrateData()
    {
        CalibrationSettings oldSettings = new CalibrationSettings();
        int ret = getCalibrationSettings(oldSettings);
        if(ret < 0)
        {
            Log.e(TAG, "copyOldCalibrateData fail");
            return;
        }

        oldListCalibrateData = new ArrayList<>();
        for (int i = 0; i < oldSettings.pointCount; i++) {
            CalibrationData data = new CalibrationData();
            ret = getCalibrationPointData((byte)1,(byte)i,data);
            if(ret < 0)
                return;
            oldListCalibrateData.add(data);
        }
        calibrate_interface.saveOldCalibratedata(oldListCalibrateData);
    }
    public void restoreOldCalibrateData(){
    for (int i = 0; i < oldListCalibrateData.size(); i++) {
        setCalibrationPointData((byte)i,oldListCalibrateData.get(i));
    }
        saveCalibrationData();
    }
    public void startWork(){
        setPointActive(0);
    }
    public boolean setPointActive(int index)
    {
        CalibrationData data = new CalibrationData();
        int[] circle = new int[4];//x,y,bigR,smallR
        switch (index)
        {
            case 0:
                circle = Arrays.copyOf(circle1,circle1.length);
                break;
            case 1:
                circle = Arrays.copyOf(circle2,circle2.length);
                break;
            case 2:
                circle = Arrays.copyOf(circle3,circle3.length);
                break;
            case 3:
                circle = Arrays.copyOf(circle4,circle4.length);
                break;
        }
        data.targetX = circle[0];
        data.targetY = circle[1];
        data.collectX = 0xffff;
        data.collectY = 0xffff;
        data.maxX =  width;
        data.maxY = height;
        //计算靶心坐标点数据并设置校准点数据
        int ret = setCalibrationPointData((byte)index,data);
        if(ret < 0)
        {
            Log.e(TAG, "setPointActive: 计算靶心坐标点数据并设置校准点数据失败" );
            return false;
        }
        //通知开始采集数据
        ret = startCalibrationCapture((byte)index);
        if(ret < 0)
        {
            Log.e(TAG, "setPointActive: 通知开始采集数据失败" );
            return false;
        }
        return true;
    }


    public boolean enterCalibrationMode() {
        if (myTouchDevice == null || (!touchInfo.noTouch && touchInfo.filePath == null) ||
                (touchInfo.noTouch && (touchInfo.usbEpIn == null || touchInfo.usbEpOut == null || touchInfo.mDeviceConnection == null)))
            return false;
        int ret;
        ret = setCalibrationMode((byte)getResInteger(R.integer.CALIBRATION_MODE_COLLECT));
        if (ret != 0)
            return false;
        ret = setCoordsEnabled((byte)getResInteger(R.integer.COORDS_CHANNEL_SERIAL),(byte)getResInteger(R.integer.COORDS_CHANNEL_DISABLE) );
        if (ret != 0)
            return false;
        ret = setCoordsEnabled((byte)getResInteger(R.integer.COORDS_CHANNEL_USB),(byte)getResInteger(R.integer.COORDS_CHANNEL_DISABLE));
        if (ret != 0)
            return false;
        return true;
    }

    public boolean exitCalibrationMode() {
        if (myTouchDevice == null || (!touchInfo.noTouch && touchInfo.filePath == null) ||
                (touchInfo.noTouch && (touchInfo.usbEpIn == null || touchInfo.usbEpOut == null || touchInfo.mDeviceConnection == null)))
            return false;
        int ret;
        ret = setCalibrationMode((byte)getResInteger(R.integer.CALIBRATION_MODE_CALIBRATION));
        if (ret != 0)
            return false;
        ret = setCoordsEnabled((byte)getResInteger(R.integer.COORDS_CHANNEL_SERIAL),(byte)getResInteger(R.integer.COORDS_CHANNEL_ENABLE));
        if (ret != 0)
            return false;
        ret = setCoordsEnabled((byte)getResInteger(R.integer.COORDS_CHANNEL_USB),(byte)getResInteger(R.integer.COORDS_CHANNEL_ENABLE));
        if (ret != 0)
            return false;
        return true;
    }

    public Map<String,Byte> getSettingsInfos()
    {
        Map<String ,Byte> map = new HashMap<>();
        byte val;
        if (myTouchDevice == null || (!touchInfo.noTouch && touchInfo.filePath == null) ||
                (touchInfo.noTouch && (touchInfo.usbEpIn == null || touchInfo.usbEpOut == null || touchInfo.mDeviceConnection == null)))
            return null;
        if(!MainActivity.noTouch)
        {
            val = -1;
            val = getCoordsEnabled((byte)getResInteger(R.integer.COORDS_CHANNEL_USB));
            map.put("usbEnable", val);
            Log.d(TAG, "getSettingsInfos: " + String.format("usbEnable:%d", val));
            if(val == 1)
            {
                val = -1;
                val = getCoordsMode((byte)getResInteger(R.integer.COORDS_CHANNEL_USB));
                map.put("usbMode", val);
                Log.d(TAG, "getSettingsInfos: " + String.format("usbMode:%d", val));
            }
            val = -1;
            val = getCoordsEnabled((byte)getResInteger(R.integer.COORDS_CHANNEL_SERIAL));
            map.put("uartEnable", val);
        }


//        val = -1;
//        val = getCoordsMode((byte)getResInteger(R.integer.COORDS_CHANNEL_SERIAL));
//        map.put("serialMode", val);
//        Log.d(TAG, "getSettingsInfos: " + String.format("serialMode:%d", val));

        //获取旋转参数
        byte[] values = new byte[2];
        byte[]defs = new byte[2];
        initByteArray(values);
        initByteArray(defs);
        getRotation(values, defs);
        map.put("touchRotation", values[0]);
        map.put("screenRotation", values[1]);
        Log.d(TAG, "getSettingsInfos: "+String.format("touch rotation: %d", values[0]));
        Log.d(TAG, "getSettingsInfos: " + String.format("screen rotation: %d", values[1]));

        //获取触摸框坐标翻转参数
        initByteArray(values);
        initByteArray(defs);
        getMirror(values, defs);
        map.put("xMirror", values[0]);
        map.put("yMirror", values[1]);
        Log.d(TAG, "getSettingsInfos: "+String.format("xMirror: %d", values[0]));
        Log.d(TAG, "getSettingsInfos: "+String.format("yMirror: %d", values[1]));

        //获取MAC OS坐标模式的设定
        values[0] = -1;
        defs[0] = -1;
        getMacOSMode( values, defs);
        map.put("mac", values[0]);
        Log.d(TAG, "getSettingsInfos: "+String.format("mac: %d", values[0]));

        //获取坐标通道是否已经使能
        byte enabled = 0;
//        enabled = getCoordsEnabled((byte)getResInteger(R.integer.COORDS_CHANNEL_USB));
//        map.put("usbEnabled", enabled);
//        Log.d(TAG, "getSettingsInfos: "+String.format("usb enabled: %d", enabled));
//
//        enabled = 0;
//        enabled = getCoordsEnabled((byte)getResInteger(R.integer.COORDS_CHANNEL_SERIAL));
//        map.put("serialEnabled", enabled);
//        Log.d(TAG, "getSettingsInfos: "+String.format("serial enabled: %d", enabled));

        //获取AGC锁定状态
//        enabled = isLockAGC();
//        map.put("lockAGC", enabled);
//        Log.d(TAG, "getSettingsInfos: " + String.format("LockAGC: %d", enabled));

        return map;
    }
    public byte getCoordsMode(byte channel)
    {
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_COORDS)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_GET_COORDS_MODE)&0xff);
        require.data_length = 1;
        require.data[0] = channel;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.d(TAG, "getCoordsMode: 获取坐标点模式失败");
            return -2;
        }
        byte mode = reply.data[1];
        Log.d(TAG, "getCoordsMode: 获取坐标点模式成功 stadus = " + mode);
//        mainActivity.setTestTextView("getCoordsEnabled: 坐标通道使能获取成功 stadus = " + mode);
        return mode;
    }
    public byte setCoordsMode(byte channel,byte mode)
    {
        byte[] data = new byte[2];
        data[0] = channel;
        data[1] = mode;
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_COORDS)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_SET_COORDS_MODE)&0xff);
        require.data_length = 2;
        require.data = data;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "getCoordsMode: 获取坐标点模式失败");
            return -2;
        }
        return 0;
    }
    public int getRotation(byte[]current, byte[]def)
    {
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_COORDS)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_GET_ROTATION)&0xff);
        require.data_length = 0;
        require.data = null;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "getRotation: 获取旋转参数失败");
            return -2;
        }
        current[0] = reply.data[0];
        current[1] = reply.data[1];
        def[0] = reply.data[2];
        def[1] = reply.data[3];
        return 0;
    }
    public int setRotation(byte[]buffer)
    {
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_COORDS)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_SET_ROTATION)&0xff);
        require.data_length = 2;
        require.data = buffer;
        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "getRotation: 获取旋转参数失败");
            return -2;
        }
        return 0;
    }
    public int getMirror(byte[]current, byte[]def)
    {
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_COORDS)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_GET_REFLECTION)&0xff);
        require.data_length = 0;
        require.data = null;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "getMirror: 获取触摸框坐标翻转参数失败");
            return -2;
        }
        current[0] = reply.data[0];
        current[1] = reply.data[1];
        def[0] = reply.data[2];
        def[1] = reply.data[3];
        return 0;
    }
    public int setMirror(byte[]values)
    {
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_COORDS)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_SET_REFLECTION)&0xff);
        require.data_length = 2;
        require.data = values;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "getMirror: 设置触摸框坐标翻转参数失败");
            return -2;
        }
        return 0;
    }
    public int getMacOSMode(byte[]current, byte[]def)
    {
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_COORDS)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_GET_MACOS_MODE)&0xff);
        require.data_length = 0;
        require.data = null;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "getMirror: 获取MAC OS模式失败");
            return -2;
        }
        current[0] = reply.data[0];
        def[0] = reply.data[1];
        return 0;
    }
    public int setMacOSMode(byte mode)
    {
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_COORDS)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_SET_MACOS_MODE)&0xff);
        require.data_length = 1;
        require.data[0] = mode;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "getMirror: 设置MAC OS模式成功");
            return -2;
        }
        return 0;
    }
    public byte isLockAGC()
    {
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_HARDWARE)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_GET_LOCK_AGC)&0xff);
        require.data_length = 0;
        require.data = null;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "isLockAGC: 获取AGC锁定状态失败");
            return -2;
        }
        return (reply.data[0] == (byte)(getResInteger(R.integer.LOCK_AGC_ENABLE)&0xff) ? (byte)(getResInteger(R.integer.LOCK_AGC_ENABLE)&0xff) : (byte)(getResInteger(R.integer.LOCK_AGC_DISABLE)&0xff));
    }

    public int setCalibrationMode(byte mode)
    {
        byte[] data = new byte[1];
        data[0] = mode;
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_CALIBRATION)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_SET_WORK_MODE)&0xff);
        require.data_length = 1;
        require.data = data;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "setCalibrationMode failed");
            return -2;
        }
        return 0;
    }

    public int getCalibrationSettings(CalibrationSettings calibrationSettings)
    {
        if(calibrationSettings == null)
            return -1;
        byte[] data = new byte[4];
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_CALIBRATION)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_GET_CALI_SETTINGS)&0xff);
        require.data_length = 0;
        require.data = null;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "getCalibrationSettings failed");
            return -2;
        }
        byteArrayCopy(data,0,reply.data,0,4);
        ArrayAndClassConversion.arrayToCalibrationSettings(data,calibrationSettings);
        return 0;
    }
    public int setCalibrationSettings(CalibrationSettings calibrationSettings)
    {
        if(calibrationSettings == null)
            return -1;
        byte[] data = new byte[4];
        ArrayAndClassConversion.calibrationSettingsToArray(calibrationSettings,data);
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_CALIBRATION)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_SET_CALI_SETTINGS)&0xff);
        require.data_length = 2;
        require.data = data;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "setCalibrationSettings failed");
            return -2;
        }
        return 0;
    }
    public int startCalibrationCapture(byte index)
    {
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_CALIBRATION)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_START_CAPTURE)&0xff);
        require.data_length = 1;
        require.data[0] = index;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "startCalibrationCapture failed");
            return -2;
        }
        return 0;
    }
    public int saveCalibrationData()
    {
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_CALIBRATION)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_SAVE_CALI_DATA)&0xff);
        require.data_length = 0;
        require.data = null;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "saveCalibrationData failed");
            return -2;
        }
        return 0;
    }
    public int getCalibrationCapture(CalibrationCapture data)
    {
        if(data == null)
            return -1;
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_CALIBRATION)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_GET_CAPTURE_DATA)&0xff);
        require.data_length = 0;
        require.data = null;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "getCalibrationCapture failed");
            return -2;
        }
        data.index = reply.data[0];
        data.finished = (short) toWord(reply.data[1],reply.data[2]);
        data.count = (short) toWord(reply.data[3],reply.data[4]);
        return 0;
    }
    public int getCalibrationPointData(byte where, byte index, CalibrationData data)
    {
        if(data == null)
            return -1;
        byte[] buf = new byte[2];
        buf[0] = where;
        buf[1] = index;
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_CALIBRATION)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_GET_CALI_POINT_DATA)&0xff);
        require.data_length = 2;
        require.data = buf;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "getCalibrationPointData failed");
            return -2;
        }
        data.targetX = byteToInt_HL(reply.data,2);
        data.targetY = byteToInt_HL(reply.data,6);
        data.collectX = byteToInt_HL(reply.data,10);
        data.collectY = byteToInt_HL(reply.data,14);
        data.maxX = byteToInt_HL(reply.data,18);
        data.maxY = byteToInt_HL(reply.data,22);
        return 0;
    }
    public int setCalibrationPointData(byte index,CalibrationData data)
    {
        if(data == null)
            return -1;
        byte[] temp = new byte[25];
        temp[0] = index;
        intToByte_LH(data.targetX, temp,1);
        intToByte_LH(data.targetY, temp,5);
        intToByte_LH(data.collectX, temp,9);
        intToByte_LH(data.collectY, temp,13);
        intToByte_LH(data.maxX, temp,17);
        intToByte_LH(data.maxY, temp,21);
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_CALIBRATION)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_SET_CALI_POINT_DATA)&0xff);
        require.data_length = 25;
        require.data = temp;

        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "setCalibrationPointData failed");
            return -2;
        }
        return 0;
    }
    //信号图部分
    private volatile List<Map<String,Integer>> checkSignalMapList;
    public void refreshSigal(boolean force,List<Map<String,Integer>> _checkSignalMapList)
    {
        if(_checkSignalMapList != null && _checkSignalMapList.size() > 0)
        {
            checkSignalMapList = _checkSignalMapList;
        }
        else
        {
            checkSignalMapList = null;
        }
        if(force)
        {
            if(signalIsRunning)
                return;
            Log.e(TAG, "signalRun: 创建新的线程刷新信号图" );
            getSignalThread = new GetSignalThread();
            getSignalThread.setRunning(true);
            signalFinshed = false;
            signalIsRunning = true;
            getSignalThread.start();
        }
        else
        {
            if(getSignalThread != null)
            {
                Log.e(TAG, "signalRun: 设置停止刷新信号图" );
                getSignalThread.setRunning(false);
//                signalIsRunning = false;
            }
        }
    }
    class GetSignalThread  extends Thread {
        public void setRunning(boolean running) {
            this.running = running;
        }

        boolean running;

        @Override
        public void run() {
            int index;
            int count = 0;
            int FIXED_PERIOD  = 16; // 60Fps
            long period = FIXED_PERIOD;
//            List<TouchTestData> listSignalData = new ArrayList<>();
            List<Map<String,Integer>> realSignalList = new ArrayList<>();
            Log.d(TAG, "signalRun: GetSignalThread thread start");

            while (running && signalIsRunning) {
                Touch_info touchInfo = MainActivity.firstDevice();
                if(touchInfo == null || touchInfo.bootloader == 1 || (!touchInfo.noTouch && (touchInfo.filePath == null || touchInfo.filePath.equals("existHidraw"))) ||
                        (touchInfo.noTouch && (touchInfo.usbEpIn == null || touchInfo.usbEpOut == null || touchInfo.mDeviceConnection == null)) ||
                        touchInfo.model == null ||touchInfo.model.equals("") )
                {

                    signalFinshed = true;
                    signalIsRunning = false;
                    Log.d(TAG, "signalRun: 2GetSignalThread thread end");
                    return;
                }

                if(checkSignalMapList == null || checkSignalMapList.size() <= 0 && signalIsRunning)
                {
//                    Log.e(TAG, "signalRun: checkSignalMapList == null");
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                realSignalList.clear();
                synchronized (TouchManager.class)
                {
                    for(int i = 0; i < checkSignalMapList.size();i++)
                    {
                        realSignalList.add(checkSignalMapList.get(i));
                    }
                }
//                Log.e(TAG, "getSignal: 获取数据中");
//                listSignalData.clear();
                long startMillTime = System.currentTimeMillis();
//                    for (Map<String,Integer> map:checkSignalMapList){
                for (int i = 0; i < realSignalList.size() && running && signalIsRunning;i++){
                    Map<String,Integer> map = realSignalList.get(i);
                    if(map == null)
                        continue;
                    index = map.get("signalNum");
                    TouchTestData touchTestData = new TouchTestData();
                    int ret = getSignalDatas(touchTestData ,index, count,true);
                    if(ret < 0)
                    {
                        continue;
                    }
                    while (SignalCanvasView.refreshInterface)
                    {

                        try {
                            sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    mainActivity.signal_fragment_interface.upgradeSignaldata(touchTestData);
                }
//                Log.e(TAG, "getSignal: 获取数据结束");


                long delay = period - (System.currentTimeMillis() - startMillTime);
                if (delay > 0) {
                    // excess
                    if (!running && !signalIsRunning)
                    {
                        signalFinshed = true;
                        signalIsRunning = false;
                        Log.d(TAG, "signalRun: 1GetSignalThread thread end");
                        break;
                    }

                    try {
                        sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    period = FIXED_PERIOD;
                } else {
                    // insufficient, need add this overtime
                    period = FIXED_PERIOD + Math.abs(delay);
                }
            }
            signalFinshed = true;
            signalIsRunning = false;
            Log.d(TAG, "signalRun: 2GetSignalThread thread end");
        }
    };
    public List<UntData> untDataBuf = new ArrayList<>();
    public int getSignalDatas(TouchTestData data,int index, int dataCount, boolean useOldData)
    {
        if(index < 0 || myTouchDevice == null || (!touchInfo.noTouch && touchInfo.filePath == null) ||
                (touchInfo.noTouch && (touchInfo.usbEpOut == null || touchInfo.usbEpIn == null || touchInfo.mDeviceConnection == null)))
        {
            return -1;
        }
        boolean gotoEnd = false;
        Touch_test_standard standard = new Touch_test_standard();
        int ret;
        byte[] buf = new byte[1024];
        int tledState = isTLedOn();
        if (tledState < 0)
            tledState = getResInteger(R.integer.TLED_ON);

        if (dataCount <= 0) {
            ret = getSignalTestStandard((byte)index,standard,getAppMode());
            if (ret != 0) {
                Log.e(TAG, "getSignalDatas: " + String.format("get signal test standard failed %d", index));
                return -2;
            }
            dataCount = toWord(standard.count_l, standard.count_h);
        }
        if (dataCount < 0) {
            return -3;
        }
        int count = 0;
        int setTled = isTestTledOn(standard);

        int isRepeat = isTestRepeat(standard);

        int testMethod = getTestMethod(standard);

        if (standard.method_enum == 0) {
//            Log.e(TAG, "getSignalDatas: 4444444444444444444444");

            if (isRVR(index)) {
                if (tledState == getResInteger(R.integer.TLED_ON)) {
                    setTLed(0);
                }
            } else {
                if (tledState == getResInteger(R.integer.TLED_OFF)) {
                    setTLed(1);
                }
            }

            if (isUNT(index)) {

                List<UntData> bufDataList = new ArrayList<>();
                int TEST_COUNT = isRepeat == 1 ? 25 : 1;
                int get_count = TEST_COUNT;
                for(int i = (untDataBuf.size() - 1);i >= 0 && TEST_COUNT > 1;i--)
                {
                    if(untDataBuf.get(i).index == index)
                    {
                        bufDataList.add(untDataBuf.get(i));
                        if(bufDataList.size() == TEST_COUNT - 1)
                            break;
                    }
                }
                if(bufDataList.size() > 1)
                {
                    get_count = TEST_COUNT - bufDataList.size();
                }
                else
                {
                    get_count = TEST_COUNT;
                }
                for (int ti = 0; ti < get_count && !gotoEnd; ti++) {
                    byte[] tmpData = new byte[dataCount];
                    count = getSignalTestData((byte) index, tmpData, dataCount, count);
                    if (count < 0) {
                        Log.e(TAG, "getSignalDatas: " + String.format("test number 0x%02x, get data command failed", index));
                        count = dataCount;
                        gotoEnd = true;
                    } else {
                        UntData untData = new UntData();
                        untData.index = index;
                        untData.buf = Arrays.copyOf(tmpData,count);
                        bufDataList.add(untData);
                        if(TEST_COUNT > 1 )
                        {
                            untDataBuf.add(untData);
                            if(bufDataList.size() == TEST_COUNT)
                            {
                                for(int j = 0;j < untDataBuf.size();j++)
                                {
                                    if(untDataBuf.get(j).index == index)
                                    {
                                        untDataBuf.remove(j);
                                    }
                                }
                            }
                        }
                    }
                }

                // delta
                for (int ti = 0; ti < dataCount && !gotoEnd; ti++) {
                    int vMax = 0, vMin = 0xff;
                    int tmpVal = 0;
                    // find dataCount max, min
                    for (int i = 0; i < TEST_COUNT; i++) {
                        tmpVal = byteToInt(bufDataList.get(i).buf[ti]);
                        //TEST_DEBUG("test number 0x%02x, index %d: 0x%02x", testNo, i, tmpVal);
                        if (tmpVal > vMax)
                            vMax = tmpVal;
                        if (tmpVal < vMin)
                            vMin = tmpVal;
                    }

                    int delta = vMax - vMin;

                    data.datas.add(delta);
                }
            }
            else {

//                Log.e(TAG, "getSignalDatas: 33333333333333333333333");
                count = getSignalTestData((byte)index, buf, dataCount, count);
                if (count <  0) {
                    Log.e(TAG, "getSignalDatas: get signal test datas failed" );
                    return -4;
                }
                for (int i = 0; i < count; i++) {
                    data.datas.add(byteToInt(buf[i]));
                }
            }
        }
        else {

//        TDEBUG("peter testMethod:%d", testMethod);
            switch (testMethod) {
                case 0x02: {
                    List<UntData> bufDataList = new ArrayList<>();
                    int TEST_COUNT = isRepeat == 1 ? 25 : 1;
                    int get_count = TEST_COUNT;
                    for(int i = (untDataBuf.size() - 1);i >= 0 && TEST_COUNT > 1;i--)
                    {
                        if(untDataBuf.get(i).index == index)
                        {
                            bufDataList.add(untDataBuf.get(i));
                            if(bufDataList.size() == TEST_COUNT - 1)
                                break;
                        }
                    }
                    if(bufDataList.size() > 1)
                    {
                        get_count = TEST_COUNT - bufDataList.size();
                    }
                    else
                    {
                        get_count = TEST_COUNT;
                    }
                    for (int ti = 0; ti < get_count && !gotoEnd; ti++) {
                        byte[] tmpData = new byte[dataCount];
                        count = getSignalTestData((byte) index, tmpData, dataCount, count);
                        if (count < 0) {
                            Log.e(TAG, "getSignalDatas: " + String.format("test number 0x%02x, get data command failed", index));
                            count = dataCount;
                            gotoEnd = true;
                        } else {
                            UntData untData = new UntData();
                            untData.index = index;
                            untData.buf = Arrays.copyOf(tmpData,count);
                            bufDataList.add(untData);
                            if(TEST_COUNT > 1 )
                            {
                                untDataBuf.add(untData);
                                if(bufDataList.size() == TEST_COUNT)
                                {
                                    for(int j = 0;j < untDataBuf.size();j++)
                                    {
                                        if(untDataBuf.get(j).index == index)
                                        {
                                            untDataBuf.remove(j);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // delta
                    for (int ti = 0; ti < dataCount && !gotoEnd; ti++) {
                        int vMax = 0, vMin = 0xff;
                        int tmpVal = 0;
                        // find dataCount max, min
                        for (int i = 0; i < TEST_COUNT; i++) {
                            tmpVal = byteToInt(bufDataList.get(i).buf[ti]);
                            //TEST_DEBUG("test number 0x%02x, index %d: 0x%02x", testNo, i, tmpVal);
                            if (tmpVal > vMax)
                                vMax = tmpVal;
                            if (tmpVal < vMin)
                                vMin = tmpVal;
                        }

                        int delta = vMax - vMin;

                        data.datas.add(delta);
                    }
                }
                break;
                case 0x01:
                default: {
//            TDEBUG("RAW RANGE");
//                    Log.e(TAG, "getSignalDatas: 111111111111111111111111111");
                    count = getSignalTestData((byte)index, buf, dataCount,count);

                    if (count < 0) {
                        Log.e(TAG, "getSignalDatas: get signal test datas failed");
                        return -5;
                    }
                    for (int i = 0; i < dataCount; i++) {
                        data.datas.add(byteToInt(buf[i]));
                    }
                }
                break;
            }

        }
        setTLed(tledState);
        data.count = dataCount;
        data.f_max = byteToInt(standard.factory_max);
        data.f_min = byteToInt(standard.factory_min);
        data.c_min = byteToInt(standard.client_min);
        data.c_max = byteToInt(standard.client_max);
        data.r_min = byteToInt(standard.min);
        data.r_max = byteToInt(standard.max);
        data.number = index;

        return 0;
    }
    public void setClickSignalItem(List<Map<String,Integer>> mapList){
        synchronized (TouchManager.class)
        {
            checkSignalMapList = mapList;
        }

    }

    public int setTesting(int on)
    {
        byte data = (on == getResInteger(R.integer.TESTING_ON)) ? (byte)getResInteger(R.integer.TESTING_ON) : (byte)getResInteger(R.integer.TESTING_OFF);
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_HARDWARE)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_SET_TESTING)&0xff);
        require.data_length = 1;
        require.data[0] = data;
        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "setTesting failed");
            return -2;
        }
        return 0;
    }
    public int isTesting()
    {
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_HARDWARE)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_GET_TESTING)&0xff);
        require.data_length = 0;
        require.data = null;
        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "isTesting failed");
            return -2;
        }
        return reply.data[0]; // 1表示测试状态，0表示非测试状态
    }
    int setLockAGC( int on)
    {
        byte data = (on == getResInteger(R.integer.LOCK_AGC_ENABLE)) ? (byte)getResInteger(R.integer.LOCK_AGC_ENABLE) : (byte)getResInteger(R.integer.LOCK_AGC_DISABLE);
        Touch_package require = new Touch_package();
        Touch_package reply = new Touch_package();
        require.master_cmd = (byte)(getResInteger(R.integer.TOUCH_M_CMD_HARDWARE)&0xff);
        require.sub_cmd = (byte)(getResInteger(R.integer.TOUCH_S_CMD_SET_LOCK_AGC)&0xff);
        require.data_length = 1;
        require.data[0] = data;
        reply = commandThread.addCommandToQueue(getTouchDevice(),require);

        if (!isCommandReplySuccessful(require, reply, getFunctionName())) {
            Log.e(TAG, "setLockAGC failed");
            return -2;
        }
        return 0;
    }


    public static int toDWord(byte a0,byte a1,byte a2,byte a3)
    {
        int b0,b1,b2,b3;
        if(a0 < 0)
            b0 = 256 - Math.abs(a0);
        else
            b0 = a0;
        if(a1 < 0)
            b1 = 256 - Math.abs(a1);
        else
            b1 = a1;
        if(a2 < 0)
            b2 = 256 - Math.abs(a2);
        else
            b2 = a2;
        if(a3 < 0)
            b3 = 256 - Math.abs(a3);
        else
            b3 = a3;
        return (b0 + (b1 << 8) + (b2 << 16) + (b3 << 24));
    }
    public static int toWord(byte low,byte high)
    {
        int h,l;
        if(low < 0)
            l = 256 - Math.abs(low);
        else
            l = low;
        if(high < 0)
            h = 256 - Math.abs(high);
        else
            h = high;
        return (((h << 8) | l) & 0xff);
    }
    public void byteArrayCopy(byte[] dest,int doffset,byte[] ori,int ooffset,int length)
    {
        for(int i = 0;(i < length) && (doffset + i < dest.length) && (ooffset + i < ori.length);i++)
        {
            dest[doffset + i] = ori[ooffset + i];
        }
    }
    public static void wordToPackage(short value, byte[] target) {
        target[0] = (byte)(value & 0xff);
        target[1] = (byte)((value >> 8) & 0xff);
    }
//    public static void intToLittleEndian(int mode,byte[] buffer){
//        //小端序：低字节在前，高字节在后
//        int count = 0;
//        for(int i = 0;i < 4;i++)
//        {
//            if(buffer[i] == -1)
//            {
//                buffer[i] = (byte)((mode >> (count++ * 8)) & 0xff);
//            }
//        }
//    }
//    public static void shortToLittleEndian(short mode,byte[] buffer){
//        //小端序：低字节在前，高字节在后
//        int count = 0;
//        for(int i = 0;i < 2;i++)
//        {
//            if(buffer[i] == -1)
//            {
//                buffer[i] = (byte)((mode >> (count++ * 8)) & 0xff);
//            }
//        }
//    }
     public static void shortToByte_LH(short shortVal, byte[] b, int offset) {
        b[0 + offset] = (byte) (shortVal & 0xff);
        b[1 + offset] = (byte) (shortVal >> 8 & 0xff);
     }
    public static void intToByte_LH(int intVal, byte[] b, int offset) {
        b[0 + offset] = (byte) (intVal & 0xff);
        b[1 + offset] = (byte) (intVal >> 8 & 0xff);
        b[2 + offset] = (byte) (intVal >> 16 & 0xff);
        b[3 + offset] = (byte) (intVal >> 24 & 0xff);
    }
    public static short byteToShort_HL(byte[] b, int offset)
    {
        short result;
        result = (short) ((((b[offset + 1]) << 8) & 0xff00 | b[offset] & 0x00ff));
        return result;
    }
    public static int byteToInt_HL(byte[] b, int offset)
    {
        int result;
        result = (((b[3 + offset] & 0x00ff) << 24) & 0xff000000)
                | (((b[2 + offset] & 0x00ff) << 16) & 0x00ff0000)
                | (((b[1 + offset] & 0x00ff) << 8) & 0x0000ff00)
                | ((b[0 + offset] & 0x00ff));
        return result;
    }

    public void initByteArray(byte[] buffer){
        for (int i = 0;i < buffer.length;i++)
        {
            buffer[i] = -1;
        }
    }
    public String getFunctionName(){
        String funcName = new Throwable().getStackTrace()[1].getMethodName();
        return funcName;
    }

    public void Array_to_touch_test_standard_class(byte[] data,Touch_test_standard tts){
        tts.no = data[0];
        tts.count_l = data[1];
        tts.count_h = data[2];
        tts.factory_min = data[3];
        tts.factory_max = data[4];
        tts.client_min = data[5];
        tts.client_max = data[6];

        tts.mode = toDWord(data[7],data[8],data[9],data[10]);
        tts.min = data[11];
        tts.max = data[12];
        tts.method_switch_0 = data[13];
        tts.method_switch_1 = data[14];
        tts.method_switch_2 = data[15];
        tts.method_switch_3 = data[16];
        tts.method_enum = data[17];
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
    public static int shortToInt(byte l,byte h){
        return (h << 8 | l);
    }
    public static long toUnsignedLong(long a){
        long b;
        if(a < 0)
        {
            b = (long)(Math.pow(2,32) - Math.abs(a)) & 0xffffffff;
//            Log.d(TAG, "unsignedLong: a = " + a + "toUnsignedLong = " + b);
        }
        else
        {
            b = a;
        }

        return b;
    }
    public  int getAppMode(){
        int mode =  getResInteger(R.integer.STE_ALL_ITEMS);
        switch (MainActivity.getAppType()) {
            case APP_CLIENT:
                mode = getResInteger(R.integer.STE_END_USER_GRAPH);
                break;
            case APP_FACTORY:
                mode = getResInteger(R.integer.STE_FACTORY_GRAPH);
                break;
            case APP_RD:
                mode = getResInteger(R.integer.STE_DEV_GRAPH);
                break;
            case APP_PCBA:
                mode = getResInteger(R.integer.STE_PCBA_CUSTOMER_GRAPH);
                break;
        }
        return mode;
    }
}

