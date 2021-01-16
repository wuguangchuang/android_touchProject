package com.newskyer.meetingpad.fileselector.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.newskyer.meetingpad.R;
import com.newskyer.meetingpad.fileselector.file.adapter.NavigatioListAdapter;
import com.newskyer.meetingpad.fileselector.fragment.CategoryFragment;
import com.newskyer.meetingpad.fileselector.fragment.DocFragment;
import com.newskyer.meetingpad.fileselector.fragment.MusicFragment;
import com.newskyer.meetingpad.fileselector.fragment.NccFragment;
import com.newskyer.meetingpad.fileselector.fragment.PictureFragment;
import com.newskyer.meetingpad.fileselector.fragment.VideoFragment;
import com.newskyer.meetingpad.fileselector.model.TabInfo;
import com.newskyer.meetingpad.fileselector.util.MediaFileTool;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;


/**
 * @author liziyang
 * @since 2018/1/24
 */
public class FileSelectActivity extends AppCompatActivity {

    public final String TAG = "myfileText";
    private int selectType;
    private int selectItems;
    public static final int SELECT_TYPE_ALL = 0;
    public static final int SELECT_TYPE_PICTURE = 1;
    public static final int SELECT_TYPE_MUSIC = 2;
    public static final int SELECT_TYPE_VIDEO = 3;
    public static final int SELECT_TYPE_DOC = 4;
    public static final int SELECT_TYPE_NCC = 5;
    public static final int SELECT_TYPE_KEYWORD = 6;
    public static final int SELECT_TYPE_PDF = 7;
    public static final int SELECT_TYPE_NZ = 8;
    public static final int SELECT_TYPE_DIR = 9;
    public static final String SELECT_TYPE = "select_type";
    public static final String SELECT_OPEN = "select_open";
    public static final String SELECT_WEB = "select_send_web";
    public static final String SELECT_DIR = "select_dir";
    public static final String SELECT_ITEMS = "select_items";
    public static final String SELECT_DEFAULT_ITEM = "select_default_item";
    public static final int SELECT_ITEM_ALL = 0xff;
    public static final int SELECT_ITEM_PICTURE = 1;
    public static final int SELECT_ITEM_MUSIC = 1 << 1;
    public static final int SELECT_ITEM_VIDEO = 1 << 2;
    public static final int SELECT_ITEM_DOC = 1 << 3;
    public static final int SELECT_ITEM_NCC = 1 << 4;
    public static final int SELECT_ITEM_PDF = 1 << 5;
    public static final int SELECT_ITEM_NZ = 1 << 6;

    public static final int REQUEST_CODE = 2222;
    public static final String FILE_PATH = "file_path";
    public static final String FILE_NAME = "file_name";



    // CustomFragmentTabHost fragmentTabHost;

    private static String STRING_CATEGORY = "目录";
    private static String STRING_PICTURE = "图片";
    private static String STRING_MUSIC = "音乐";
    private static String STRING_VIDEO = "视频";
    private static String STRING_DOC = "文档";
    private static String STRING_NCC = "笔迹";

    private boolean mActionView = false;
    private boolean mWebView = false;

    private String webIp = "";
    private int webPort = 0;
    private NavigatioListAdapter adapter;
    private ListView navigation_list;
    private Fragment lastFragment;
    private  List<TabInfo> tabInfoList;

    public static boolean readPermission = false;
    public boolean isExitChooseFile = false;
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbDiskReceiver);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readPermission = false;
        exitChooseFile = false;
        setContentView(R.layout.activity_file_select);
        Resources resources = getResources();
        STRING_CATEGORY = resources.getString(R.string.category);
        STRING_PICTURE = resources.getString(R.string.picture);
        STRING_MUSIC = resources.getString(R.string.music);
        STRING_VIDEO = resources.getString(R.string.video);
        STRING_DOC = resources.getString(R.string.doc);
        STRING_NCC = resources.getString(R.string.ncc);

        final LayoutInflater layoutInflater = LayoutInflater.from(this);

        selectItems = getIntent().getIntExtra(SELECT_ITEMS, 0);
        // fragmentTabHost.setup(this, getSupportFragmentManager(), R.id.layout_file_content);
        // Fragment fragment = new PictureFragment();
	    CategoryFragment categoryFragment = new CategoryFragment();
        TabInfo tabInfoAll = new TabInfo(STRING_CATEGORY, categoryFragment, R.drawable.ic_file_category, 0, SELECT_ITEM_ALL);
        TabInfo tabInfoPic = new TabInfo(STRING_PICTURE, new PictureFragment(), R.drawable.ic_file_note, 1, SELECT_ITEM_PICTURE);
        TabInfo tabInfoMusic = new TabInfo(STRING_MUSIC, new MusicFragment(), R.drawable.ic_file_note, 2, SELECT_ITEM_MUSIC);
        TabInfo tabInfoVideo = new TabInfo(STRING_VIDEO, new VideoFragment(), R.drawable.ic_file_note, 3, SELECT_ITEM_VIDEO);
        TabInfo tabInfoDoc = new TabInfo(STRING_DOC, new DocFragment(selectItems == SELECT_ITEM_PDF ?
                MediaFileTool.MEDIA_TYPE_PDF : MediaFileTool.MEDIA_TYPE_DOC),
                R.drawable.ic_file_note, 4,
                SELECT_ITEM_DOC);
        TabInfo tabInfoNcc = new TabInfo(STRING_NCC, new NccFragment(), R.drawable.ic_file_note, 5, SELECT_ITEM_NCC);

        String dir = getIntent().getStringExtra(SELECT_DIR);
        Log.d("paint", "dir = " + dir);
        if (dir != null && !dir.isEmpty())
            categoryFragment.setDir(dir);

        tabInfoList = new ArrayList<>();
        tabInfoList.add(tabInfoAll);
        selectType = getIntent().getIntExtra(SELECT_TYPE, SELECT_TYPE_ALL);
        mActionView = getIntent().getBooleanExtra(SELECT_OPEN, false);
        mWebView = getIntent().getBooleanExtra(SELECT_WEB, false);
        webIp = getIntent().getStringExtra("ip");
        webPort = getIntent().getIntExtra("port", 0);
        int defaultItem = getIntent().getIntExtra(SELECT_DEFAULT_ITEM, 0);
        Log.d("paint", String.format("select: type=%d, items=%x", selectType, selectItems));
        if (selectItems != 0) {
        	if ((selectItems & SELECT_ITEM_PICTURE) != 0) {
                tabInfoList.add(tabInfoPic);
                if (defaultItem == 0)
                    defaultItem = SELECT_ITEM_PICTURE;
            }
            if ((selectItems & SELECT_ITEM_MUSIC) != 0) {
                tabInfoList.add(tabInfoMusic);
                if (defaultItem == 0)
                    defaultItem = SELECT_ITEM_MUSIC;
            }
            if ((selectItems & SELECT_ITEM_VIDEO) != 0) {
                tabInfoList.add(tabInfoVideo);
                if (defaultItem == 0)
                    defaultItem = SELECT_ITEM_VIDEO;
            }
            if ((selectItems & SELECT_ITEM_DOC) != 0) {
                tabInfoList.add(tabInfoDoc);
                if (defaultItem == 0)
                    defaultItem = SELECT_ITEM_DOC;
            }
            if ((selectItems & SELECT_ITEM_NCC) != 0) {
                tabInfoList.add(tabInfoNcc);
                if (defaultItem == 0)
                    defaultItem = SELECT_ITEM_ALL;
            }

        } else {
            switch (selectType) {
                case SELECT_TYPE_ALL:
                    tabInfoList.add(tabInfoPic);
                    tabInfoList.add(tabInfoMusic);
                    tabInfoList.add(tabInfoVideo);
                    tabInfoList.add(tabInfoDoc);
                    tabInfoList.add(tabInfoNcc);
                    selectItems = SELECT_ITEM_ALL;
                    defaultItem = 0;
                    break;
                case SELECT_TYPE_PICTURE:
                    tabInfoList.add(tabInfoPic);
                    defaultItem = selectItems = SELECT_ITEM_PICTURE;
                    break;
                case SELECT_TYPE_MUSIC:
                    tabInfoList.add(tabInfoMusic);
                    defaultItem = selectItems = SELECT_ITEM_MUSIC;
                    break;
                case SELECT_TYPE_VIDEO:
                    tabInfoList.add(tabInfoVideo);
                    defaultItem = selectItems = SELECT_ITEM_VIDEO;
                    break;
                case SELECT_TYPE_DOC:
                    tabInfoList.add(tabInfoDoc);
                    defaultItem = selectItems = SELECT_ITEM_DOC;
                    break;
                case SELECT_TYPE_PDF:
                    tabInfoList.add(tabInfoDoc);
                    tabInfoDoc.setSelectType(SELECT_TYPE_PDF);
//                    defaultItem = selectItems = SELECT_ITEM_PDF;
                    defaultItem = 0;
                    selectItems = SELECT_ITEM_ALL;
                    break;
                case SELECT_TYPE_NCC:
                    tabInfoList.add(tabInfoNcc);
                    defaultItem = selectItems = SELECT_ITEM_NCC;
                    break;
            }
        }
        Log.d("paint", String.format("# after select: type=%d, items=%x, default=%x", selectType, selectItems,
                defaultItem));
        navigation_list = (ListView) findViewById(R.id.navigation_list);
        adapter = new NavigatioListAdapter(this, R.layout.layout_selector_tab, tabInfoList, selectType);
        navigation_list.setAdapter(adapter);
        /**设置默认打开的Fragement
         * 设置左侧导航栏选中状态
         * 例：点击插入图片按钮，即打开PictureFragment，左侧选中图片按钮*/
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        for (int i = 0; i < tabInfoList.size(); i++) {
            fragmentTransaction.add(R.id.layout_file_content, tabInfoList.get(i).getCls());
//            if ((tabInfoList.get(i).getSelectItem() == defaultItem)  || (i == 0 && defaultItem == 0)) { // 设置文件选择的默认与选择默认全部项时的起始项
            if(i == 0){ //表示不管选择什么类型的文件或者目录，起始项都是第0项（存储设备的项）
                lastFragment = tabInfoList.get(i).getCls();
                Log.d(TAG, "onCreate: i =============== " + i);
                final int finalI = i;
                navigation_list.post(new Runnable() {
                    @Override
                    public void run() {
                        navigation_list.setSelection(finalI);
                        adapter.setSelectedPosition(finalI);
                        adapter.notifyDataSetInvalidated();
                    }
                });

            } else
                fragmentTransaction.hide(tabInfoList.get(i).getCls());
        }
        fragmentTransaction.commit();


        navigation_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /**切换Fragment打开状态*/
                if (lastFragment != tabInfoList.get(position).getCls()) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.hide(lastFragment);
                    transaction.show(tabInfoList.get(position).getCls());
                    transaction.commit();
                    lastFragment = tabInfoList.get(position).getCls();
                }
                Log.e(TAG, "" + navigation_list.getCount() + "--" + navigation_list.getChildCount());

                adapter.setSelectedPosition(position);
                adapter.notifyDataSetInvalidated();
            }
        });

        IntentFilter intentFilterUSB = new IntentFilter();
        intentFilterUSB.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilterUSB.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilterUSB.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilterUSB.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilterUSB.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilterUSB.addDataScheme("file");
        registerReceiver(usbDiskReceiver, intentFilterUSB);

    }

    private BroadcastReceiver usbDiskReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            Log.d("2222", "╔════════════════════════════════════════════════");
//            Log.d("2222", "║    action : " + intent.getAction());
//            Log.d("2222", "╚════════════════════════════════════════════════");
            switch (intent.getAction()) {
                case Intent.ACTION_MEDIA_SCANNER_FINISHED:
                    break;
                case Intent.ACTION_MEDIA_MOUNTED:
                    // 挂载
                    break;
                case Intent.ACTION_MEDIA_REMOVED:
                    // 移除


//                    if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
//                        Log.d("2222", "╔════════════════════════════════════════════════");
//                        Log.d("2222", "║    挂载");
//                        Log.d("2222", "╚════════════════════════════════════════════════");
//                    } else {
//                        Log.d("2222", "╔════════════════════════════════════════════════");
//                        Log.d("2222", "║    卸载");
//                        Log.d("2222", "╚════════════════════════════════════════════════");
//                    }
//                    if (currentDir.equals("当前设备")) {
                    // 更新
                    //          fragmentTabHost.updateDevices();
//                    }
                    break;
            }
        }
    };

    public void finish() {
        Log.e("FileSelectActivity","onDestroy");
        if (mActionView) {
            Intent data = getIntent();
            String path = data.getStringExtra(FileSelectActivity.FILE_PATH);
            if (path == null || path.isEmpty()) {
                super.finish();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String type = null;
            int index = path.lastIndexOf(".");
            if (index < 0) {
                super.finish();
                return;
            }
            String ext = path.substring(index + 1);
            Uri uri = Uri.parse("file://" + path);
            if (type == null && path.endsWith(".bin")) {
                uri = Uri.parse(URLEncoder.encode("file://" + path));
                intent.putExtra("encode", true);
                type = "note/ncc";
            } else {
                type = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(ext.toLowerCase());
            }
            if (Build.VERSION.SDK_INT >= 24) { // 24 is N
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
            }
            intent.setDataAndType(uri, type);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (Exception e) {
            }
        } else if (mWebView) {
            Intent data = getIntent();
            String path = data.getStringExtra(FileSelectActivity.FILE_PATH);
            if (path == null || path.isEmpty()) {
                super.finish();
                return;
            }
            Intent send = new Intent("newskyer.action.intent.WEB_SEND_FILE");
            Log.d("paint", "file: " + webIp + ":" + webPort);
            send.putExtra("path", path);
            send.putExtra("ip", webIp);
            send.putExtra("port", webPort);
            sendBroadcast(send);
        }
        super.finish();
    }

    public int getSelectType() {
        return selectType;
    }
    public int getSelectItems() {
        return selectItems;
    }


    @Override
    protected void onResume() {
        readPermission = checkPermissionREAD_EXTERNAL_STORAGE(this);
        super.onResume();
    }

    //检查API级别23及更高级别的手动权限
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;
    public boolean checkPermissionREAD_EXTERNAL_STORAGE(
            final Context context) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        (Activity) context,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    ActivityCompat.requestPermissions((Activity) context,
                            new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
//                    showDialog("External storage", context,
//                            Manifest.permission.READ_EXTERNAL_STORAGE);
                } else {
                    ActivityCompat
                            .requestPermissions(
                                    (Activity) context,
                                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
                return false;
            } else {
                return true;
            }

        } else {
            return true;
        }
    }
    public void showDialog(final String msg, final Context context,
                           final String permission) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle("Permission necessary");
        alertBuilder.setMessage(msg + " permission is necessary");
        alertBuilder.setPositiveButton(android.R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions((Activity) context,
                                new String[] { permission },
                                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                });
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }
    static public boolean exitChooseFile = false;
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // do your stuff
                    readPermission = true;
                    Log.d(TAG, "onRequestPermissionsResult: 111111111有读权限");
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: 222222没有读取权限");
                    exitChooseFile = true;
                    Intent intent = new Intent();
//                    intent.putExtra(FileSelectActivity.FILE_PATH,"");
//                    intent.putExtra(FileSelectActivity.FILE_NAME,"");
                    this.setResult(Activity.RESULT_OK, intent);
                    this.setIntent(intent);
                    this.finish();

                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions,
                        grantResults);
        }
    }

}