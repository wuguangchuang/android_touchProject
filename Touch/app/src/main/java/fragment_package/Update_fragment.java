package fragment_package;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.touch.FileIO;
import com.example.touch.MainActivity;
import com.example.touch.R;
import com.example.touch.TouchManager;
import com.newskyer.meetingpad.fileselector.activity.FileSelectActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fragment_interface.Upgrade_fragment_interface;

public class Update_fragment extends Fragment implements Upgrade_fragment_interface {

    public static final int FILE_RESULT_CODE = 1;
    public static final int EVENT_TYPE_IMPORT_NZ = 2;
    private final String TAG = "myText";
    private TextView updateTextView;

    public String getUpdateString() {
        return updateString;
    }

    private String updateString = "";

    public int getUpgradeProgress() {
        return upgradeProgress;
    }

    private int upgradeProgress = 0;
    private ScrollView updateScrollView;
    private String text = "";
    private ArrayAdapter<String> spinnerAdapter;
    private Spinner spinner;
    private List<String> spinnerData;
    private TextView spinnerText;
    private Button upgradeBtn;
    private ProgressBar upgradeProgressBar;
    private Button chooseFileBtn;
    private View view;
    private Intent chooseFileIntent;
    private boolean btnChecked = true;
    private ImageView imageView;
    private TextView imagetext;

    public int getImageType() {
        return imageType;
    }

    private int imageType = 0;

    public String getImageText() {
        return imageStr;
    }



    public String imageStr = "";
    public boolean firstRestoreUpgradeFile = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

//        MainActivity.addFragment(new Update_fragment(),"Update_fragment");
        if(view == null)
        {
            Log.d(TAG, "onCreateView: 缓存updatePage");

            //加载Fragment
            view = inflater.inflate(R.layout.update_fragment,container,false);
            initUpgradeControls(view);
            ((MainActivity)getActivity()).setUpgrade_fragment_interface(this);
            if(!((MainActivity)getActivity()).upgradeString.equals(""))
            {
                setTextViewStr(((MainActivity)getActivity()).upgradeString);
                setUpgradeInProgress(((MainActivity)getActivity()).upgradePro);
                setUpgradeImageInfo(((MainActivity)getActivity()).upgradeImageType,((MainActivity)getActivity()).upgradeImageText);
            }

            upgradeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (btnChecked && upgradeBtn.isEnabled()) {
                        if (TouchManager.testRunning) {
                            if (getActivity() != null) {
                                Toast.makeText(getActivity(), "正在测试中,请勿操作！！", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            upgradeBtn.setText(R.string.cancel_upgrade);
                            btnChecked = false;
                            ((MainActivity) getActivity()).upgrade_interface.startUpgrade();
                        }

                    }
                    else
                    {
                        setUpgradeImageInfo(7,getResources().getString(R.string.cancel_upgrade));
                        setUpgradeBtnStatus(getResources().getString(R.string.upgrade),true);
                        ((MainActivity)getActivity()).upgrade_interface.cancelUpgrade();
                    }
                }

            });
            chooseFileBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(getActivity() == null)
                    {
                        setTextViewStr("getActivity() == null");
                    }else
                    {
                            Intent intent =new Intent("newskyer.intent.action.MOBILE_SELECT_FILE");
                            intent.putExtra(FileSelectActivity.SELECT_ITEMS, FileSelectActivity.SELECT_ITEM_DOC);
                            startActivityForResult(intent, EVENT_TYPE_IMPORT_NZ);

//                        Intent intent = new Intent(getActivity(), MyFileManager.class);
//                        startActivityForResult(intent, FILE_RESULT_CODE);
                    }
                }
            });

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if(spinner.getItemAtPosition(i).toString().equals("清空历史"))
                    {
                        spinnerData.clear();
                        spinnerData.add(0,"清空历史");
                        spinnerData.add(0," ");
//                        TextView upgrade_file = view.findViewById(R.id.spinnerText);
//                        upgrade_file.setText(spinnerData.get(0));
                        spinnerAdapter.notifyDataSetChanged(); // 通知spinner刷新数据
                        spinner.setSelection(0);
                        TouchManager.path = " ";
                        saveUpgradeFile();

                    }
                    else
                    {
                        //                Log.d(TAG, "onItemSelected: 你选择了"+spinner.getItemAtPosition(i));
//                Log.d(TAG, "onItemSelected: 你选择了"+spinnerData.get(i));
                        //删除下拉框中的对应的，然后在添加在首条,
                        String tmp = spinnerData.get(i);
                        spinnerData.remove(i);
                        spinnerData.add(0,tmp);
                        TouchManager.path = spinnerData.get(0);
                        spinnerAdapter.notifyDataSetChanged(); // 通知spinner刷新数据
                        spinner.setSelection(0);
                        saveUpgradeFile();
                    }

                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            if(!MainActivity.quickUpgradeSwitch)
            {
                //非定制版升级固件时，获取曾经保存在本地保存的升级文件
                restoreUpgradeFile();
            }

        }


//        textThread.start();
        return view;
    }
    private void initUpgradeControls(View view){
        upgradeBtn = view.findViewById(R.id.startUpdateBtn);
        chooseFileBtn = view.findViewById(R.id.choose_file_btn);
        updateScrollView = view.findViewById(R.id.updateScrollView);
        updateTextView = view.findViewById(R.id.updateTextView);
        spinner = view.findViewById(R.id.updateSpinner);
        spinnerText = (TextView) view.findViewById(R.id.spinnerText);
        upgradeProgressBar = view.findViewById(R.id.upProgressBar);
        imageView = view.findViewById(R.id.upgrade_image);
        imagetext = view.findViewById(R.id.upgrade_image_text);
        spinnerData = new ArrayList<String>();
        spinnerData.add(0,"清空历史");
        spinnerData.add(0," ");

        if(getContext()!=null)
        {
            Log.d(TAG, "initUpgradeControls:给spinner绑定了适配器");
//            spinnerAdapter = new ArrayAdapter<String>(getContext(),R.layout.spinner_text_view,R.id.spinnerText,spinnerData);
            spinnerAdapter = new ArrayAdapter<String>(getContext(),R.layout.spinner_text_view,R.id.spinnerText,spinnerData);
            spinner.setAdapter(spinnerAdapter);
        }

        Toast.makeText(getActivity(),"升级界面控件初始化完成",Toast.LENGTH_SHORT);
        Log.d(TAG, "initUpgradeControls: 升级界面控件初始化完成");
    }

    @Override
    public void setTextViewStr(String text){
        updateString += text + "\n";
        if(getActivity() != null)
        {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateTextView.setText(updateString);
                    updateScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });

        }

    }

    @Override
    public void setUpgradeInProgress(final int progress) {
        upgradeProgress = progress;
        if(getActivity() != null)
        {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    upgradeProgressBar.setProgress(progress);
                }
            });

        }

    }
    @Override
    public void setUpgradeBtnStatus(final String btnText, final boolean enable)
    {
        if(getActivity() != null)
        {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    upgradeBtn.setText(btnText);
                    upgradeBtn.setEnabled(enable);
                    btnChecked = enable;
                    if(enable)
                    {
                        upgradeBtn.setBackgroundColor(getResources().getColor(R.color.blue_color));
                    }
                    else
                    {
                        upgradeBtn.setBackgroundColor(getResources().getColor(R.color.gray_color));
                    }
                }
            });

        }

    }

    //响应选择文件的操作
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean exist = false;
        if(EVENT_TYPE_IMPORT_NZ == requestCode)
        {
            if(data == null || data.equals(""))
            {
                return;
            }
            String checkFireware = data.getStringExtra(FileSelectActivity.FILE_PATH);
            if(checkFireware != null)
            {
                if(spinnerData.get(0).equals(" "))
                {
                    spinnerData.remove(0);
                }
                exist = false;
                for(int i = 0;i < spinnerData.size();i++)
                {
                    if(spinnerData.get(i).equals(checkFireware))
                    {
                        exist = true;
                        Toast.makeText((MainActivity)getActivity(),"选择升级文件已经存在",Toast.LENGTH_SHORT).show();
                        //删除下拉框中的对应的，然后在添加在首条
                        spinnerData.remove(i);
                        spinnerData.add(0,checkFireware);
//                        TextView upgrade_file =  view.findViewById(R.id.spinnerText);
//                        upgrade_file.setText(spinnerData.get(i));
                        spinnerAdapter.notifyDataSetChanged(); // 通知spinner刷新数据
                        spinner.setSelection(0);
                        TouchManager.path = spinnerData.get(i);
                        saveUpgradeFile();
                        break;
                    }
                }
                if(!exist)
                {
                    Log.e(TAG, "onActivityResult: 添加了一个固件");
                    spinnerData.add(0,checkFireware);
//                    TextView upgrade_file = view.findViewById(R.id.spinnerText);
//                    upgrade_file.setText(spinnerData.get(0));
                    spinnerAdapter.notifyDataSetChanged(); // 通知spinner刷新数据
                    spinner.setSelection(0);
                    TouchManager.path = spinnerData.get(0);
                    saveUpgradeFile();
                }
            }

        }
        if(FILE_RESULT_CODE == requestCode){
            Bundle bundle = null;
            if(data!=null&&(bundle=data.getExtras())!=null){
//                setTextViewStr(bundle.getString("file"));
                if(spinnerData.get(0).equals(" "))
                {
                    spinnerData.remove(0);
                }
//                String uri = bundle.getString("file");
                exist = false;
                for(int i = 0;i < spinnerData.size();i++)
                {
                    if(spinnerData.get(i).equals(bundle.getString("file")))
                    {
                        exist = true;
                        Toast.makeText((MainActivity)getActivity(),"选择升级文件已经存在",Toast.LENGTH_SHORT).show();
                        //删除下拉框中的对应的，然后在添加在首条
                        spinnerData.remove(i);
                        spinnerData.add(0,bundle.getString("file"));
//                        TextView upgrade_file =  view.findViewById(R.id.spinnerText);
//                        upgrade_file.setText(spinnerData.get(i));
                        spinnerAdapter.notifyDataSetChanged(); // 通知spinner刷新数据
                        spinner.setSelection(0);
                        TouchManager.path = spinnerData.get(i);
                        saveUpgradeFile();
                        break;
                    }
                }
                if(!exist)
                {
                    Log.e(TAG, "onActivityResult: 添加了一个固件");
                    spinnerData.add(0,bundle.getString("file"));
//                    TextView upgrade_file = view.findViewById(R.id.spinnerText);
//                    upgrade_file.setText(spinnerData.get(0));
                    spinnerAdapter.notifyDataSetChanged(); // 通知spinner刷新数据
                    spinner.setSelection(0);
                    TouchManager.path = spinnerData.get(0);
                    saveUpgradeFile();
                }

            }
        }

    }
//    public String uriToFile(String uri)
//    {
//        String file = "file:///";
//        file += uri.replace("/", "\\");
//
//        return file;
//    }


    @Override
    public void onResume() {
        super.onResume();
        updateTextView.setText(updateString);
        updateScrollView.fullScroll(ScrollView.FOCUS_DOWN);
        upgradeProgressBar.setProgress(upgradeProgress);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }
    @Override
    public void setUpgradeImageInfo(final int type, final String text) {
        if (getActivity() != null)
        {
            this.imageType = type;
            this.imageStr = text;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (type){
                        case 1:
                            imageView.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.success));
                            break;
                        case 2:
                            imageView.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.error));
                            break;
                        case 3:
                            imageView.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.no_touch));
                            break;
                        case 4:
                            imageView.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.touch));
                            break;
                        case 5:
                            imageView.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.update));
                            break;
                        case 6:
                            imageView.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.disconnect));
                            break;
                        case 7:
                            imageView.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.cancel));
                            break;
                        case 8:
                            imageView.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.no_permission));
                            break;

                    }
                    imagetext.setText(text);

                }
            });
        }
    }
    public void saveUpgradeFile()
    {
        //获取内置SD目录路径
        String SDPath = FileIO.getInterSDPath();
        String upgradeFilePath = SDPath + "/TouchAssistant";
        File filePath = new File(upgradeFilePath);
        if(!filePath.exists())
        {
            filePath.mkdirs();
            filePath.setExecutable(true);//设置可执行权限
            filePath.setReadable(true);//设置可读权限
            filePath.setWritable(true);//设置可写权限
        }
        String fileStr = upgradeFilePath + "/upgrade";
        File file  = new File(fileStr);
        if(filePath.exists() && filePath.isDirectory())
        {
            if(!file.exists() || !file.isFile())
            {
                try {
                    file.createNewFile();
                    file.setExecutable(true);
                    file.setReadable(true);
                    file.setWritable(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(file.exists() && file.isFile())
            {
                String[] upgradeFile = new String[spinnerData.size()];
                for (int i = 0;i < spinnerData.size();i++)
                {
                    if(spinnerData.get(i).equals(" ") || spinnerData.get(i).equals("清空历史") ||
                    spinnerData.get(i).isEmpty() || spinnerData.get(i) == null)
                    {
                        continue;
                    }
                    upgradeFile[i] = spinnerData.get(i);
                }
                FileIO.writeLineFile(fileStr,upgradeFile);
            }
        }
    }
    public void restoreUpgradeFile(){

        if(!MainActivity.storagePermission)
        {
            return;
        }
        //获取内置存储目录路径
        String SDPath = FileIO.getInterSDPath();
        String readFilePath = SDPath + "/TouchAssistant/upgrade";
        List<String> filePathList = FileIO.readInfoFromFile(readFilePath);
//        Log.d(TAG, "restoreUpgradeFile: 恢复数据的文件个数：" + filePathList.size());
        if(filePathList == null)
            return;
        if(!firstRestoreUpgradeFile)
        {
            return;
        }
        firstRestoreUpgradeFile = false;
        if(spinnerData.get(0).equals(" ") || spinnerData.get(0) == null)
        {
            spinnerData.remove(0);
        }

        for(int i = 0;i < filePathList.size()-1;i++)
        {
            if(filePathList.get(i) == null || filePathList.get(i).equals(" ") || filePathList.get(i).isEmpty())
                continue;
//            Log.e(TAG, "restoreUpgradeFile: " + filePathList.get(i));
            spinnerData.add(i,filePathList.get(i));
            TouchManager.path = spinnerData.get(0);
            spinnerAdapter.notifyDataSetChanged(); // 通知spinner刷新数据
            spinner.setSelection(0);
        }
    }

    @Override
    public void addUpgradeFilePath(String filePath) {
        Log.d(TAG, "addUpgradeFilePath: start");
        if(spinnerData.get(0).equals(" ") || spinnerData.get(0) == null)
        {
            spinnerData.remove(0);
        }
        spinnerData.add(0,filePath);
        spinnerAdapter.notifyDataSetChanged(); // 通知spinner刷新数据
        spinner.setSelection(0);
    }
}
