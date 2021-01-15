package com.newskyer.meetingpad.fileselector.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
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

import com.newskyer.meetingpad.R;
import com.newskyer.meetingpad.fileselector.activity.FileSelectActivity;
import com.newskyer.meetingpad.fileselector.file.adapter.FileListAdapter;
import com.newskyer.meetingpad.fileselector.file.model.FileInfo;
import com.newskyer.meetingpad.fileselector.util.MediaFileTool;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.Fragment;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * @author liziyang
 * @since 2018/1/24
 */
public class NccFragment extends Fragment {

    private ListView listViewNcc;
    private GridView gridViewNcc;

    private List<FileInfo> fileInfoList = new ArrayList<>();
    private FileListAdapter adapter;

    private MediaFileTool mediaFileTool;
    private RadioButton tile, list;
    private  ImageView imageView;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_ncc, container, false);
        listViewNcc = (ListView) view.findViewById(R.id.list_file_selector_ncc);
        gridViewNcc = (GridView) view.findViewById(R.id.grid_file_selector_ncc);
        LinearLayout listlinear = (LinearLayout) view.findViewById(R.id.ncc_list_linear);
        LinearLayout tilelinear = (LinearLayout) view.findViewById(R.id.ncc_tile_linear);
        final TextView ncc_text_list = (TextView) view.findViewById(R.id.ncc_text_list);
        final TextView ncc_text_tile = (TextView) view.findViewById(R.id.ncc_text_tile);
        tile = (RadioButton) view.findViewById(R.id.ncc_icon_tile);
        list = (RadioButton) view.findViewById(R.id.ncc_icon_list);

        imageView = (ImageView) view.findViewById(R.id.ncc_image_empty);

        adapter = new FileListAdapter(getContext(), R.layout.item_file_grid, fileInfoList, FileListAdapter.TYPE_NCC, true);
        gridViewNcc.setAdapter(adapter);
        ncc_text_tile.setSelected(true);
        tilelinear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list.setChecked(false);
                tile.setChecked(true);
                ncc_text_tile.setSelected(true);
                ncc_text_list.setSelected(false);
                adapter = new FileListAdapter(getContext(), R.layout.item_file_grid, fileInfoList, FileListAdapter.TYPE_NCC, true);
                gridViewNcc.setAdapter(adapter);
                gridViewNcc.setVisibility(View.VISIBLE);
                listViewNcc.setVisibility(View.GONE);
            }
        });
        listlinear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tile.setChecked(false);
                list.setChecked(true);
                ncc_text_list.setSelected(true);
                ncc_text_tile.setSelected(false);
                        Log.d("paint", "ncc file list: " + fileInfoList.size());
                adapter = new FileListAdapter(getContext(), R.layout.item_file_list, fileInfoList, FileListAdapter.TYPE_NCC, false);
                listViewNcc.setAdapter(adapter);
                gridViewNcc.setVisibility(View.GONE);
                listViewNcc.setVisibility(View.VISIBLE);
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mediaFileTool = new MediaFileTool(getContext(), MediaFileTool.MEDIA_TYPE_NCC);

        listViewNcc.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 选择文件，返回路径
                Intent intent = new Intent();
                intent.putExtra(FileSelectActivity.FILE_PATH, fileInfoList.get(position).getFilePath());
                intent.putExtra(FileSelectActivity.FILE_NAME, fileInfoList.get(position).getFileName());
                getActivity().setResult(Activity.RESULT_OK, intent);
                getActivity().setIntent(intent);
                getActivity().finish();
            }
        });
        gridViewNcc.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 选择文件，返回路径
                Intent intent = new Intent();
                intent.putExtra(FileSelectActivity.FILE_PATH, fileInfoList.get(position).getFilePath());
                intent.putExtra(FileSelectActivity.FILE_NAME, fileInfoList.get(position).getFileName());

                getActivity().setResult(Activity.RESULT_OK, intent);
                getActivity().setIntent(intent);
                getActivity().finish();
            }
        });

        Observable
                .create(new ObservableOnSubscribe<List<FileInfo>>() {
                    @Override
                    public void subscribe(ObservableEmitter<List<FileInfo>> emitter) throws Exception {
                        emitter.onNext(mediaFileTool.getMediaFileList());
                        emitter.onComplete();
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<FileInfo>>() {
                    @Override
                    public void accept(List<FileInfo> fileInfoList) throws Exception {
                        NccFragment.this.fileInfoList.clear();

                        if (fileInfoList.size() != 0) {
                            NccFragment.this.fileInfoList.addAll(fileInfoList);
                            imageView.setVisibility(View.GONE);
                        } else
                            imageView.setVisibility(View.VISIBLE);
                        NccFragment.this.adapter.notifyDataSetChanged();
                    }
                });
    }
}
