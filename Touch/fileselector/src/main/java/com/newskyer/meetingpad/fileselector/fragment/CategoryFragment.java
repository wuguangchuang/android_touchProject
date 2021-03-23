package com.newskyer.meetingpad.fileselector.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.newskyer.meetingpad.R;
import com.newskyer.meetingpad.fileselector.activity.FileSelectActivity;
import com.newskyer.meetingpad.fileselector.file.adapter.FileListAdapter;
import com.newskyer.meetingpad.fileselector.file.adapter.PathListAdapter;
import com.newskyer.meetingpad.fileselector.file.model.FileInfo;
import com.newskyer.meetingpad.fileselector.util.LocalFileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.newskyer.meetingpad.fileselector.activity.FileSelectActivity.SELECT_ITEM_DOC;
import static com.newskyer.meetingpad.fileselector.activity.FileSelectActivity.SELECT_ITEM_MUSIC;
import static com.newskyer.meetingpad.fileselector.activity.FileSelectActivity.SELECT_ITEM_NCC;
import static com.newskyer.meetingpad.fileselector.activity.FileSelectActivity.SELECT_ITEM_PICTURE;
import static com.newskyer.meetingpad.fileselector.activity.FileSelectActivity.SELECT_ITEM_VIDEO;
import static com.newskyer.meetingpad.fileselector.activity.FileSelectActivity.SELECT_TYPE_DOC;
import static com.newskyer.meetingpad.fileselector.activity.FileSelectActivity.SELECT_TYPE_MUSIC;
import static com.newskyer.meetingpad.fileselector.activity.FileSelectActivity.SELECT_TYPE_NCC;
import static com.newskyer.meetingpad.fileselector.activity.FileSelectActivity.SELECT_TYPE_PICTURE;
import static com.newskyer.meetingpad.fileselector.activity.FileSelectActivity.SELECT_TYPE_VIDEO;


/**
 * @author liziyang
 * @since 2018/1/24
 */
public class CategoryFragment extends Fragment {

    private ListView listViewCategory;
    private GridView gridViewCategory;


    private List<FileInfo> fileInfoList = new ArrayList<>();
    private FileListAdapter adapter;

    private String mLocalDevice = "";
    private String currentDir = "Local Device";//getResources().getString(R.string.current_dir);
    private TextView textViewCurrentPath;
    private String setDir = "";

    private View imageViewBack;

    private String fileInnerPath;
    private FileInfo fileInfoInner;

    private List<FileInfo> fileInfoListUSB = new ArrayList<>();
    private RadioButton tile, list;
    private ImageView imageView;
    private RecyclerView dirRecyclerView;
    private List<PathListAdapter.PathData> dirList = new ArrayList<>();
    private PathListAdapter pathAdapter;
    private List<PathListAdapter.PathData> historyPath = new ArrayList<>();
    private String parentDir = "Local Device";
    private int selectorNum = -1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_category, container, false);
        listViewCategory = (ListView) view.findViewById(R.id.list_file_selector_category);
        gridViewCategory = (GridView) view.findViewById(R.id.grid_file_selector_category);

        textViewCurrentPath = (TextView) view.findViewById(R.id.text_file_current_path);
        imageView = (ImageView) view.findViewById(R.id.categorg_image_empty);
        LinearLayout categorg_list_linear = (LinearLayout) view.findViewById(R.id.categorg_list_linear);
        LinearLayout categorg_tile_linear = (LinearLayout) view.findViewById(R.id.categorg_tile_linear);
        final TextView pic_text_list = (TextView) view.findViewById(R.id.categorg_text_list);
        final TextView pic_text_tile = (TextView) view.findViewById(R.id.categorg_text_tile);
        tile = (RadioButton) view.findViewById(R.id.categorg_icon_tile);
        list = (RadioButton) view.findViewById(R.id.categorg_icon_list);
        pic_text_tile.setSelected(true);
        adapter = new FileListAdapter(getContext(), R.layout.item_file_grid, fileInfoList, FileListAdapter.TYPE_EXPLORER, true);
        gridViewCategory.setAdapter(adapter);
        categorg_tile_linear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list.setChecked(false);
                tile.setChecked(true);
                pic_text_tile.setSelected(true);
                pic_text_list.setSelected(false);
                adapter = new FileListAdapter(getContext(), R.layout.item_file_grid, fileInfoList, FileListAdapter.TYPE_EXPLORER, true);
                gridViewCategory.setAdapter(adapter);
                view.findViewById(R.id.grid_file_selector_category).setVisibility(View.VISIBLE);
                view.findViewById(R.id.list_file_selector_category).setVisibility(View.GONE);
            }
        });
        categorg_list_linear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tile.setChecked(false);
                list.setChecked(true);
                pic_text_list.setSelected(true);
                pic_text_tile.setSelected(false);
                adapter = new FileListAdapter(getContext(), R.layout.item_file_list, fileInfoList, FileListAdapter.TYPE_EXPLORER, false);
                listViewCategory.setAdapter(adapter);
                view.findViewById(R.id.grid_file_selector_category).setVisibility(View.GONE);
                view.findViewById(R.id.list_file_selector_category).setVisibility(View.VISIBLE);
            }
        });
        imageViewBack = view.findViewById(R.id.image_back_to_parent);
//        mLocalDevice = currentDir = getResources().getString(R.string.current_dir);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backToParent();
            }
        });

        dirRecyclerView = (RecyclerView) view.findViewById(R.id.category_path_select_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        dirRecyclerView.setLayoutManager(linearLayoutManager);
        pathAdapter = new PathListAdapter(dirList);
        dirRecyclerView.setAdapter(pathAdapter);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pathAdapter.setOnItemClickListener(new PathListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String dir = "";
                for (int i = 0; i <= position; i++)
                    if (i == position)
                        dir = dir + dirList.get(i).path;
                    else
                        dir = dir + dirList.get(i).path + "/";
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
        listViewCategory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 判断文件类型
                FileInfo fileInfo = fileInfoList.get(position);
                if (fileInfo != null) {
                    if (fileInfo.getFileType() == FileInfo.FILE_TYPE_DIR ||
                            fileInfo.getFileType() == FileInfo.FILE_TYPE_INNER_DISK ||
                            fileInfo.getFileType() == FileInfo.FILE_TYPE_U_DISK) {
                        // 目录跳转
                        parentDir = currentDir = fileInfo.getFilePath();
                        String[] dir;
                        if (currentDir.contains(fileInnerPath)) {
                            dir = currentDir.replace(fileInnerPath, getResources().getString(R.string.inner_storage)).split("/");
                        } else
                            dir = currentDir.split("/");
                        selectorNum = -1;
                        PathListAdapter.PathData data = new PathListAdapter.PathData();
                        data.path = currentDir;
                        data.selector = dir.length - 1;
                        historyPath.add(data);

                        update();
                        updateDirRecyclerView();
                    } else {
                        // 选择文件，返回路径
                        Intent intent = new Intent();
                        intent.putExtra(FileSelectActivity.FILE_PATH, fileInfo.getFilePath());
                        intent.putExtra(FileSelectActivity.FILE_NAME, fileInfoList.get(position).getFileName());
                        getActivity().setResult(Activity.RESULT_OK, intent);
                        getActivity().setIntent(intent);
                        getActivity().finish();
                    }
                }
            }
        });
        gridViewCategory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 判断文件类型
                FileInfo fileInfo = fileInfoList.get(position);
                if (fileInfo != null) {
                    if (fileInfo.getFileType() == FileInfo.FILE_TYPE_DIR ||
                            fileInfo.getFileType() == FileInfo.FILE_TYPE_INNER_DISK ||
                            fileInfo.getFileType() == FileInfo.FILE_TYPE_U_DISK) {
                        // 目录跳转
                        parentDir = currentDir = fileInfo.getFilePath();
                        String[] dir;
                        if (currentDir.contains(fileInnerPath)) {
                            dir = currentDir.replace(fileInnerPath, getResources().getString(R.string.inner_storage)).split("/");
                        } else
                            dir = currentDir.split("/");
                        selectorNum = -1;
                        PathListAdapter.PathData data = new PathListAdapter.PathData();
                        data.path = currentDir;
                        data.selector = dir.length - 1;
                        historyPath.add(data);
                        update();
                        updateDirRecyclerView();
                    } else {
                        // 选择文件，返回路径
                        Intent intent = new Intent();
                        intent.putExtra(FileSelectActivity.FILE_PATH, fileInfo.getFilePath());
                        intent.putExtra(FileSelectActivity.FILE_NAME, fileInfoList.get(position).getFileName());
                        getActivity().setResult(Activity.RESULT_OK, intent);
                        getActivity().setIntent(intent);
                        getActivity().finish();
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
        Log.d("U盘","111111=================================================");
        fileInfoListUSB.addAll(LocalFileUtil.getUSBDevicesL(getContext()));
        fileInfoList.addAll(fileInfoListUSB);

        adapter.notifyDataSetChanged();

        if (setDir != null && !setDir.isEmpty()) {
            Log.d("paint", "set dir: " + setDir);
            if (new File(setDir).exists()) {
                currentDir = setDir;
                update();
                updateDirRecyclerView();
            }
        }
    }


    private void updateCurrentPath() {

        if(historyPath.size() <= 0)
        {
            textViewCurrentPath.setText(getResources().getString(R.string.current_device));
            return;
        }
        if (currentDir.contains(fileInnerPath)) {
            // 内置存储
            textViewCurrentPath.setText(getResources().getString(R.string.inner_storage));
        } else {
            textViewCurrentPath.setText(getResources().getString(R.string.current_dir));
        }
    }

    private void setBackIconVisible(boolean v) {
        imageViewBack.setVisibility(v ? View.VISIBLE : View.GONE);
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
//
//            } else {
//                currentDir = LocalFileUtil.getParentPath(currentDir);
//                update();
//                updateDirRecyclerView();
//            }
//        }
        if (historyPath.size() > 0)
            historyPath.remove(historyPath.size() - 1);
        if (historyPath.size() > 0) {
            currentDir = historyPath.get(historyPath.size() - 1).path;
            selectorNum = historyPath.get(historyPath.size() - 1).selector;
            update();
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

    private List<FileInfo> getFileInfoList(int type) {
        return LocalFileUtil.getFileList(currentDir, type);
    }
    private void update() {
    	int items = ((FileSelectActivity)getActivity()).getSelectItems();
        fileInfoList.clear();
        if ((items & SELECT_ITEM_PICTURE) != 0) {
            fileInfoList.addAll(getFileInfoList(SELECT_TYPE_PICTURE));
        }
        if ((items & SELECT_ITEM_MUSIC) != 0) {
            fileInfoList.addAll(getFileInfoList(SELECT_TYPE_MUSIC));
        }
        if ((items & SELECT_ITEM_VIDEO) != 0) {
            fileInfoList.addAll(getFileInfoList(SELECT_TYPE_VIDEO));
        }
        if ((items & SELECT_ITEM_DOC) != 0) {
            fileInfoList.addAll(getFileInfoList(SELECT_TYPE_DOC));
        }
        if ((items & SELECT_ITEM_NCC) != 0) {
            fileInfoList.addAll(getFileInfoList(SELECT_TYPE_NCC));
        }
        if (fileInfoList.size() != 0) {
            imageView.setVisibility(View.GONE);
        } else {
            imageView.setVisibility(View.VISIBLE);
        }
        Log.d("paint", "## " + fileInfoList.size());
        adapter.notifyDataSetChanged();
        updateCurrentPath();
        setBackIconVisible(true);
    }

    private void updateDirRecyclerView() {
        if (parentDir.contains(currentDir + "/")) {
            currentDir = parentDir;
        } else {
            parentDir = currentDir;
        }
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
            dirRecyclerView.smoothScrollToPosition(dirList.size() - 1);
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
        Log.d("U盘","222222=================================================");
        fileInfoListUSB.addAll(LocalFileUtil.getUSBDevicesL(getContext()));
        fileInfoList.addAll(fileInfoListUSB);

        adapter.notifyDataSetChanged();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

//    @Override
//    public void onUpdate() {
//        updateDevices();
//    }
}
