package com.newskyer.meetingpad.fileselector.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.newskyer.meetingpad.R;
import com.newskyer.meetingpad.fileselector.file.adapter.FileListAdapter;
import com.newskyer.meetingpad.fileselector.file.adapter.HistoryListAdapter;
import com.newskyer.meetingpad.fileselector.file.adapter.PathListAdapter;
import com.newskyer.meetingpad.fileselector.file.model.FileInfo;
import com.newskyer.meetingpad.fileselector.util.LocalFileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class PathSelectorFragment extends Fragment {

    private ListView listViewpathSelector;
    private GridView gridViewpathSelector;


    private List<FileInfo> fileInfoList = new ArrayList<>();
    private FileListAdapter adapter;
    private PathListAdapter pathAdapter;

    private String mLocalDevice = "";
    private String currentDir = "Local Device";//getResources().getString(R.string.current_dir);
    private TextView textViewCurrentPath;
    private String setDir = "";

    private View imageViewBack;

    private String fileInnerPath;
    private FileInfo fileInfoInner;

    private List<FileInfo> fileInfoListUSB = new ArrayList<>();
    private List<PathListAdapter.PathData> dirList = new ArrayList<>();

    private RadioButton tile, list;
    private ImageView imageView;
    private Button pathSelector_ok;
    private RecyclerView recyclerViewDirPath;
    private TextView currentRoot;
    private LinearLayoutManager linearLayoutManager;
    private List<PathListAdapter.PathData> historyPath = new ArrayList<>();
    private String parentDir = "Local Device";
    private int selectorNum = -1;
    private FrameLayout parentLayout;
    private ListView dirListView;
    private boolean isHistory = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_path_selector, container, false);
        listViewpathSelector = (ListView) view.findViewById(R.id.list_file_selector);
        gridViewpathSelector = (GridView) view.findViewById(R.id.grid_file_selector);
        parentLayout = (FrameLayout) view.findViewById(R.id.parent_layout);
        textViewCurrentPath = (TextView) view.findViewById(R.id.text_file_current_path);
        imageView = (ImageView) view.findViewById(R.id.pathSelector_image_empty);
        LinearLayout pathSelector_list_linear = (LinearLayout) view.findViewById(R.id.pathSelector_list_linear);
        LinearLayout pathSelector_tile_linear = (LinearLayout) view.findViewById(R.id.pathSelector_tile_linear);
        final TextView pic_text_list = (TextView) view.findViewById(R.id.pathSelector_text_list);
        final TextView pic_text_tile = (TextView) view.findViewById(R.id.pathSelector_text_tile);
        tile = (RadioButton) view.findViewById(R.id.pathSelector_icon_tile);
        list = (RadioButton) view.findViewById(R.id.pathSelector_icon_list);
        pathSelector_ok = (Button) view.findViewById(R.id.pathSelector_ok);
        pathSelector_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("currentDir", textViewCurrentPath.getText());
                getActivity().setResult(Activity.RESULT_OK, intent);
                getActivity().setIntent(intent);
                getActivity().finish();
            }
        });
        pic_text_tile.setSelected(true);
        adapter = new FileListAdapter(getContext(), R.layout.item_file_grid, fileInfoList, FileListAdapter.TYPE_EXPLORER, true);
        gridViewpathSelector.setAdapter(adapter);
        pathSelector_tile_linear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list.setChecked(false);
                tile.setChecked(true);
                pic_text_tile.setSelected(true);
                pic_text_list.setSelected(false);
                adapter = new FileListAdapter(getContext(), R.layout.item_file_grid, fileInfoList, FileListAdapter.TYPE_EXPLORER, true);
                gridViewpathSelector.setAdapter(adapter);
                view.findViewById(R.id.grid_file_selector).setVisibility(View.VISIBLE);
                view.findViewById(R.id.list_file_selector).setVisibility(View.GONE);
            }
        });
        pathSelector_list_linear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tile.setChecked(false);
                list.setChecked(true);
                pic_text_list.setSelected(true);
                pic_text_tile.setSelected(false);
                adapter = new FileListAdapter(getContext(), R.layout.item_file_list, fileInfoList, FileListAdapter.TYPE_EXPLORER, false);
                listViewpathSelector.setAdapter(adapter);
                view.findViewById(R.id.grid_file_selector).setVisibility(View.GONE);
                view.findViewById(R.id.list_file_selector).setVisibility(View.VISIBLE);
            }
        });

        recyclerViewDirPath = (RecyclerView) view.findViewById(R.id.path_select_view);
        linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerViewDirPath.setLayoutManager(linearLayoutManager);
        pathAdapter = new PathListAdapter(dirList);
        recyclerViewDirPath.setAdapter(pathAdapter);

        imageViewBack = view.findViewById(R.id.image_back_to_device);
        currentRoot = (TextView) view.findViewById(R.id.current_root);
        mLocalDevice = currentDir = getResources().getString(R.string.current_dir);
        currentRoot.setText(mLocalDevice);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dirListView.getVisibility() == View.VISIBLE)
                    dirListView.setVisibility(View.GONE);
                else
                    backToParent();

            }
        });
        dirListView = (ListView) view.findViewById(R.id.historyDir);
        imageViewBack.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                dirListView.setVisibility(View.VISIBLE);
                parentLayout.setVisibility(View.VISIBLE);
                final List<String> historyDir = new ArrayList<>();
                historyDir.clear();
                for (int i = 0; i < historyPath.size(); i++) {
                    historyDir.add(historyPath.get(historyPath.size() - i - 1).path);
                }
                dirListView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        for (int i = 0; i < dirListView.getChildCount(); i++)
                            dirListView.getChildAt(i).findViewById(R.id.historyDiritem).setSelected(true);
                    }
                });
                final HistoryListAdapter historyListAdapter;
                historyListAdapter = new HistoryListAdapter(getContext(), R.layout.history_listview_item, historyDir);
                dirListView.setAdapter(historyListAdapter);
                dirListView.smoothScrollToPosition(0);

                dirListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        currentDir = historyDir.get(position);
                        dirListView.setVisibility(View.GONE);
                        parentLayout.setVisibility(View.GONE);
                        isHistory = true;
                        update();
                        updateDirRecyclerView();
                    }
                });
                return true;
            }
        });
        parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dirListView.setVisibility(View.GONE);
                parentLayout.setVisibility(View.GONE);
            }
        });
        return view;
    }

    private Handler myHandler = new Handler();

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pathAdapter.setOnItemClickListener(new PathListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                String dir = "";
                for (int i = 0; i <= position; i++)
                    if (i == position) {
                        dir = dir + dirList.get(i).path;

                    } else {
                        dir = dir + dirList.get(i).path + "/";
                    }
                currentDir = dir;
                if (currentDir.contains(getResources().getString(R.string.inner_storage))) {
                    currentDir = dir.replace(getResources().getString(R.string.inner_storage), fileInnerPath);
                }
                PathListAdapter.PathData data = new PathListAdapter.PathData();
                data.path = currentDir;
                data.selector = position;
                historyPath.add(data);
                update();

            }
        });
        listViewpathSelector.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 判断文件类型
                FileInfo fileInfo = fileInfoList.get(position);
                if (fileInfo != null) {
                    if (fileInfo.getFileType() == FileInfo.FILE_TYPE_DIR ||
                            fileInfo.getFileType() == FileInfo.FILE_TYPE_INNER_DISK ||
                            fileInfo.getFileType() == FileInfo.FILE_TYPE_U_DISK) {
                        // 目录跳转
                        currentDir = fileInfo.getFilePath();

                        String[] dir;
                        if (currentDir.contains(fileInnerPath)) {
                            dir = currentDir.replace(fileInnerPath, getResources().getString(R.string.inner_storage)).split("/");
                        } else
                            dir = currentDir.split("/");
                        PathListAdapter.PathData data = new PathListAdapter.PathData();
                        data.path = currentDir;
                        data.selector = dir.length - 1;
                        selectorNum = dir.length - 1;
                        historyPath.add(data);
                        isHistory = false;
                        update();
                        updateDirRecyclerView();
                    }
                }
            }
        });
        gridViewpathSelector.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // 判断文件类型
                FileInfo fileInfo = fileInfoList.get(position);
                if (fileInfo != null) {
                    if (fileInfo.getFileType() == FileInfo.FILE_TYPE_DIR ||
                            fileInfo.getFileType() == FileInfo.FILE_TYPE_INNER_DISK ||
                            fileInfo.getFileType() == FileInfo.FILE_TYPE_U_DISK) {
                        // 目录跳转
                        currentDir = fileInfo.getFilePath();
                        String[] dir;
                        if (currentDir.contains(fileInnerPath)) {
                            dir = currentDir.replace(fileInnerPath, getResources().getString(R.string.inner_storage)).split("/");
                        } else
                            dir = currentDir.split("/");
                        PathListAdapter.PathData data = new PathListAdapter.PathData();
                        data.path = currentDir;
                        data.selector = dir.length - 1;
                        selectorNum = dir.length - 1;
                        historyPath.add(data);
                        isHistory = false;
                        update();
                        updateDirRecyclerView();

                    }
                }
            }
        });

        mLocalDevice = currentDir = getResources().getString(R.string.current_dir);
        // 获取内置存储
        File fileInner = LocalFileUtil.getInnerStorage();
        fileInnerPath = fileInner.getAbsolutePath();
        fileInfoInner = new FileInfo();
        if (!fileInnerPath.equals("")) {
            fileInfoInner.setFileType(FileInfo.FILE_TYPE_INNER_DISK);
            fileInfoInner.setFileName(getResources().getString(R.string.inner_storage));
            fileInfoInner.setFilePath(fileInnerPath);
        }
        fileInfoList.add(fileInfoInner);

        fileInfoListUSB.clear();
        fileInfoListUSB.addAll(LocalFileUtil.getUSBDevicesL(getContext()));
        fileInfoList.addAll(fileInfoListUSB);

        adapter.notifyDataSetChanged();

        if (setDir != null && !setDir.isEmpty()) {
            Log.d("paint", "set dir: " + setDir);
            if (new File(setDir).exists()) {
                currentDir = setDir;
                isHistory = false;
                update();
                updateDirRecyclerView();
            }
        }
    }


    private void updateCurrentPath() {
        if (currentDir.contains(fileInnerPath)) {
            // 内置存储
            textViewCurrentPath.setText(currentDir.replace(fileInnerPath, getResources().getString(R.string.inner_storage)));
        } else {
            textViewCurrentPath.setText(currentDir);
        }
    }

    private void setBackIconVisible(boolean v) {
        imageViewBack.setVisibility(v ? View.VISIBLE : View.GONE);
        pathSelector_ok.setVisibility(v ? View.VISIBLE : View.GONE);
    }


    public void backToParent() {
        // 返回上一级
        // 还要加上U盘
//        if (currentDir.contains(getResources().getString(R.string.inner_storage))) {
//            currentDir = currentDir.replace(getResources().getString(R.string.inner_storage), fileInnerPath);
//        }
//        if (imageViewBack.getVisibility() == View.VISIBLE) {
//            if (LocalFileUtil.isRootDir(currentDir)) {
//                // 某个设备的顶层目录，返回设备列表
//                dirList.clear();
//                pathAdapter.notifyDataSetChanged();
//                updateDevices();
//                setBackIconVisible(false);
//            } else {
//                currentDir = LocalFileUtil.getParentPath(currentDir);
//                update();
//                updateDirRecyclerView();
//            }
//        }
        if (historyPath.size() > 0 && !isHistory)
            historyPath.remove(historyPath.size() - 1);
        if (historyPath.size() > 0) {
            currentDir = historyPath.get(historyPath.size() - 1).path;
            selectorNum = historyPath.get(historyPath.size() - 1).selector;
            update();
            isHistory = false;
            updateDirRecyclerView();
            Log.e("currentDir", currentDir);
        } else {
            dirList.clear();
            pathAdapter.notifyDataSetChanged();
            parentDir = "Local Device";
            updateDevices();
            setBackIconVisible(false);
        }
    }

    private void update() {

        List<FileInfo> fileInfoListTemp = LocalFileUtil.getFileList(currentDir, 5);

        fileInfoList.clear();
        if (fileInfoListTemp.size() != 0) {
            fileInfoList.addAll(fileInfoListTemp);
            imageView.setVisibility(View.GONE);
        } else {
            imageView.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();

        updateCurrentPath();
        setBackIconVisible(true);
    }

    private void updateDirRecyclerView() {
        if (!isHistory)
            if (parentDir.contains(currentDir + "/")) {
                currentDir = parentDir;
            } else {
                parentDir = currentDir;
            }
        else
            parentDir = currentDir;
        dirList.clear();
        String[] dir;
        if (currentDir.contains(fileInnerPath)) {
            dir = currentDir.replace(fileInnerPath, getResources().getString(R.string.inner_storage)).split("/");
        } else
            dir = currentDir.split("/");

        for (int i = 0; i < dir.length; i++) {
            PathListAdapter.PathData data = new PathListAdapter.PathData();
            data.path = dir[i];
            data.selector = 0;
            if (-1 == selectorNum) {
                if (i == dir.length - 1) {
                    data.selector = 1;
                    selectorNum = -1;
                }
            } else {
                if (i == selectorNum) {
                    data.selector = 1;
                }
            }

            dirList.add(data);
        }
        if (dirList.size() > 1)
            recyclerViewDirPath.smoothScrollToPosition(dirList.size() - 1);
        pathAdapter.notifyDataSetChanged();

    }

    public void setDir(String path) {
        setDir = path;
    }

    public void updateDevices() {
        currentDir = mLocalDevice;//getResources().getString(R.string.current_dir);
        updateCurrentPath();
        fileInfoList.clear();
        fileInfoList.add(fileInfoInner);

        fileInfoListUSB.clear();
        fileInfoListUSB.addAll(LocalFileUtil.getUSBDevicesL(getContext()));
        fileInfoList.addAll(fileInfoListUSB);

        adapter.notifyDataSetChanged();
    }
//    public void setRecyclerViewSelector(int position){
//        for(int i =0;i<dirList.size();i++) {
//            View v = linearLayoutManager.findViewByPosition(i);
//            TextView dirName = (TextView) v.findViewById(R.id.dir_name);
//            if(i==position) {
//                dirName.setTextColor(getActivity().getResources().getColor(R.color.text_pressed_color));
//            }else {
//                dirName.setTextColor(getActivity().getResources().getColor(R.color.text_normal_color));
//            }
//        }
//    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

//    @Override
//    public void onUpdate() {
//        updateDevices();
//    }
}
