package com.example.touch;

import android.Manifest;
import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import adapter.MySaveFileAdapter;

public class MySaveFileManager extends ListActivity {
    private List<String> items = null;
    private List<String> paths = null;
    private String rootPath = "/";
    private String curPath = "/";
    private TextView mPath;
    private TextView editFileName;
    private HorizontalScrollView file_scrollbar;
    private final static String TAG = "fileText";
    private String defaultFileName = "saveCalibrationPoint.json";
    private String cusFileName = "";
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        checkPermission();
    }
    public void showFileInfo()
    {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.savefileselect);
        mPath = (TextView) findViewById(R.id.show_path);
        file_scrollbar = findViewById(R.id.save_file_scrollbar);
        editFileName = findViewById(R.id.save_file_editText);
        rootPath = getInnerSDCardPath();
        curPath = rootPath;
//        List<String> extSDCardPath = getExtSDCardPath();
//        if(extSDCardPath.size() > 0)
//        {
//            rootPath = extSDCardPath.get(0);
//            curPath = rootPath;
//        }
        Log.d(TAG, "SD卡路径: " + rootPath);
        Button save_button = (Button) findViewById(R.id.save_btn);
        save_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent data = new Intent(MySaveFileManager.this, MainActivity.class);
                Bundle bundle = new Bundle();
                if(new File(curPath).isDirectory())
                {
                    if(editFileName.getText().toString() == null)
                    {
                        curPath +="/" +  defaultFileName;
                    }
                    else
                    {
                        if(editFileName.getText().toString().contains(".json"))
                        {
                            curPath += "/" + editFileName.getText().toString();
                        }
                        else
                        {
                            curPath += "/" + editFileName.getText().toString() + ".json";
                        }

                    }
                    bundle.putString("file", curPath);
                    data.putExtras(bundle);
                    setResult(2, data);

                    finish();
                }
                else
                {
                    bundle.putString("file", curPath);
                    data.putExtras(bundle);
                    setResult(2, data);
                    finish();
                }

            }
        });
        Button buttonCancle = (Button) findViewById(R.id.cancel_save_btn);
        buttonCancle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        getFileDir(rootPath);
    }
    //获取内置SD目录路径
    public String getInnerSDCardPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }
    //获取外置SD目录路径
    public List<String> getExtSDCardPath() {
        List<String> lResult = new ArrayList<String>();
        try {
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec("mount");
            InputStream is = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("extSdCard")) {
                    String[] arr = line.split(" ");
                    String path = arr[1];
                    File file = new File(path);
                    if (file.isDirectory()) {
                        lResult.add(path);
                    }
                }
            }
            isr.close();
        } catch (Exception e) {
        }
        return lResult;
    }
    //获取SD卡读写权限
    public void checkPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//android 6.0以上

            int writePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (writePermission != PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"申请SD读写权限");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                }

            }else{
                Log.v(TAG,"SD读写权限已申请");
                showFileInfo();
            }
        }else{//android 6.0以下
            Log.v(TAG,"测试手机版本为：android 6.0以下");
            showFileInfo();
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions,  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {//允许
                Log.d(TAG, "SD读写权限申请成功！");
                showFileInfo();
            } else {//拒绝
                Log.e(TAG, "SD读写权限申请失败！");
                finish();
            }
        }
    }

    private void getFileDir(String filePath) {
        mPath.setText(filePath);
        TouchManager.path = filePath;
//        file_scrollbar.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
        items = new ArrayList<String>();
        paths = new ArrayList<String>();
        File f = new File(filePath);
        File[] files = f.listFiles();
        if (!filePath.equals(rootPath)) {
            items.add("b1");
            paths.add(rootPath);
            items.add("b2");
            paths.add(f.getParent());
        }
        if(files != null)
        {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if(file.isFile())
                {
                    if(file.getName().contains(".json"))
                    {
                        editFileName.setText(file.getName());
                        cusFileName = file.getAbsolutePath();
                        items.add(file.getName());
                        paths.add(file.getPath());
                    }
                }
                else if(file.isDirectory())
                {
                    items.add(file.getName());
                    paths.add(file.getPath());
                }

            }
        }

        setListAdapter(new MySaveFileAdapter(this, items, paths));
    }
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        File file = new File(paths.get(position));
        if (file.isDirectory()) {
            curPath = paths.get(position);
            getFileDir(paths.get(position));
        } else {
//            curPath = paths.get(position);
//            mPath.setText(curPath);
            editFileName.setText(file.getName());
            curPath = file.getPath();
            mPath.setText(curPath);
            //可以打开文件
        }
    }

}

