package com.newskyer.meetingpad.fileselector.file.adapter;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.newskyer.meetingpad.R;
import com.newskyer.meetingpad.fileselector.file.model.FileInfo;
import com.zhy.adapter.abslistview.CommonAdapter;
import com.zhy.adapter.abslistview.ViewHolder;

import java.util.List;

/**
 * @author liziyang
 * @since 2018/1/24
 */
public class FileListAdapter extends CommonAdapter<FileInfo> {

    private int type;
    private boolean tile;
    public static final int TYPE_EXPLORER = 0;
    public static final int TYPE_PICTURE = 1;
    public static final int TYPE_MUSIC = 2;
    public static final int TYPE_VIDEO = 3;
    public static final int TYPE_DOC = 4;
    public static final int TYPE_NCC = 5;

    private Context context;


    public FileListAdapter(Context context, int layoutId, List<FileInfo> fileInfoList, int type, boolean tile) {
        super(context, layoutId, fileInfoList);
        this.context = context;
        this.type = type;
        this.tile = tile;
    }

    @Override
    protected void convert(ViewHolder viewHolder, FileInfo item, int position) {
        viewHolder.setText(R.id.text_file_name, item.getFileName());
        if (!tile)
            viewHolder.setText(R.id.text_file_path, item.getFilePath());

        switch (type) {
            case TYPE_EXPLORER:
                // 目录
                viewHolder.setVisible(R.id.text_file_path, false);
                // 目录里的文件类型
                switch (item.getFileType()) {
                    case FileInfo.FILE_TYPE_INNER_DISK:
                        viewHolder.setImageResource(R.id.image_file_image, R.mipmap.ic_inner_storage);
                        break;
                    case FileInfo.FILE_TYPE_U_DISK:
                        viewHolder.setImageResource(R.id.image_file_image, R.mipmap.ic_u_disk);
                        break;
                    case FileInfo.FILE_TYPE_DIR:
                        viewHolder.setImageResource(R.id.image_file_image, R.mipmap.ic_file_tab_category);
                        break;
                    case FileInfo.FILE_TYPE_PICTURE:
                        if (item != null)
                            showPicture(viewHolder, item, false);
                        else

                        break;
                    case FileInfo.FILE_TYPE_MUSIC:
                        viewHolder.setImageResource(R.id.image_file_image, R.mipmap.ic_file_tab_music);
                        break;
                    case FileInfo.FILE_TYPE_VIDEO:
                        // 显示缩略图
                        showPicture(viewHolder, item, true);
                        break;
                    case FileInfo.FILE_TYPE_DOC:
                        viewHolder.setImageResource(R.id.image_file_image, R.mipmap.ic_file_tab_bin);
                        break;
                    case FileInfo.FILE_TYPE_NCC:
                        viewHolder.setImageResource(R.id.image_file_image, R.mipmap.ic_file_tab_ncc);
                        break;
                }
                break;
            case TYPE_PICTURE:
                if (!tile)
                    viewHolder.setVisible(R.id.text_file_path, true);
                showPicture(viewHolder, item, false);
                break;
            case TYPE_MUSIC:
                if (!tile)
                    viewHolder.setVisible(R.id.text_file_path, true);
                viewHolder.setImageResource(R.id.image_file_image, R.mipmap.ic_file_tab_music);
                break;
            case TYPE_VIDEO:
                if (!tile)
                    viewHolder.setVisible(R.id.text_file_path, true);
                // 显示缩略图
                showPicture(viewHolder, item, true);
                break;
            case TYPE_DOC:
                if (!tile)
                    viewHolder.setVisible(R.id.text_file_path, true);
                viewHolder.setImageResource(R.id.image_file_image, R.mipmap.ic_file_tab_bin);
                break;
            case TYPE_NCC:
                if (!tile)
                    viewHolder.setVisible(R.id.text_file_path, true);
                viewHolder.setImageResource(R.id.image_file_image, R.mipmap.ic_file_tab_ncc);
                break;
        }
    }

    private void showPicture(ViewHolder viewHolder, FileInfo item, boolean isVideo) {
        Glide.with(context)
                .load(item.getFilePath())
                .placeholder(isVideo ? R.mipmap.ic_file_tab_video : R.mipmap.ic_file_tab_picture)
                .centerCrop()
                .into((ImageView) viewHolder.getView(R.id.image_file_image));
    }

    private void showEmty(ViewHolder viewHolder, FileInfo item, boolean isVideo) {

    }
}
