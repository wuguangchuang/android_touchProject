package com.newskyer.meetingpad.fileselector.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.util.Log;

import com.newskyer.meetingpad.fileselector.activity.FileSelectActivity;
import com.newskyer.meetingpad.fileselector.file.model.FileInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author liziyang
 * @since 2018/1/26
 */
public class MediaFileTool {

    public static final int MEDIA_TYPE_PICTURE = 0;
    public static final int MEDIA_TYPE_MUSIC = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int MEDIA_TYPE_DOC = 3;
    public static final int MEDIA_TYPE_NCC = 4;
    public static final int MEDIA_TYPE_PDF = 5;

    public static String BRAND = "note";
    public static void setBrand(String brand) {
        BRAND = brand;
    }
    private Context context;
    private int mediaType;

    public MediaFileTool(Context context, int mediaType) {
        this.context = context;
        this.mediaType = mediaType;
    }

    private void addNoteFile(File dir, List<FileInfo> infos) {
        if (!dir.exists() || !dir.canRead())
            return;
        File[] files = dir.listFiles();
        FileInfo fileInfo = new FileInfo();
        String name = "";
        if (files.length == 0)
            return;
        for (File file : files) {
            if (file.isDirectory()) {
                addNoteFile(file, infos);
            } else {
                name = file.getName();
                if (name.endsWith(".bin")) {
                    fileInfo = new FileInfo();
                    fileInfo.setFileName(name);
                    fileInfo.setFilePath(file.getAbsolutePath());

                    infos.add(fileInfo);
                }
            }
        }
    }

    public final String TAG = "myFileText";
    private StorageManager mStorageManager;
    public List<FileInfo> getMediaFileList() {

        if(!FileSelectActivity.readPermission)
        {
            Log.e(TAG, "getMediaFileList: 没有读取的权限");
            while (true)
            {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(FileSelectActivity.readPermission)
                {
                    break;
                }
            }
        }
        List<FileInfo> fileInfoList = new ArrayList<>();
        String fileName = null;
        String filePath = null;
        if (mediaType == MEDIA_TYPE_NCC) {
            mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            StorageVolume[] storageVolumes = StorageManagerUtils.getVolumeList(context);
            if (storageVolumes != null) {
                for (StorageVolume volume : storageVolumes) {
                    final String path = StorageManagerUtils.getPath(volume);
                    String noteDir = path + "/note";
                    addNoteFile(new File(noteDir), fileInfoList);
                    if (!BRAND.equals("note")) {
                        String ccDir = path + "/" + BRAND;
                        addNoteFile(new File(ccDir), fileInfoList);
                    }
                }
            }
            return fileInfoList;
        }

        String[] projection = null;
        switch (mediaType) {
            case MEDIA_TYPE_PICTURE:
                projection = new String[]{
                        MediaStore.Images.ImageColumns.DISPLAY_NAME,
                        MediaStore.Images.ImageColumns.DATA
                };
                break;
            case MEDIA_TYPE_MUSIC:
                projection = new String[]{
                        MediaStore.Audio.AudioColumns.DISPLAY_NAME,
                        MediaStore.Audio.AudioColumns.DATA
                };
                break;
            case MEDIA_TYPE_VIDEO:
                projection = new String[]{
                        MediaStore.Video.VideoColumns.DISPLAY_NAME,
                        MediaStore.Video.VideoColumns.DATA
                };
                break;
            case MEDIA_TYPE_DOC:
                projection = new String[]{
                        MediaStore.Files.FileColumns.DISPLAY_NAME,
                        MediaStore.Files.FileColumns.DATA
                };
                break;
            case MEDIA_TYPE_NCC:
                projection = new String[]{
                        MediaStore.Files.FileColumns.DATA
                };
                break;
        }


        Cursor cursor = null;
        switch (mediaType) {
            case MEDIA_TYPE_PICTURE:
                cursor = context.getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection, null, null,
                        MediaStore.Images.ImageColumns.DISPLAY_NAME + " asc"
                );
                break;
            case MEDIA_TYPE_MUSIC:
                cursor = context.getContentResolver().query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection, null, null,
                        MediaStore.Audio.AudioColumns.DISPLAY_NAME + " asc"
                );
                break;
            case MEDIA_TYPE_VIDEO:
                cursor = context.getContentResolver().query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        projection, null, null,
                        MediaStore.Video.VideoColumns.DISPLAY_NAME + " asc"
                );
                break;
            case MEDIA_TYPE_PDF: {
                cursor = context.getContentResolver().query(
                        Uri.parse("content://media/external/file"),
                        projection,
                        MediaStore.Files.FileColumns.DATA + " like ?",
                        new String[]{"%.bin"},
                        MediaStore.Files.FileColumns.TITLE + " asc"
                );
                break;
            }
            case MEDIA_TYPE_DOC:
                String selection = MediaStore.Files.FileColumns.DATA + " like ?";
                String[] selectionArgs = new String[]{
                        "%.bin",
                };

                cursor = context.getContentResolver().query(
                        Uri.parse("content://media/external/file"),
                        projection,
                        selection, selectionArgs,
                        MediaStore.Files.FileColumns.TITLE + " asc"
                );
                break;
            case MEDIA_TYPE_NCC:
                cursor = context.getContentResolver().query(
                        Uri.parse("content://media/external/file"),
                        projection,
                        MediaStore.Files.FileColumns.DATA + " like ?",
                        new String[]{"%.bin"},
                        MediaStore.Files.FileColumns.TITLE + " asc"
                );
                break;
        }


        if (cursor == null) {
            return fileInfoList;
        }

        while (cursor.moveToNext()) {
            FileInfo fileInfo = new FileInfo();

            switch (mediaType) {
                case MEDIA_TYPE_PICTURE:
                    fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME));
                    filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
                    break;
                case MEDIA_TYPE_MUSIC:
                    fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DISPLAY_NAME));
                    filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA));
                    break;
                case MEDIA_TYPE_VIDEO:
                    fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME));
                    filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA));
                    break;
                case MEDIA_TYPE_DOC:
                case MEDIA_TYPE_NCC:
                    filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                    int index = filePath.lastIndexOf("/");
                    fileName = filePath.substring(index + 1);
                    break;
            }

            fileInfo.setFileName(fileName);
            fileInfo.setFilePath(filePath);

            fileInfoList.add(fileInfo);
        }

        cursor.close();
        cursor = null;

        return fileInfoList;
    }



}
