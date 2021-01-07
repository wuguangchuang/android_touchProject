package fragment_package;

import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.touch.MainActivity;
import com.example.touch.R;
import com.example.touch.SignalCanvasView;
import com.example.touch.TouchManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import adapter.Signal_RecycleViewAdapter;
import dataInformation.SignalData;
import dataInformation.TouchTestData;
import dataInformation.Touch_test_standard;
import fragment_interface.Signal_fragment_interface;

public class Signal_fragment extends Fragment implements Signal_fragment_interface {

    private static final String TAG = "myText";
//    private SignalAdapter signalAdapter;
    private Signal_RecycleViewAdapter signalAdapter;
    private List<String> testSignalList = new ArrayList<>();
//    HorizontalScrollView horizontalScrollView ;
//    ListView testItemListView;
    private RecyclerView signalItemRecycleView;
    public static FrameLayout canvasFrame;
    private LinearLayout bottom_btnS;
    private Button realRefreshBtn;
    private Button initSignalBtn;
    private Button autoHidCoordBtn;
    private Button testModeBtn;
    View view;
    SignalCanvasView signalCanvasView;

    public static TypedArray signalColorS;
    public String[] signal_test_items ;
    private int checkedCount = 0;
    private int maxChecked = 8;

    private ListenBottombtn listenBottombtn;
    private boolean enterTest = false;          //测试模式状态
    private boolean currentStatus = false;      //自动屏蔽坐标状态
    public static boolean needRestoreStatus = false;
    private boolean usbStatus = true;
    private boolean serialStatus = true;
    private int usb_channel = 1;
    private int serial_channel = 2;
    private boolean stopRefresh = false;    //是否停止实时刷新
    private boolean initSignal = false;     //信号初始化

//    private MainActivity.Signal_interface_to_touchManager touchManager;
    public static byte[]items;
    public static List<Touch_test_standard> testStandardList;
    public static List<Map<String,Integer>> checkSignalMapList;
    public List<SignalData> signalDataList;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if(view == null)
        {
            testStandardList = new ArrayList<>();
            checkSignalMapList = new ArrayList<>();
            signalDataList = new ArrayList<>();
            items = new byte[1024];
            signalColorS = getResources().obtainTypedArray(R.array.signalColorS);
            signal_test_items = getResources().getStringArray(R.array.signal_test_name);
            ((MainActivity)getActivity()).setSignal_fragment_interface(this);
//            touchManager = ((MainActivity)getActivity()).signal_interface_to_touchManager;
            Log.d(TAG, "onCreateView: 缓存SignalPage");
            view = inflater.inflate(R.layout.signal_fragment,container,false);
            initControl(view);
            listenBottombtn();
//            signalAdapter = new SignalAdapter(getActivity(),testSignalList);
//            testItemListView.setAdapter(signalAdapter);

            LinearLayoutManager ms= new LinearLayoutManager(getActivity());
            // 设置 recyclerview 布局方式为横向布局
            ms.setOrientation(LinearLayoutManager.HORIZONTAL);
            //给RecyClerView 添加设置好的布局样式
            signalItemRecycleView.setLayoutManager(ms);
            signalAdapter = new Signal_RecycleViewAdapter(testSignalList,this);
            signalAdapter.setNormalColor(getResources().getColor(R.color.normalColor));
            signalItemRecycleView.setAdapter(signalAdapter);

            if(MainActivity.curPage == MainActivity.signalTab)
            {
                refreshItems(false);
                Log.e(TAG, "1111:创建了信号图的view并刷新");
                startSignalChart(true);
            }

        }
        return view;
    }
    private void initControl(View view){
//        horizontalScrollView = view.findViewById(R.id.signal_item_scroll);
//        testItemListView = view.findViewById(R.id.list);
        signalItemRecycleView = view.findViewById(R.id.signal_item_recycle);
        signalItemRecycleView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        canvasFrame = view.findViewById(R.id.signal_canvas);
        bottom_btnS = view.findViewById(R.id.bottom_btnS);
        realRefreshBtn = view.findViewById(R.id.real_time_refresh);
        initSignalBtn = view.findViewById(R.id.init_signal);
        autoHidCoordBtn = view.findViewById(R.id.auto_hid_coord);
        if(MainActivity.noTouch)
        {
            autoHidCoordBtn.setEnabled(false);
            autoHidCoordBtn.setBackgroundColor(getResources().getColor(R.color.shallow_gray_color));
        }
        testModeBtn = view.findViewById(R.id.test_mode);

         signalCanvasView = new SignalCanvasView(getActivity());
         canvasFrame.addView(signalCanvasView);
//         signalCanvasView.postInvalidate();
    }
    private void listenBottombtn(){
        realRefreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRefresh = !stopRefresh;
                if (stopRefresh)
                {
                    realRefreshBtn.setBackgroundColor(getResources().getColor(R.color.normalColor));
                    ((MainActivity)getActivity()).signal_interface_to_touchManager.refreshSigal(false,null);
                }
                else
                {
                    realRefreshBtn.setBackgroundColor(getResources().getColor(R.color.signal_bottom_true));
                    ((MainActivity)getActivity()).signal_interface_to_touchManager.refreshSigal(true,checkSignalMapList);
                }
            }
        });
        initSignalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initSignalBtn.setBackgroundColor(getResources().getColor(R.color.signal_bottom_true));
                ((MainActivity)getActivity()).signal_interface_to_touchManager.refreshSigal(false,null);
                listenBottombtn = new ListenBottombtn(1);
                listenBottombtn.start();
            }
        });
        autoHidCoordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentStatus = !currentStatus;
                ((MainActivity)getActivity()).signal_interface_to_touchManager.refreshSigal(false,null);
                listenBottombtn = new ListenBottombtn(2);
                listenBottombtn.start();

//                ((MainActivity)getActivity()).signal_interface_to_touchManager.refreshSigal(true,checkSignalMapList);

            }
        });
        testModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enterTest = !enterTest;
                ((MainActivity)getActivity()).signal_interface_to_touchManager.refreshSigal(false,null);
                listenBottombtn = new ListenBottombtn(3);
                listenBottombtn.start();
//                ((MainActivity)getActivity()).signal_interface_to_touchManager.refreshSigal(true,checkSignalMapList);
            }
        });
    }

    @Override
    public void upgradeSignaldata(TouchTestData signalData) {
        SignalData drawData = new SignalData();
        switch (MainActivity.getAppType()) {
            case APP_CLIENT:
                drawData.min = signalData.c_min;
                drawData.max = signalData.c_max;
                break;
            case APP_FACTORY:
                drawData.min = signalData.f_min;
                drawData.max = signalData.f_max;
                break;
            case APP_PCBA:
            case APP_RD:
                drawData.min = signalData.r_min;
                drawData.max = signalData.r_max;
                break;

        }

//        boolean exist = false;
        for(int i = 0;i < signalDataList.size();i++)
        {
            if(signalDataList.get(i).number == signalData.number)
            {
//                exist = true;
                signalDataList.get(i).count = signalData.count;
                signalDataList.get(i).max = drawData.max;
                signalDataList.get(i).min = drawData.min;
                signalDataList.get(i).datas.clear();
                for(int j = 0;j < signalData.count && j < signalData.datas.size();j++)
                {
                    signalDataList.get(i).datas.add(signalData.datas.get(j));
                }
//                Log.e(TAG, "upgradeSignaldata: 添加数据成功");
                signalCanvasView.refreshCancasData(signalDataList);
                break;
            }
        }
//        Log.d(TAG, "upgradeSignaldata: signalData.number = " + signalData.number);
//        Log.d(TAG, "upgradeSignaldata: signalData.count = " + signalData.count);
//        Log.d(TAG, "upgradeSignaldata: signalData.max = " + drawData.max );
//        Log.d(TAG, "upgradeSignaldata: signalData.min = " + drawData.min);
//        Log.d(TAG, "upgradeSignaldata: signalData.data.size() = " + signalData.datas.size());
//        Log.d(TAG, "upgradeSignaldata: signalData.data.max = " + signalData.datas.get(signalData.count - 1));
    }

    public void refreshItems(boolean force)
    {
        if (!force && testSignalList.size() > 0)
            return;
        int mode = getResources().getInteger(R.integer.STE_ALL_ITEMS);
        switch (MainActivity.getAppType()) {
            case APP_CLIENT:
                mode = getResources().getInteger(R.integer.STE_END_USER_GRAPH);
                break;
            case APP_FACTORY:
                mode = getResources().getInteger(R.integer.STE_FACTORY_GRAPH);
                break;
            case APP_RD:
                mode = getResources().getInteger(R.integer.STE_DEV_GRAPH);
                break;
            case APP_PCBA:
                mode = getResources().getInteger(R.integer.STE_PCBA_CUSTOMER_GRAPH);
                break;
        }
        int ret = 0;
        String info = "";
        int maxStandard = 0;
        int minStandard = 0;

        int count = ((MainActivity)getActivity()).signal_interface_to_touchManager.getSignalTestItems(items,items.length,mode);
        if(count <= 0)
            return;


        for (int i = 0; i < count; i++) {
            info = signal_test_items[items[i]];
            Touch_test_standard standard = new Touch_test_standard();
            ret = ((MainActivity)getActivity()).signal_interface_to_touchManager.getSignalTestStandard(items[i],standard, mode);
//            if(ret != 0)
//            {
//                continue;
//            }
            testStandardList.add(standard);
            if(mode == getResources().getInteger(R.integer.STE_END_USER_GRAPH))
            {
                //client
                minStandard = TouchManager.byteToInt(standard.client_min);
                maxStandard = TouchManager.byteToInt(standard.client_max);
            }
            else if(mode == getResources().getInteger(R.integer.STE_FACTORY_GRAPH))
            {
                //factory
                minStandard = TouchManager.byteToInt(standard.factory_min);
                maxStandard = TouchManager.byteToInt(standard.factory_max);
            }
            else if(mode == getResources().getInteger(R.integer.STE_DEV_GRAPH))
            {
                //RD
                minStandard = TouchManager.byteToInt(standard.min);
                maxStandard = TouchManager.byteToInt(standard.max);
            }
            else if(mode == getResources().getInteger(R.integer.STE_PCBA_CUSTOMER_GRAPH))
            {
                //PCBA
                minStandard = TouchManager.byteToInt(standard.min);
                maxStandard = TouchManager.byteToInt(standard.max);
            }
            info += String.format("\n%d  [%d,%d]",TouchManager.byteToInt(items[i]),minStandard,maxStandard);
//            Log.e(TAG, "refreshItems: "+ info );
            testSignalList.add(info);
            ((MainActivity)getActivity()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    signalAdapter.notifyItemInserted(testSignalList.size());
                }
            });

        }

    }
    public void startSignalChart(boolean force)
    {
        ((MainActivity)getActivity()).signal_interface_to_touchManager.setTesting(0);
//        refreshItems(force);
        usbStatus = ((MainActivity)getActivity()).signal_interface_to_touchManager.getCoordsEnabled((byte)usb_channel) == 1 ? true : false;
        serialStatus = ((MainActivity)getActivity()).signal_interface_to_touchManager.getCoordsEnabled((byte)serial_channel) == 1 ? true : false;

        if(currentStatus)
        {
            disableCoords();
        }


        needRestoreStatus = currentStatus;

        if (!stopRefresh) {
            ((MainActivity)getActivity()).signal_interface_to_touchManager.refreshSigal(true,checkSignalMapList);
        }
    }
    public void listenClickSignalItem(View view ,int position){
//        signalAdapter.setOnItemClickListener(new Signal_RecycleViewAdapter.OnItemClickListener() {
//            //实现onItemClick的接口
//            @Override
//            public void onItemClick(final View view, int position) {
                Log.e(TAG, "onItemClick: 选中了" +  signal_test_items[items[position]] + "下标为：" + items[position]);
                TextView tvRecycleViewItemText = (TextView) view.findViewById(R.id.test_item_text);

                int colorCode = getResources().getColor(R.color.normalColor);
                if (tvRecycleViewItemText.getBackground() instanceof ColorDrawable) {
                    ColorDrawable cd = (ColorDrawable) tvRecycleViewItemText.getBackground();
                    colorCode = cd.getColor();
                }

                if (colorCode != getResources().getColor(R.color.normalColor))
                {
                    for(int i = 0;i < checkSignalMapList.size();i++)
                    {
                        if(TouchManager.byteToInt(testStandardList.get(position).no) == checkSignalMapList.get(i).get("signalNum"))
                        {
//                            Log.e(TAG, "onItemClick: 删除下标为："+checkSignalMapList.get(i).get("signalNum"));
                            for(int j = 0;j < signalDataList.size();j++)
                            {
                                if(signalDataList.get(j).number == checkSignalMapList.get(i).get("signalNum"))
                                {
                                    signalDataList.remove(j);
                                    signalCanvasView.refreshCancasData(signalDataList);
                                    break;
                                }
                            }
                            checkSignalMapList.remove(i);
                            tvRecycleViewItemText.setBackgroundColor(getResources().getColor(R.color.normalColor));
                            Log.d(TAG, "onItemClick: 删除项的信息" + tvRecycleViewItemText.getText());
                            checkedCount--;

                            ((MainActivity)getActivity()).signal_interface_to_touchManager.setClickSignalItem(checkSignalMapList);
                            break;
                        }
                    }
                }
                else
                {
                    if(checkedCount == maxChecked)
                    {
                        Toast.makeText(getActivity(),String.format("最多可以同时选择%d项",maxChecked),Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int checkColorNum = 0;
                    boolean exist = false;
                    for(int i = 0;i < signalColorS.length();i++)
                    {
                        exist = false;
                        for(int j = 0;j < checkSignalMapList.size();j++)
                        {
                            if(i == checkSignalMapList.get(j).get("color"))
                            {
                                exist = true;
                                break;
                            }
                        }
                        if(!exist)
                        {
                            checkColorNum = i;
                            break;
                        }
                    }
                    Map<String,Integer> map = new HashMap<>();
                    map.put("color",checkColorNum);
                    map.put("signalNum",TouchManager.byteToInt(testStandardList.get(position).no));
                    map.put("signalStrNum",TouchManager.byteToInt(items[position]));
                    map.put("position",position);
                    checkSignalMapList.add(map);

//                    Log.e(TAG, "onItemClick: checkColorNum = " + checkColorNum );

//                    Log.e(TAG, "onItemClick: 添加选择项,下标为："+ TouchManager.byteToInt(testStandardList.get(position).no));
                    SignalData signalData = new SignalData();
                    signalData.number = TouchManager.byteToInt(testStandardList.get(position).no);
                    signalData.colorNum = checkColorNum;
                    signalData.itemInfo = signal_test_items[items[position]];
                    signalDataList.add(signalData);
//                    Log.e(TAG, "onItemClick: 添加了一个新的项" );
                    tvRecycleViewItemText.setBackgroundColor(signalColorS.getColor(checkColorNum,0));
                    Log.d(TAG, "onItemClick: 增加项的信息" + tvRecycleViewItemText.getText());
                    checkedCount++;
                    ((MainActivity)getActivity()).signal_interface_to_touchManager.setClickSignalItem(checkSignalMapList);
                }

//            }
//        });

    }
    public void disableCoords()
    {
        usbStatus = ((MainActivity)getActivity()).signal_interface_to_touchManager.getCoordsEnabled((byte)usb_channel) == 1 ? true : false;
        serialStatus = ((MainActivity)getActivity()).signal_interface_to_touchManager.getCoordsEnabled((byte)serial_channel) == 1 ? true : false;

        ((MainActivity)getActivity()).signal_interface_to_touchManager.setCoordsEnabled((byte)usb_channel, (byte)0);
        ((MainActivity)getActivity()).signal_interface_to_touchManager.setCoordsEnabled((byte)serial_channel, (byte)0);
        needRestoreStatus = currentStatus;
    }
    public void restoreCoords(){
        ((MainActivity)getActivity()).signal_interface_to_touchManager.setCoordsEnabled((byte)usb_channel, usbStatus?(byte)1:(byte)0);
        ((MainActivity)getActivity()).signal_interface_to_touchManager.setCoordsEnabled((byte)serial_channel, serialStatus?(byte)1:(byte)0);
    }

    @Override
    public void clearSignaldata() {
        Log.e(TAG, "clearSignaldata: 清空数据");
        testStandardList.clear();
//        checkSignalMapList.clear();
//        signalDataList.clear();
        signalCanvasView.refreshCancasData(signalDataList);
        ((MainActivity)getActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {

                for(int i = testSignalList.size()-1;i >= 0;i--)
                {
                    testSignalList.remove(0);
                    signalAdapter.notifyItemRemoved(0);
                    signalAdapter.notifyItemRangeChanged(0, testSignalList.size());
                }
            }
        });


    }

    class ListenBottombtn extends Thread{
        private boolean running = true;
        private int btnNum = 0;
        public ListenBottombtn(int btnNum)
        {
            this.btnNum = btnNum;
        }
        @Override
        public void run() {
            while (running)
            {
                if(TouchManager.signalIsRunning)
                {
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else
                {
                    running = false;
                }
            }
            switch (btnNum)
            {
                case 1: // 信号初始化
                    int ret = ((MainActivity)getActivity()).signal_interface_to_touchManager.signalInit((byte)1);
                    if(ret == 0)
                        Log.e(TAG, "onClick: 信号初始化成功");
                    ((MainActivity)getActivity()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            initSignalBtn.setBackgroundColor(getResources().getColor(R.color.normalColor));
                        }
                    });
                    Log.e(TAG, "onClick: 信号初始化完成并且启动了刷新信号的线程");
                    break;
                case 2: // 自动屏蔽坐标
                    if(currentStatus)
                    {
                        ((MainActivity)getActivity()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                autoHidCoordBtn.setBackgroundColor(getResources().getColor(R.color.signal_bottom_true));
                            }
                        });

                        disableCoords();
                    }
                    else if(needRestoreStatus)
                    {
                        ((MainActivity)getActivity()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                autoHidCoordBtn.setBackgroundColor(getResources().getColor(R.color.normalColor));
                            }
                        });
                        restoreCoords();
                    }
                    break;
                case 3: //测试模式
                    if(enterTest)
                    {
                        ((MainActivity)getActivity()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                testModeBtn.setBackgroundColor(getResources().getColor(R.color.signal_bottom_true));
                            }
                        });

                        ((MainActivity)getActivity()).signal_interface_to_touchManager.setTesting(1);
                    }
                    else
                    {
                        ((MainActivity)getActivity()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                testModeBtn.setBackgroundColor(getResources().getColor(R.color.normalColor));
                            }
                        });

                        ((MainActivity)getActivity()).signal_interface_to_touchManager.setTesting(0);
                    }
                    break;
                default:
                    return;
            }
            ((MainActivity)getActivity()).signal_interface_to_touchManager.refreshSigal(true,checkSignalMapList);
        }
    }

    public void restoreCoordsOrNot() {
        if (needRestoreStatus) {
            restoreCoords();
        }
    }
    @Override
    public void onResume() {
//        if(MainActivity.curPage == MainActivity.signalTab)
//        refreshItems(false);
//        startSignalChart(false);
//        Log.e(TAG, "onResume: 刷新信号图");
        super.onResume();
    }

    @Override
    public void onPause() {

//        touchManager.refreshSigal(false,null);
//        Log.e(TAG, "onPause: 暂停信号图刷新");
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
//        outState.putCharSequence();
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
//        Log.e(TAG, "onCreateView: signal stop" );
        super.onStop();
    }

    @Override
    public void onDestroy() {
//        Log.e(TAG, "onCreateView: signal onDestroy" );
        super.onDestroy();
    }
}
