package fragment_package;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.touch.MainActivity;
import com.example.touch.MyCailbrateManager;
import com.example.touch.MyFileManager;
import com.example.touch.MySaveFileManager;
import com.example.touch.R;
import com.example.touch.TouchManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dataInformation.CalibrationData;
import dataInformation.CalibrationSettings;
import fragment_interface.Setting_fragment_interface;


public class Setting_fragment extends Fragment implements Setting_fragment_interface {

    private final String TAG = "myText";
    private int READ_FILE_RESULT_CODE = 2;
    private int RECALIBRATE_FILE_RESULT_CODE = 3;
    public static boolean readFileFlag = false;
    public static View view;
    private RelativeLayout setting_all_ctrl;
    private RelativeLayout coordRelativeLayout;
    private CheckBox usbCheckBox;
    private RadioGroup USB_radio_group;
    private CheckBox UARTCheckBox;
    private RadioButton mouseRadioBtn;
    private RadioButton multiRadioBtn;
    private CheckBox XRollover;
    private CheckBox YRollover;
    private Button XYRestoreFactory;
    private RadioGroup setting_touch_group;
    private RadioButton touch_rollover_0;
    private RadioButton touch_rollover_90;
    private RadioButton touch_rollover_180;
    private RadioButton touch_rollover_270;
    private Button touch_restore_factory;
    private RadioGroup setting_turn_screen_clockwise_group;
    private RadioButton screen_rollover_0;
    private RadioButton screen_rollover_90;
    private RadioButton screen_rollover_180;
    private RadioButton screen_rollover_270;
    private Button screen_restore_factory;
    private RadioGroup setting_MAC_OS_group;
    private RadioButton low_MAC_OS;
    private RadioButton high_MAC_OS;
    private Button MAC_restore_factory;
    private Button recalibrate;
    private Button refresh;
    private Button setting;
    private Button save;
    private Button read;
    private Button hid_data;
    private Button recalibrate_restory_factory;
    private EditText editText_target_x0;
    private EditText editText_target_x1;
    private EditText editText_target_x2;
    private EditText editText_target_x3;
    private EditText editText_target_y0;
    private EditText editText_target_y1;
    private EditText editText_target_y2;
    private EditText editText_target_y3;
    private EditText editText_collect_x0;
    private EditText editText_collect_x1;
    private EditText editText_collect_x2;
    private EditText editText_collect_x3;
    private EditText editText_collect_y0;
    private EditText editText_collect_y1;
    private EditText editText_collect_y2;
    private EditText editText_collect_y3;

    private RelativeLayout hid_data_layout;
    //原出厂设置的值
    private byte[] XYMirrorValue = new byte[2];
    private byte touchRolloverValue = 0 ;
    private byte screenRolloverValue = 0 ;
    private byte macosValue = 0;
    private List<byte[]> pointValue = new ArrayList<>();
    private boolean hid_data_flag;
    String saveFilePath = "";

    private int SAVE_FILE_RESULT_CODE = 1;
    Gson gson ;
    private Refreshinfo refreshinfo;
    public static boolean waitRefresh = false;
    public static boolean calibrateFinish = false;

    public static boolean enterCalibrate = false;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if(view == null)
        {
            Log.d(TAG, "onCreateView: 缓存settingPage");
            ((MainActivity)getActivity()).setSetting_fragment_interface(this);
            view = inflater.inflate(R.layout.setting_fragment,container,false);
            gson = new Gson();
            initSettingFragment();
            listeningControl();
            hid_data_flag = false;
            refreshinfo = new Refreshinfo();
            refreshinfo.start();

        }

        return view;
    }
    public void initSettingFragment(){
        setting_all_ctrl = view.findViewById(R.id.setting_all_ctrl);
        //usb坐标
        coordRelativeLayout = view.findViewById(R.id.coordID);
        usbCheckBox = view.findViewById(R.id.USB_check);
        USB_radio_group = view.findViewById(R.id.USB_radio_group);
        mouseRadioBtn = view.findViewById(R.id.setting_mouse_radio_button);
        multiRadioBtn = view.findViewById(R.id.setting_multi_touch);
        //uart坐标模式
        UARTCheckBox = view.findViewById(R.id.setting_UART_check);
        //触摸框翻转
        XRollover = view.findViewById(R.id.setting_x_rollover);
        YRollover = view.findViewById(R.id.setting_y_rollover);
        XYRestoreFactory = view.findViewById(R.id.setting_XY_restore_factory);
        //触摸框顺时针旋转
        setting_touch_group = view.findViewById(R.id.setting_touch_group);
        touch_rollover_0 = view.findViewById(R.id.setting_touch_0);
        touch_rollover_90 = view.findViewById(R.id.setting_touch_90);
        touch_rollover_180 = view.findViewById(R.id.setting_touch_180);
        touch_rollover_270 = view.findViewById(R.id.setting_touch_270);
        touch_restore_factory = view.findViewById(R.id.setting_restore_factory_touch);
        //屏幕顺时针旋转
        setting_turn_screen_clockwise_group = view.findViewById(R.id.setting_turn_screen_clockwise_group);
        screen_rollover_0 = view.findViewById(R.id.setting_screen_0);
        screen_rollover_90 = view.findViewById(R.id.setting_screen_90);
        screen_rollover_180 = view.findViewById(R.id.setting_screen_180);
        screen_rollover_270 = view.findViewById(R.id.setting_screen_270);
        screen_restore_factory = view.findViewById(R.id.setting_restore_factory_screen);
        //MAC_OS
        setting_MAC_OS_group = view.findViewById(R.id.setting_MAC_OS_group);
        low_MAC_OS = view.findViewById(R.id.setting_MAC_OS_10_9_following);
        high_MAC_OS = view.findViewById(R.id.setting_MAC_OS_10_9_above);
        MAC_restore_factory = view.findViewById(R.id.setting_restore_factory_setting_MAC_OS);
        //校准按钮
        recalibrate = view.findViewById(R.id.setting_recalibrate);
        refresh = view.findViewById(R.id.setting_refresh);
        setting = view.findViewById(R.id.setting_setting);
        save = view.findViewById(R.id.setting_save);
        read = view.findViewById(R.id.setting_read);
        hid_data = view.findViewById(R.id.setting_hid_data);
        recalibrate_restory_factory = view.findViewById(R.id.setting_recalibrate_restore_factory);
        //采集数据
        editText_target_x0 = view.findViewById(R.id.setting_editText_target_x0);
        editText_target_x1 = view.findViewById(R.id.setting_editText_target_x1);
        editText_target_x2 = view.findViewById(R.id.setting_editText_target_x2);
        editText_target_x3 = view.findViewById(R.id.setting_editText_target_x3);
        editText_target_y0 = view.findViewById(R.id.setting_editText_target_Y0);
        editText_target_y1 = view.findViewById(R.id.setting_editText_target_Y1);
        editText_target_y2 = view.findViewById(R.id.setting_editText_target_Y2);
        editText_target_y3 = view.findViewById(R.id.setting_editText_target_Y3);
        editText_collect_x0 = view.findViewById(R.id.setting_editText_collect_x0);
        editText_collect_x1 = view.findViewById(R.id.setting_editText_collect_X1);
        editText_collect_x2 = view.findViewById(R.id.setting_editText_collect_X2);
        editText_collect_x3 = view.findViewById(R.id.setting_editText_collect_X3);
        editText_collect_y0 = view.findViewById(R.id.setting_editText_collect_Y0);
        editText_collect_y1 = view.findViewById(R.id.setting_editText_collect_Y1);
        editText_collect_y2 = view.findViewById(R.id.setting_editText_collect_Y2);
        editText_collect_y3 = view.findViewById(R.id.setting_editText_collect_Y3);
        hid_data_layout = view.findViewById(R.id.setting_hid_data_point);
    }
    public void listeningControl(){
        usbCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            if(((MainActivity)getActivity()).setting_interface == null)
                return;
            if(compoundButton.isPressed())//防止在通过代码设置时也触发监听机制
            {
                if(compoundButton.isChecked())
                {
                    ((MainActivity)getActivity()).setting_interface.setCoordsEnabled((byte)(getResources().getInteger(R.integer.COORDS_CHANNEL_USB) & 0xff),(byte)1);
                    Log.e(TAG, "onCheckedChanged: USB通道打开");
                    for(int i = 0;i < USB_radio_group.getChildCount();i++)
                    {
                        USB_radio_group.getChildAt(i).setEnabled(true);
                        USB_radio_group.setBackgroundColor(getResources().getColor(R.color.white_color));
                    }
                    byte mode = ((MainActivity)getActivity()).setting_interface.getCoordsMode((byte)(getResources().getInteger(R.integer.COORDS_CHANNEL_USB) & 0xff));
                    if(mode == 1)
                    {
                        mouseRadioBtn.setChecked(true);
                    }
                    else
                    {
                        multiRadioBtn.setChecked(true);
                    }

                }
                else
                {
                    Log.e(TAG, "onCheckedChanged: USB通道关闭");
                    ((MainActivity)getActivity()).setting_interface.setCoordsEnabled((byte)(getResources().getInteger(R.integer.COORDS_CHANNEL_USB) & 0xff),(byte)0);
                    USB_radio_group.setBackgroundColor(getResources().getColor(R.color.shallow_gray_color));
                    for(int i = 0;i < USB_radio_group.getChildCount();i++)
                    {
                        USB_radio_group.getChildAt(i).setEnabled(false);
                    }
                }
            }
        }
        });

        UARTCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isPressed()){
                    if(compoundButton.isChecked())
                    {
                        ((MainActivity)getActivity()).setting_interface.setCoordsEnabled((byte)(getResources().getInteger(R.integer.COORDS_CHANNEL_SERIAL) & 0xff),(byte)1);
                    }
                    else
                    {
                        ((MainActivity)getActivity()).setting_interface.setCoordsEnabled((byte)(getResources().getInteger(R.integer.COORDS_CHANNEL_SERIAL) & 0xff),(byte)0);
                    }
                }

            }
        });
        USB_radio_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
//                if(radioGroup.isPressed())
//                {
                    if(i == R.id.setting_mouse_radio_button)
                    {
                        Log.d(TAG, "onCheckedChanged: 设置为鼠标模式");
                        ((MainActivity)getActivity()).setting_interface.setCoordsMode((byte)(getResources().getInteger(R.integer.COORDS_CHANNEL_USB) & 0xff),
                                (byte)(getResources().getInteger(R.integer.COORDS_USB_MODE_MOUSE) & 0xff));
                    }
                    else if(i == R.id.setting_multi_touch)
                    {
                        Log.d(TAG, "onCheckedChanged: 设置为多点触摸");
                        ((MainActivity)getActivity()).setting_interface.setCoordsMode((byte)(getResources().getInteger(R.integer.COORDS_CHANNEL_USB) & 0xff),
                                (byte)(getResources().getInteger(R.integer.COORDS_USB_MODE_TOUCH) & 0xff));
                    }

//                }

            }
        });
        XRollover.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isPressed()){
                    byte[]buffer = new byte[2];
                    if(compoundButton.isChecked())
                    {
                        byte[] current = new byte[2];
                        byte[] def = new byte[2];
                        ((MainActivity)getActivity()).setting_interface.getMirror(current,def);
                        buffer[0] = 1;  //X轴翻转
                        buffer[1] = current[1];//Y轴当前的值不变
                        ((MainActivity)getActivity()).setting_interface.setMirror(buffer);
                    }
                    else
                    {
                        byte[] current = new byte[2];
                        byte[] def = new byte[2];
                        ((MainActivity)getActivity()).setting_interface.getMirror(current,def);
                        buffer[0] = 0;  //X轴不翻转
                        buffer[1] = current[1];//Y轴当前的值不变
                        ((MainActivity)getActivity()).setting_interface.setMirror(buffer);
                    }
                }

            }
        });
        YRollover.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isPressed()){
                    byte[]buffer = new byte[2];
                    byte[] current = new byte[2];
                    byte[] def = new byte[2];
                    int ret = ((MainActivity)getActivity()).setting_interface.getMirror(current,def);
                    if(ret == 0)
                    {
                        XYMirrorValue[0] = def[0];
                        XYMirrorValue[1] = def[1];
                    }
                    if(compoundButton.isChecked())
                    {
                        buffer[0] = current[0];  //X轴翻转保持不变
                        buffer[1] = 1;//Y轴翻转
                        ((MainActivity)getActivity()).setting_interface.setMirror(buffer);
                    }
                    else
                    {
                        buffer[0] = current[0];  //X轴翻转保持不变
                        buffer[1] = 0;//Y轴不翻转
                        ((MainActivity)getActivity()).setting_interface.setMirror(buffer);
                    }
                }

            }
        });
        XYRestoreFactory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                XYRestoreFactory.setBackgroundColor(getResources().getColor(R.color.blue_color));
                byte[] current = new byte[2];
                byte[] def = new byte[2];
                ((MainActivity)getActivity()).setting_interface.getMirror(current,def);
                ((MainActivity)getActivity()).setting_interface.setMirror(def);

                ((MainActivity)getActivity()).setting_interface.getMirror(current,def);
                if(current[0] == 0)
                    XRollover.setChecked(false);
                else if(current[0] == 1)
                    XRollover.setChecked(true);
                if(current[1] == 0)
                    YRollover.setChecked(false);
                else if(current[1] == 1)
                    YRollover.setChecked(true);
                XYRestoreFactory.setBackgroundColor(getResources().getColor(R.color.normalColor));
                Toast.makeText((MainActivity)getActivity(),"触摸框翻转恢复出厂完成",Toast.LENGTH_SHORT).show();
            }
        });
        setting_touch_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                byte[] buffer = new byte[2];
                byte[] current = new byte[2];
                byte[] def = new byte[2];
                int ret = ((MainActivity)getActivity()).setting_interface.getRotation(current,def);
                if(ret == 0)
                {
                    touchRolloverValue = def[0];
                    screenRolloverValue = def[1];
                }
                buffer[1] = current[1];
                if(i == R.id.setting_touch_0)
                {
                    buffer[0] = 0;
                    ((MainActivity)getActivity()).setting_interface.setRotation(buffer);
                }
                else if(i == R.id.setting_touch_90)
                {
                    buffer[0] = 1;
                    ((MainActivity)getActivity()).setting_interface.setRotation(buffer);
                }
                else if(i == R.id.setting_touch_180)
                {
                    buffer[0] = 2;
                    ((MainActivity)getActivity()).setting_interface.setRotation(buffer);
                }
                else if(i == R.id.setting_touch_270)
                {
                    buffer[0] = 3;
                    ((MainActivity)getActivity()).setting_interface.setRotation(buffer);
                }
            }
        });
        touch_restore_factory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                touch_restore_factory.setBackgroundColor(getResources().getColor(R.color.blue_color));
                byte[] current = new byte[2];
                byte[] def = new byte[2];
                int ret = ((MainActivity)getActivity()).setting_interface.getRotation(current,def);
                byte[] buffer = new byte[2];
                buffer[0] = def[0];
                buffer[1] = current[1];
                ((MainActivity)getActivity()).setting_interface.setRotation(buffer);

                ((MainActivity)getActivity()).setting_interface.getRotation(current,def);
                switch (current[0])
                {
                    case 0:
                        touch_rollover_0.setChecked(true);
                        break;
                    case 1:
                        touch_rollover_90.setChecked(true);
                        break;
                    case 2:
                        touch_rollover_180.setChecked(true);
                        break;
                    case 3:
                        touch_rollover_270.setChecked(true);
                        break;
                }
                touch_restore_factory.setBackgroundColor(getResources().getColor(R.color.normalColor));
                Toast.makeText((MainActivity)getActivity(),"触摸框旋转恢复出厂完成",Toast.LENGTH_SHORT).show();
            }
        });
        setting_turn_screen_clockwise_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                byte[] buffer = new byte[2];
                byte[] current = new byte[2];
                byte[] def = new byte[2];
                int ret = ((MainActivity)getActivity()).setting_interface.getRotation(current,def);
                if(ret == 0)
                {
                    touchRolloverValue = def[0];
                    screenRolloverValue = def[1];
                }
                buffer[0] = current[0];
                if(i == R.id.setting_screen_0)
                {
                    buffer[1] = 0;
                    ((MainActivity)getActivity()).setting_interface.setRotation(buffer);
                }
                else if(i == R.id.setting_screen_90)
                {
                    buffer[1] = 1;
                    ((MainActivity)getActivity()).setting_interface.setRotation(buffer);
                }
                else if(i == R.id.setting_screen_180)
                {
                    buffer[1] = 2;
                    ((MainActivity)getActivity()).setting_interface.setRotation(buffer);
                }
                else if(i == R.id.setting_screen_270)
                {
                    buffer[1] = 3;
                    ((MainActivity)getActivity()).setting_interface.setRotation(buffer);
                }
            }
        });
        screen_restore_factory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                screen_restore_factory.setBackgroundColor(getResources().getColor(R.color.blue_color));
                byte[] current = new byte[2];
                byte[] def = new byte[2];
                int ret = ((MainActivity)getActivity()).setting_interface.getRotation(current,def);
                byte[] buffer = new byte[2];
                buffer[0] = current[0];
                buffer[1] = def[1];
                ((MainActivity)getActivity()).setting_interface.setRotation(buffer);

                ((MainActivity)getActivity()).setting_interface.getRotation(current,def);
                switch (current[1])
                {
                    case 0:
                        screen_rollover_0.setChecked(true);
                        break;
                    case 1:
                        screen_rollover_90.setChecked(true);
                        break;
                    case 2:
                        screen_rollover_180.setChecked(true);
                        break;
                    case 3:
                        screen_rollover_270.setChecked(true);
                        break;
                }
                screen_restore_factory.setBackgroundColor(getResources().getColor(R.color.normalColor));
                Toast.makeText((MainActivity)getActivity(),"屏幕旋转恢复出厂完成",Toast.LENGTH_SHORT).show();
            }
        });
        setting_MAC_OS_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {

                if(i == R.id.setting_MAC_OS_10_9_following)
                {
                    byte mode = 1;
                    ((MainActivity)getActivity()).setting_interface.setMacOSMode(mode);
                }
                else if(i == R.id.setting_MAC_OS_10_9_above)
                {
                    byte mode = 2;
                    ((MainActivity)getActivity()).setting_interface.setMacOSMode(mode);
                }
            }
        });
        MAC_restore_factory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MAC_restore_factory.setBackgroundColor(getResources().getColor(R.color.blue_color));
                byte[] current = new byte[2];
                byte[] def = new byte[2];
                int ret = ((MainActivity)getActivity()).setting_interface.getMacOSMode(current,def);
                ((MainActivity)getActivity()).setting_interface.setMacOSMode(def[0]);

                ((MainActivity)getActivity()).setting_interface.getMacOSMode(current,def);
                if(current[0] == 1)
                {
                    low_MAC_OS.setChecked(true);
                }
                else if(current[0] == 2)
                {
                    high_MAC_OS.setChecked(true);
                }
                MAC_restore_factory.setBackgroundColor(getResources().getColor(R.color.normalColor));
                Toast.makeText((MainActivity)getActivity(),"MAC OS恢复出厂完成",Toast.LENGTH_SHORT).show();
            }
        });
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh.setBackgroundColor(getResources().getColor(R.color.blue_color));
                refreshCalibrationData(1);
                refresh.setBackgroundColor(getResources().getColor(R.color.normalColor));
                Toast.makeText((MainActivity)getActivity(),"校准信息刷新成功",Toast.LENGTH_SHORT).show();

            }
        });
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setting.setBackgroundColor(getResources().getColor(R.color.blue_color));
                setCalibrateData();
                setting.setBackgroundColor(getResources().getColor(R.color.normalColor));
                Toast.makeText((MainActivity)getActivity(),"校准信息设置成功",Toast.LENGTH_SHORT).show();
            }
        });
        hid_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hid_data.setBackgroundColor(getResources().getColor(R.color.blue_color));
                if(!hid_data_flag)
                {
                    hid_data_flag = true;
                    hid_data.setText(getResources().getString(R.string.show_data));
                    hid_data_layout.setVisibility(view.GONE);
                }
                else
                {
                    hid_data_flag = false;
                    hid_data.setText(getResources().getString(R.string.hid_data));
                    hid_data_layout.setVisibility(View.VISIBLE);
                }
                hid_data.setBackgroundColor(getResources().getColor(R.color.normalColor));
            }
        });
        recalibrate_restory_factory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recalibrate_restory_factory.setBackgroundColor(getResources().getColor(R.color.blue_color));
                refreshCalibrationData(2);
                setCalibrateData();
                recalibrate_restory_factory.setBackgroundColor(getResources().getColor(R.color.normalColor));
                Toast.makeText((MainActivity)getActivity(),"恢复校准数据成功",Toast.LENGTH_SHORT).show();
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save.setBackgroundColor(getResources().getColor(R.color.blue_color));
                save.setBackgroundColor(getResources().getColor(R.color.normalColor));
                if(getActivity() != null){
                    Intent intent = new Intent(getActivity(), MySaveFileManager.class);
                    startActivityForResult(intent, SAVE_FILE_RESULT_CODE);
                }
            }
        });
        read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                read.setBackgroundColor(getResources().getColor(R.color.blue_color));
                read.setBackgroundColor(getResources().getColor(R.color.normalColor));
                if(getActivity() != null)
                {
                    readFileFlag = true;
                    Intent intent = new Intent(getActivity(), MyFileManager.class);
                    startActivityForResult(intent, READ_FILE_RESULT_CODE);
                }
            }
        });
        recalibrate.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onClick(View view) {
                recalibrate.setBackgroundColor(getResources().getColor(R.color.blue_color));
                recalibrate.setBackgroundColor(getResources().getColor(R.color.normalColor));
                if(getActivity() != null)
                {
                    enterCalibrate = true;
                    TouchManager.firstTimeCalibrate = true;
                    Intent intent=new Intent(getActivity(), MyCailbrateManager.class);
                    startActivity(intent);
                }
            }
        });

    }

    private void getEditTextValue(int index,CalibrationData data)
    {
        switch (index){
            case 0:
                data.targetX = Integer.valueOf(editText_target_x0.getText().toString());
                data.targetY = Integer.valueOf(editText_target_y0.getText().toString());
                data.collectX = Integer.valueOf(editText_collect_x0.getText().toString());
                data.collectY = Integer.valueOf(editText_collect_y0.getText().toString());
                break;
            case 1:
                data.targetX = Integer.valueOf(editText_target_x1.getText().toString());
                data.targetY = Integer.valueOf(editText_target_y1.getText().toString());
                data.collectX = Integer.valueOf(editText_collect_x1.getText().toString());
                data.collectY = Integer.valueOf(editText_collect_y1.getText().toString());
                break;
            case 2:
                data.targetX = Integer.valueOf(editText_target_x2.getText().toString());
                data.targetY = Integer.valueOf(editText_target_y2.getText().toString());
                data.collectX = Integer.valueOf(editText_collect_x2.getText().toString());
                data.collectY = Integer.valueOf(editText_collect_y2.getText().toString());
                break;
            case 3:
                data.targetX = Integer.valueOf(editText_target_x3.getText().toString());
                data.targetY = Integer.valueOf(editText_target_y3.getText().toString());
                data.collectX = Integer.valueOf(editText_collect_x3.getText().toString());
                data.collectY = Integer.valueOf(editText_collect_y3.getText().toString());
                break;
            default:
                data.targetX = -1;
                data.targetY = -1;
                data.collectX =-1;
                data.collectY =-1;
                break;
        }
    }

    @Override
    public void setEnable(boolean enable) {
        Log.e(TAG, "setEnable: ........");
        if(view == null)
            return;
        //usb坐标
        if(usbCheckBox == null)
            return;
        ((MainActivity)getActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!MainActivity.noTouch)
                {
                    if(!enable )
                    {
                        usbCheckBox.setChecked(false);
                        UARTCheckBox.setChecked(false);
                        USB_radio_group.setBackgroundColor(getResources().getColor(R.color.shallow_gray_color));
                    }
                    else {
                        USB_radio_group.setBackgroundColor(getResources().getColor(R.color.white_color));
                    }

                    usbCheckBox.setEnabled(enable);
                    for(int i = 0;i < USB_radio_group.getChildCount();i++)
                    {
                        USB_radio_group.getChildAt(i).setEnabled(enable);
                    }
                    //uart坐标模式
                    UARTCheckBox.setEnabled(enable);
                }
                else
                {
                    coordRelativeLayout.setBackgroundColor(getResources().getColor(R.color.shallow_gray_color));
                    usbCheckBox.setEnabled(false);
                    USB_radio_group.setBackgroundColor(getResources().getColor(R.color.shallow_gray_color));
                    for(int i = 0;i < USB_radio_group.getChildCount();i++)
                    {
                        USB_radio_group.getChildAt(i).setEnabled(false);
                    }
                    UARTCheckBox.setEnabled(false);
//                    coordRelativeLayout.setVisibility(View.VISIBLE);
                }




                //触摸框翻转
                XRollover.setEnabled(enable);
                YRollover.setEnabled(enable);
                XYRestoreFactory.setEnabled(enable);
                //触摸框顺时针旋转
//                setting_touch_group.clearCheck();
                for(int i = 0;i < setting_touch_group.getChildCount();i++)
                {
                    setting_touch_group.getChildAt(i).setEnabled(enable);
                }
                touch_restore_factory.setEnabled(enable);
                //屏幕顺时针旋转
//                setting_turn_screen_clockwise_group.clearCheck();
                for(int i = 0;i < setting_turn_screen_clockwise_group.getChildCount();i++)
                {
                    setting_turn_screen_clockwise_group.getChildAt(i).setEnabled(enable);
                }
                screen_restore_factory.setEnabled(enable);
                //MAC_OS
//                setting_MAC_OS_group.clearCheck();
                for(int i = 0;i < setting_MAC_OS_group.getChildCount();i++)
                {
                    setting_MAC_OS_group.getChildAt(i).setEnabled(enable);
                }
                MAC_restore_factory.setEnabled(enable);
                //校准按钮
                recalibrate.setEnabled(enable);
                refresh.setEnabled(enable);
                setting.setEnabled(enable);
                save.setEnabled(enable);
                read.setEnabled(enable);
                hid_data.setEnabled(enable);
                recalibrate_restory_factory.setEnabled(enable);
                //采集数据
                editText_target_x0.setEnabled(enable);
                editText_target_x1.setEnabled(enable);
                editText_target_x2.setEnabled(enable);
                editText_target_x3.setEnabled(enable);
                editText_target_y0.setEnabled(enable);
                editText_target_y1.setEnabled(enable);
                editText_target_y2.setEnabled(enable);
                editText_target_y3.setEnabled(enable);
                editText_collect_x0.setEnabled(enable);
                editText_collect_x1.setEnabled(enable);
                editText_collect_x2.setEnabled(enable);
                editText_collect_x3.setEnabled(enable);
                editText_collect_y0.setEnabled(enable);
                editText_collect_y1.setEnabled(enable);
                editText_collect_y2.setEnabled(enable);
                editText_collect_y3.setEnabled(enable);
                if(!enable)
                {
//            editText_target_x0.setText("" + 0);
//            editText_target_x1.setText("" + 0);
//            editText_target_x2.setText("" + 0);
//            editText_target_x3.setText("" + 0);
//            editText_target_y0.setText("" + 0);
//            editText_target_y1.setText("" + 0);
//            editText_target_y2.setText("" + 0);
//            editText_target_y3.setText("" + 0);
//            editText_collect_x0.setText("" + 0);
//            editText_collect_x1.setText("" + 0);
//            editText_collect_x2.setText("" + 0);
//            editText_collect_x3.setText("" + 0);
//            editText_collect_y0.setText("" + 0);
//            editText_collect_y1.setText("" + 0);
//            editText_collect_y2.setText("" + 0);
//            editText_collect_y3.setText("" + 0);
                }
                if(!enable)
                {
                    setting_all_ctrl.setBackgroundColor(getResources().getColor(R.color.shallow_gray_color));
                    editText_target_x0.setBackground(getResources().getDrawable(R.drawable.shallow_img_board_rect));
                    editText_target_x1.setBackground(getResources().getDrawable(R.drawable.shallow_img_board_rect));
                    editText_target_x2.setBackground(getResources().getDrawable(R.drawable.shallow_img_board_rect));
                    editText_target_x3.setBackground(getResources().getDrawable(R.drawable.shallow_img_board_rect));
                    editText_target_y0.setBackground(getResources().getDrawable(R.drawable.shallow_img_board_rect));
                    editText_target_y1.setBackground(getResources().getDrawable(R.drawable.shallow_img_board_rect));
                    editText_target_y2.setBackground(getResources().getDrawable(R.drawable.shallow_img_board_rect));
                    editText_target_y3.setBackground(getResources().getDrawable(R.drawable.shallow_img_board_rect));
                    editText_collect_x0.setBackground(getResources().getDrawable(R.drawable.shallow_img_board_rect));
                    editText_collect_x1.setBackground(getResources().getDrawable(R.drawable.shallow_img_board_rect));
                    editText_collect_x2.setBackground(getResources().getDrawable(R.drawable.shallow_img_board_rect));
                    editText_collect_x3.setBackground(getResources().getDrawable(R.drawable.shallow_img_board_rect));
                    editText_collect_y0.setBackground(getResources().getDrawable(R.drawable.shallow_img_board_rect));
                    editText_collect_y1.setBackground(getResources().getDrawable(R.drawable.shallow_img_board_rect));
                    editText_collect_y2.setBackground(getResources().getDrawable(R.drawable.shallow_img_board_rect));
                    editText_collect_y3.setBackground(getResources().getDrawable(R.drawable.shallow_img_board_rect));
                }
                else
                {
                    setting_all_ctrl.setBackgroundColor(getResources().getColor(R.color.white_color));
                    editText_target_x0.setBackground(getResources().getDrawable(R.drawable.img_board_rect));
                    editText_target_x1.setBackground(getResources().getDrawable(R.drawable.img_board_rect));
                    editText_target_x2.setBackground(getResources().getDrawable(R.drawable.img_board_rect));
                    editText_target_x3.setBackground(getResources().getDrawable(R.drawable.img_board_rect));
                    editText_target_y0.setBackground(getResources().getDrawable(R.drawable.img_board_rect));
                    editText_target_y1.setBackground(getResources().getDrawable(R.drawable.img_board_rect));
                    editText_target_y2.setBackground(getResources().getDrawable(R.drawable.img_board_rect));
                    editText_target_y3.setBackground(getResources().getDrawable(R.drawable.img_board_rect));
                    editText_collect_x0.setBackground(getResources().getDrawable(R.drawable.img_board_rect));
                    editText_collect_x1.setBackground(getResources().getDrawable(R.drawable.img_board_rect));
                    editText_collect_x2.setBackground(getResources().getDrawable(R.drawable.img_board_rect));
                    editText_collect_x3.setBackground(getResources().getDrawable(R.drawable.img_board_rect));
                    editText_collect_y0.setBackground(getResources().getDrawable(R.drawable.img_board_rect));
                    editText_collect_y1.setBackground(getResources().getDrawable(R.drawable.img_board_rect));
                    editText_collect_y2.setBackground(getResources().getDrawable(R.drawable.img_board_rect));
                    editText_collect_y3.setBackground(getResources().getDrawable(R.drawable.img_board_rect));
                }
            }
        });


    }
    public void refreshAllUiInfo(Map<String,Byte> map,List<CalibrationData> dataList)
    {
        if(map == null)
        {
            setEnable(false);
            return;
        }

//        Log.e(TAG, "setting: 刷新设置界面所有信息");
//        Log.e(TAG, "setting: map.get(usbEnable) = " + map.get("usbEnable"));
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!MainActivity.noTouch)
                {
                    if(map.get("usbEnable") >= 0)
                    {
                        if(map.get("usbEnable") == 1)
                        {
                            usbCheckBox.setChecked(true);
                            if(map.get("usbMode") == 1)
                            {
                                mouseRadioBtn.setChecked(true);
                            }
                            else if(map.get("usbMode") == 2)
                            {
                                multiRadioBtn.setChecked(true);
                            }
                        }
                        else if(map.get("usbEnable") == 0)
                        {
                            usbCheckBox.setChecked(false);
                            USB_radio_group.clearCheck();
                            USB_radio_group.setBackgroundColor(getResources().getColor(R.color.shallow_gray_color));
                        }
                    }
                    if(map.get("uartEnable") == 1)
                    {
                        UARTCheckBox.setChecked(true);
                    }
                }

                switch (map.get("touchRotation"))
                {
                    case 0:
                        touch_rollover_0.setChecked(true);
                        break;
                    case 1:
                        touch_rollover_90.setChecked(true);
                        break;
                    case 2:
                        touch_rollover_180.setChecked(true);
                        break;
                    case 3:
                        touch_rollover_270.setChecked(true);
                        break;
                }
                switch (map.get("screenRotation"))
                {
                    case 0:
                        screen_rollover_0.setChecked(true);
                        break;
                    case 1:
                        screen_rollover_90.setChecked(true);
                        break;
                    case 2:
                        screen_rollover_180.setChecked(true);
                        break;
                    case 3:
                        screen_rollover_270.setChecked(true);
                        break;
                }
                if(map.get("xMirror") == 1)
                {
                    XRollover.setChecked(true);
                }
                if(map.get("yMirror") == 1)
                {
                    XRollover.setChecked(true);
                }
                if(map.get("mac") == 1)
                {
                    low_MAC_OS.setChecked(true);
                }
                else if(map.get("mac") == 2)
                {
                    high_MAC_OS.setChecked(true);
                }

                for(int i = 0; i < dataList.size();i++)
                {
                    setEditText(i,dataList.get(i));
                }
            }
        });


    }

    @Override
    public void refreshSettings() {
        if(view == null)
            return;
        Log.e(TAG, "setting: 刷新设置界面");
        Map<String,Byte> map = new HashMap<>();
        if(getActivity() == null && ((MainActivity)getActivity()).setting_interface == null)
            return;
        map = ((MainActivity)getActivity()).setting_interface.getSettingsInfos();
        if(map == null)
            return;
        if(map.get("usbEnable") >= 0)
        {
            if(map.get("usbEnable") == 1)
            {
                usbCheckBox.setChecked(true);
                if(map.get("usbMode") == 1)
                {
                    mouseRadioBtn.setChecked(true);
                }
                else if(map.get("usbMode") == 2)
                {
                    multiRadioBtn.setChecked(true);
                }
            }
            else if(map.get("usbEnable") == 0)
            {
                usbCheckBox.setChecked(false);
                USB_radio_group.clearCheck();
                USB_radio_group.setBackgroundColor(getResources().getColor(R.color.shallow_gray_color));
            }
        }
        if(map.get("uartEnable") == 1)
        {
            UARTCheckBox.setChecked(true);
        }
        switch (map.get("touchRotation"))
        {
            case 0:
                touch_rollover_0.setChecked(true);
                break;
            case 1:
                touch_rollover_90.setChecked(true);
                break;
            case 2:
                touch_rollover_180.setChecked(true);
                break;
            case 3:
                touch_rollover_270.setChecked(true);
                break;
        }
        switch (map.get("screenRotation"))
        {
            case 0:
                screen_rollover_0.setChecked(true);
                break;
            case 1:
                screen_rollover_90.setChecked(true);
                break;
            case 2:
                screen_rollover_180.setChecked(true);
                break;
            case 3:
                screen_rollover_270.setChecked(true);
                break;
        }
        if(map.get("xMirror") == 1)
        {
            XRollover.setChecked(true);
        }
        if(map.get("yMirror") == 1)
        {
            XRollover.setChecked(true);
        }
        if(map.get("mac") == 1)
        {
            low_MAC_OS.setChecked(true);
        }
        else if(map.get("mac") == 2)
        {
            high_MAC_OS.setChecked(true);
        }
        refreshCalibrationData(1);
    }


    public void refreshCalibrationData(int where){
        CalibrationSettings settings = new CalibrationSettings();
        int ret = ((MainActivity)getActivity()).setting_interface.getCalibrationSettings(settings);
        if(ret < 0)
        {
            Log.e(TAG, "refreshCalibrationData fail");
            return;
        }
        final CalibrationData data = new CalibrationData();
        for (int i = 0; i < settings.pointCount; i++) {
            ret = ((MainActivity) getActivity()).setting_interface.getCalibrationPointData((byte) where,(byte)i,data);
            if(ret < 0)
                return;
            final int index = i;
            setEditText(index,data);
        }
    }
    public void setEditText(int index,CalibrationData data)
    {
        switch (index)
        {
            case 0:
                editText_target_x0.setText("" + data.targetX);
                editText_target_y0.setText("" +data.targetY);
                editText_collect_x0.setText("" +data.collectX);
                editText_collect_y0.setText("" +data.collectY);
                break;
            case 1:
                editText_target_x1.setText("" +data.targetX);
                editText_target_y1.setText("" +data.targetY);
                editText_collect_x1.setText("" +data.collectX);
                editText_collect_y1.setText("" +data.collectY);
                break;
            case 2:
                editText_target_x2.setText("" +data.targetX);
                editText_target_y2.setText("" +data.targetY);
                editText_collect_x2.setText("" +data.collectX);
                editText_collect_y2.setText("" +data.collectY);
                break;
            case 3:
                editText_target_x3.setText("" +data.targetX);
                editText_target_y3.setText("" +data.targetY);
                editText_collect_x3.setText("" +data.collectX);
                editText_collect_y3.setText("" +data.collectY);
                break;
        }
    }
    public void setCalibrateData(){
        CalibrationSettings settings = new CalibrationSettings();
        int ret = ((MainActivity)getActivity()).setting_interface.getCalibrationSettings(settings);
        if(ret < 0)
        {
            Log.e(TAG, "refreshCalibrationData fail");
            return;
        }
        CalibrationData data = new CalibrationData();
        for (int i = 0; i < settings.pointCount; i++) {
            ret = ((MainActivity) getActivity()).setting_interface.getCalibrationPointData((byte)1, (byte) i, data);
            if (ret < 0)
                return;
            CalibrationData setData = new CalibrationData();
            setData.maxX = data.maxX;
            setData.maxY = data.maxY;
            getEditTextValue(i,setData);
            ((MainActivity) getActivity()).setting_interface.setCalibrationPointData((byte)i,setData);
        }
        ((MainActivity) getActivity()).setting_interface.saveCalibrationData();
    }
    //响应选择文件的操作
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(SAVE_FILE_RESULT_CODE == requestCode){
            Bundle bundle = null;
            if(data!=null&&(bundle=data.getExtras())!=null){
                saveFilePath = bundle.getString("file");
                Log.e(TAG, "onActivityResult: 保存文件路径："+saveFilePath );
                saveCalibrateData();
            }
            else
            {
                saveFilePath = null;
            }
        }
        if(READ_FILE_RESULT_CODE == requestCode)
        {
            readFileFlag = false;
            Bundle bundle = null;
            if(data!=null&&(bundle=data.getExtras())!=null){
                String readFile = bundle.getString("file");
                readCalibrateData(readFile);
            }
        }
    }
    public void saveCalibrateData()
    {
        if(saveFilePath == null || saveFilePath == "")
        {
            Toast.makeText((MainActivity)getActivity(),"文件保存失败",Toast.LENGTH_SHORT).show();
            return;
        }

        CalibrationSettings settings = new CalibrationSettings();
        int ret = ((MainActivity)getActivity()).setting_interface.getCalibrationSettings(settings);
        if(ret < 0)
        {
            Log.e(TAG, "refreshCalibrationData fail");
            return;
        }
        JsonArray jsonArray = new JsonArray();
        final CalibrationData data = new CalibrationData();
        for (int i = 0; i < settings.pointCount; i++) {
            ret = ((MainActivity) getActivity()).setting_interface.getCalibrationPointData((byte) 1, (byte) i, data);
            if (ret < 0)
                return;
            JsonObject jsonObject = new JsonObject();
            int[] point = new int[6];
            jsonObject.addProperty("number",i);
            jsonObject.addProperty("targetX",data.targetX);
            jsonObject.addProperty("targetY",data.targetY);
            jsonObject.addProperty("collectX",data.collectX);
            jsonObject.addProperty("collectY",data.collectY);
            jsonObject.addProperty("maxX",data.maxX);
            jsonObject.addProperty("maxY",data.maxY);
            jsonArray.add(jsonObject.toString());
        }
        File file = new File(saveFilePath);
        if(!file.exists())
        {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            output.write(jsonArray.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText((MainActivity)getActivity(),"文件保存成功",Toast.LENGTH_SHORT).show();
    }
    public void readCalibrateData(String readFilePath)
    {
        if(readFilePath == null || readFilePath == "")
        {
            Toast.makeText((MainActivity)getActivity(),"文件读取失败",Toast.LENGTH_SHORT).show();
            return;
        }
        File file = new File(readFilePath);
        if(!file.exists())
        {
            Toast.makeText((MainActivity)getActivity(),"文件不存在",Toast.LENGTH_SHORT).show();
            return;
        }
        BufferedReader input = null;
        try {
            input = new BufferedReader(new FileReader(readFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String calibrateData = "";
        String tmpStr = "";
        while(true)
        {
            try {
                tmpStr = input.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(tmpStr != null)
                calibrateData += tmpStr;
            else
                break;
        }
        Log.e(TAG, "readCalibrateData: " + calibrateData );
        try {
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] calibrateDataArray = gson.fromJson(calibrateData, String[].class);

        for(String str : calibrateDataArray)
        {
            CalibrationData tmpCal = gson.fromJson(str,CalibrationData.class);
            setEditText(tmpCal.number,tmpCal);
            CalibrationData setData = new CalibrationData();
            setData.targetX = tmpCal.targetX;
            setData.targetY = tmpCal.targetY;
            setData.collectX = tmpCal.collectX;
            setData.collectY = tmpCal.collectY;
            setData.maxX = tmpCal.maxX;
            setData.maxY = tmpCal.maxY;
            ((MainActivity) getActivity()).setting_interface.setCalibrationPointData((byte)tmpCal.number,setData);
        }
        ((MainActivity) getActivity()).setting_interface.saveCalibrationData();
        Toast.makeText((MainActivity)getActivity(),"文件读取成功",Toast.LENGTH_SHORT).show();
    }
    class Refreshinfo extends Thread{

        @Override
        public void run() {
            waitRefresh = true;
            while (TouchManager.upgradeRunning || TouchManager.testRunning)
            {
                if(!waitRefresh)
                {
                    return;
                }
            }
            if(MainActivity.curPage == MainActivity.settingTab)
            {

                if(MainActivity.firstDevice() != null && MainActivity.firstDevice().bootloader == 0)
                {
                    setEnable(true);
                    ((MainActivity)getActivity()).setting_interface.startRefreshSetting();
                }
                else
                {
                    setEnable(false);
                }
            }
            waitRefresh = false;
        }
    }


    @Override
    public void onResume() {
        super.onResume();
//        if(calibrateFinish)
//        {
//            refreshSettings();
//            calibrateFinish = false;
//        }

//        if(view == null)
//            return;
//        if(MainActivity.firstDevice() != null && MainActivity.firstDevice().bootloader == 0)
//        {
//            setEnable(true);
////            refreshSettings();
//            ((MainActivity)getActivity()).setting_interface.startRefreshSetting();
//        }
//        else
//        {
//            setEnable(false);
//        }
    }

}
