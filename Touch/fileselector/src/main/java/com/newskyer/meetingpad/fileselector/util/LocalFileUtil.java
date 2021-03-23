package com.newskyer.meetingpad.fileselector.util;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import com.newskyer.meetingpad.R;
import com.newskyer.meetingpad.fileselector.activity.FileSelectActivity;
import com.newskyer.meetingpad.fileselector.file.model.FileInfo;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * @author liziyang
 * @since 2018/1/25
 */
public class LocalFileUtil {

    private static final ArrayList<String> FILE_TYPES_PICTURE = new ArrayList<String>() {{
        add("bmp");
        add("jpg");
        add("jpeg");
        add("png");
        add("tiff");
        add("gif");
    }};

    private static final ArrayList<String> FILE_TYPES_MUSIC = new ArrayList<String>() {{
        add("mp3");
        add("ogg");
        add("acc");
        add("wma");
        add("wav");
        add("ape");
        add("flac");
    }};

    private static final ArrayList<String> FILE_TYPES_VIDEO = new ArrayList<String>() {{
        add("mp4");
        add("rmvb");
        add("mkv");
        add("avi");
        add("rm");
        add("flv");
        add("swf");
    }};

    private static final ArrayList<String> FILE_TYPES_DOC = new ArrayList<String>() {{
        add("txt");
        add("ppt");
        add("pptx");
        add("doc");
        add("docx");
        add("xls");
        add("xlsx");
        add("bin");
    }};

    private static final ArrayList<String> FILE_TYPES_NCC = new ArrayList<String>() {{
        add("ncc");
    }};
    private static final ArrayList<String> FILE_TYPES_PDF = new ArrayList<String>() {{
        add("bin");
    }};
    public static final ArrayList<String> FILE_TYPES_NZ = new ArrayList<String>() {{
        add("nz");
        add("np");
        add("ncc");
    }};


//    private static final ArrayList<String> FILE_TYPES = new ArrayList<>();
//
//    public LocalFileUtil() {
//        FILE_TYPES.addAll(FILE_TYPES_PICTURE);
//        FILE_TYPES.addAll(FILE_TYPES_MUSIC);
//        FILE_TYPES.addAll(FILE_TYPES_VIDEO);
//        FILE_TYPES.addAll(FILE_TYPES_DOC);
//        FILE_TYPES.addAll(FILE_TYPES_NCC);
//    }
    /**
     * 支持的文件格式
     */
    private static final ArrayList<String> FILE_TYPES = new ArrayList<String>() {{
        // 图片
        add("bmp");
        add("jpg");
        add("jpeg");
        add("png");
        add("tiff");
        add("gif");

        // 音频
        add("mp3");
        add("ogg");
        add("acc");
        add("wma");
        add("wav");
        add("ape");
        add("flac");

        // 视频
        add("mp4");
        add("rmvb");
        add("mkv");
        add("avi");
        add("rm");
        add("flv");
        add("swf");

        // 文档
        add("txt");
        add("ppt");
        add("pptx");
        add("doc");
        add("docx");
        add("xls");
        add("xlsx");
        add("bin");

        // 笔迹
        add("ncc");
        // 压缩文件
        add("nz");
        add("np");
    }};

    public static File getInnerStorage() {
        return Environment.getExternalStorageDirectory();
    }


    /**
     * 获取指定路径下的 FileInfo List
     *
     * @param path
     * @return
     */
    private static boolean mOrderByDate = true;
    private static String mKeyWord = null;
    public static List<FileInfo> getFileList(String path, int type,boolean orderByDate,String keyWord) {
        mOrderByDate = orderByDate;
        mKeyWord = keyWord;
        return getFileList(path, type);
    }
    public static List<FileInfo> getFileList(String path, int type,boolean orderByDate) {
        mOrderByDate = orderByDate;
        return getFileList(path, type);
    }
    public static List<FileInfo> getFileList(String path, FilenameFilter filenameFilter, boolean orderByDate, boolean hasHidden) {
        List<FileInfo> fileInfoList = new ArrayList<>();

        File fileCurrentPath = new File(path);
        String[] files = null;

        files = fileCurrentPath.list(filenameFilter);

        if (files == null) {
            return fileInfoList;
        }

        for (String file : files) {
            if (hasHidden || !file.startsWith(".")) {
                FileInfo fileInfo = getFileInfo(makePath(path, file));
                if (fileInfo != null) {
                    fileInfoList.add(fileInfo);
                }
            }
        }
        if(orderByDate){
            //按时间排序
            Collections.sort(fileInfoList, new Comparator<FileInfo>() {
                public int compare(FileInfo f1, FileInfo f2) {
                    long diff = f1.getLastModified() - f2.getLastModified();
                    if (diff > 0)
                        return -1;
                    else if (diff == 0)
                        return 0;
                    else
                        return 1;

                }
            });
        }else {
            // 字母排序
            Collections.sort(fileInfoList, new Comparator<FileInfo>() {
                @Override
                public int compare(FileInfo lhs, FileInfo rhs) {
                    if (lhs.isDir() && !rhs.isDir()) {
                        return -1;
                    } else if (!lhs.isDir() && rhs.isDir()) {
                        return 1;
                    }
                    return String.valueOf(PinYinUtil.getPingYin(lhs.getFileName()))
                            .compareTo(String.valueOf(PinYinUtil.getPingYin(rhs.getFileName())));
                }
            });
        }
        return fileInfoList;
    }
    public static List<FileInfo> getFileList(String path, int type) {
        List<FileInfo> fileInfoList = new ArrayList<>();

        Log.d("2222", "╔════════════════════════════════════════════════");
        Log.d("2222", "║  getFileList(), Thread : " + Thread.currentThread().getName());
        Log.d("2222", "║    path=" + path);
        Log.d("2222", "╚════════════════════════════════════════════════");
        File fileCurrentPath = new File(path);
        String[] files = null;

        Log.d("2222", "╔════════════════════════════════════════════════");
        Log.d("2222", "║  getFileList(), Thread : " + Thread.currentThread().getName());
        Log.d("2222", "║    type : " + type);
        Log.d("2222", "╚════════════════════════════════════════════════");
        switch (type) {
            case FileSelectActivity.SELECT_TYPE_ALL:
                files = fileCurrentPath.list(filenameFilter);
                break;
            case FileSelectActivity.SELECT_TYPE_PICTURE:
                files = fileCurrentPath.list(filenameFilterPicture);
                break;
            case FileSelectActivity.SELECT_TYPE_MUSIC:
                files = fileCurrentPath.list(filenameFilterMusic);
                break;
            case FileSelectActivity.SELECT_TYPE_VIDEO:
                files = fileCurrentPath.list(filenameFilterVideo);
                break;
            case FileSelectActivity.SELECT_TYPE_DOC:
                files = fileCurrentPath.list(filenameFilterDoc);
                break;
            case FileSelectActivity.SELECT_TYPE_PDF:
                files = fileCurrentPath.list(filenameFilterPDF);
                break;
            case FileSelectActivity.SELECT_TYPE_NCC:
                files = fileCurrentPath.list(filenameFilterNcc);
                break;
            case FileSelectActivity.SELECT_TYPE_KEYWORD:
                files = fileCurrentPath.list(filenameFilterKeyword);
                break;
            case FileSelectActivity.SELECT_TYPE_NZ:
                files = fileCurrentPath.list(filenameFilterNz);
                break;
            case FileSelectActivity.SELECT_TYPE_DIR:
                files = fileCurrentPath.list(dirnameFilterNz);
                break;

        }


        if (files == null) {
            return fileInfoList;
        }

        Log.d("2222", "╔════════════════════════════════════════════════");
        Log.d("2222", "║  getFileList(), Thread : " + Thread.currentThread().getName());
        Log.d("2222", "║    files" + files.toString());
        Log.d("2222", "╚════════════════════════════════════════════════");

        for (String file : files) {
            if (!file.startsWith(".")) {
                FileInfo fileInfo = getFileInfo(makePath(path, file));
                if (fileInfo != null) {
                    fileInfoList.add(fileInfo);
                }
            }
        }
        if(mOrderByDate){
            //按时间排序
            Collections.sort(fileInfoList, new Comparator<FileInfo>() {
                public int compare(FileInfo f1, FileInfo f2) {
                    long diff = f1.getLastModified() - f2.getLastModified();
                    if (diff > 0)
                        return -1;
                    else if (diff == 0)
                        return 0;
                    else
                        return 1;

                }
            });
        }else {
            // 字母排序
            Collections.sort(fileInfoList, new Comparator<FileInfo>() {
                @Override
                public int compare(FileInfo lhs, FileInfo rhs) {
                    if (lhs.isDir() && !rhs.isDir()) {
                        return -1;
                    } else if (!lhs.isDir() && rhs.isDir()) {
                        return 1;
                    }
                    return String.valueOf(PinYinUtil.getPingYin(lhs.getFileName()))
                            .compareTo(String.valueOf(PinYinUtil.getPingYin(rhs.getFileName())));
                }
            });
        }
        return fileInfoList;
    }

    /**
     * 根据文件路径获取File 再包装成FileInfo对象
     *
     * @param filePath
     * @return
     */
    public static FileInfo getFileInfo(String filePath) {
        File lFile = new File(filePath);
        if (!lFile.exists())
            return null;

        FileInfo lFileInfo = new FileInfo();
        lFileInfo.setFileName(getNameFromFilepath(filePath));
        lFileInfo.setLastModified(lFile.lastModified());
        if (lFile.isDirectory()) {
            lFileInfo.setFileType(FileInfo.FILE_TYPE_DIR);
            lFileInfo.setDir(true);
        } else {
            lFileInfo.setDir(false);
            String extName = getExtFilename(filePath);
            switch (extName) {
                case "bmp":
                case "jpg":
                case "jpeg":
                case "png":
                case "tiff":
                case "gif":
                    lFileInfo.setFileType(FileInfo.FILE_TYPE_PICTURE);
                    break;
                case "mp3":
                case "ogg":
                case "acc":
                case "wma":
                case "wav":
                case "ape":
                case "flac":
                    lFileInfo.setFileType(FileInfo.FILE_TYPE_MUSIC);
                    break;
                case "mp4":
                case "rmvb":
                case "mkv":
                case "avi":
                case "rm":
                case "flv":
                case "swf":
                    lFileInfo.setFileType(FileInfo.FILE_TYPE_VIDEO);
                    break;
                case "txt":
                case "ppt":
                case "pptx":
                case "doc":
                case "docx":
                case "xls":
                case "xlsx":
                case "bin":
                    lFileInfo.setFileType(FileInfo.FILE_TYPE_DOC);
                    break;
                // 笔迹
                case "ncc":
                    lFileInfo.setFileType(FileInfo.FILE_TYPE_NCC);
                    break;
                case "nz":
                case "np":
                    lFileInfo.setFileType(FileInfo.FILE_TYPE_NZ);
                    break;

            }
        }
        lFileInfo.setFilePath(filePath);
        return lFileInfo;
    }


    /**
     * 文件过滤
     */
    public static FilenameFilter filenameFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            File file = new File(dir, filename);
            boolean accept = false;
            for (String extension : FILE_TYPES) {
                if (file.isDirectory() || filename.toLowerCase().endsWith("." + extension.toLowerCase())) {
                    accept = true;
                    break;
                }
            }
            return accept;
        }
    };

    public static FilenameFilter filenameFilterPicture = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            File file = new File(dir, filename);
            boolean accept = false;
            for (String extension : FILE_TYPES_PICTURE) {
                if (file.isDirectory() || filename.toLowerCase().endsWith("." + extension.toLowerCase())) {
                    accept = true;
                    break;
                }
            }
            return accept;
        }
    };

    public static FilenameFilter filenameFilterMusic = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            File file = new File(dir, filename);
            boolean accept = false;
            for (String extension : FILE_TYPES_MUSIC) {
                if (file.isDirectory() || filename.toLowerCase().endsWith("." + extension.toLowerCase())) {
                    accept = true;
                    break;
                }
            }
            return accept;
        }
    };

    public static FilenameFilter filenameFilterVideo = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            File file = new File(dir, filename);
            boolean accept = false;
            for (String extension : FILE_TYPES_VIDEO) {
                if (file.isDirectory() || filename.toLowerCase().endsWith("." + extension.toLowerCase())) {
                    accept = true;
                    break;
                }
            }
            return accept;
        }
    };

    public static FilenameFilter filenameFilterDoc = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            File file = new File(dir, filename);
            boolean accept = false;
            for (String extension : FILE_TYPES_DOC) {
                if (file.isDirectory() || filename.toLowerCase().endsWith("." + extension.toLowerCase())) {
                    accept = true;
                    break;
                }
            }
            return accept;
        }
    };

    public static FilenameFilter filenameFilterPDF = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            File file = new File(dir, filename);
            boolean accept = false;
            for (String extension : FILE_TYPES_PDF) {
                if (file.isDirectory() || filename.toLowerCase().endsWith("." + extension.toLowerCase())) {
                    accept = true;
                    break;
                }
            }
            return accept;
        }
    };
    public static FilenameFilter filenameFilterNcc = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            File file = new File(dir, filename);
            boolean accept = false;
            for (String extension : FILE_TYPES_NCC) {
                if (file.isDirectory() || filename.toLowerCase().endsWith("." + extension.toLowerCase())) {
                    accept = true;
                    break;
                }
            }
            return accept;
        }
    };
    public static FilenameFilter dirnameFilterNz = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            File file = new File(dir, filename);
            return file.isDirectory();
        }
    };
    public static FilenameFilter filenameFilterNz = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            File file = new File(dir, filename);
            boolean accept = false;
            for (String extension : FILE_TYPES_NZ) {
                if (file.isDirectory() || filename.toLowerCase().endsWith("." + extension.toLowerCase())) {
                    accept = true;
                    break;
                }
            }
            return accept;
        }
    };

    public static FilenameFilter filenameFilterKeyword = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            boolean accept = false;
                if (filename.contains(mKeyWord)) {
                    accept = true;
                }
            return accept;
        }
    };


    /**
     * path1和path2拼接路径
     *
     * @param path1
     * @param path2
     * @return
     */
    public static String makePath(String path1, String path2) {
        if (path1.endsWith(File.separator))
            return path1 + path2;

        return path1 + File.separator + path2;
    }

    public static String makePath(String path1, String path2, String path3) {
        return makePath(makePath(path1, path2), path3);
    }


    public static String getNameFromFilepath(String filepath) {
        int pos = filepath.lastIndexOf('/');
        if (pos != -1) {
            return filepath.substring(pos + 1);
        }
        return "";
    }


    /**
     * 获取文件的扩展名
     *
     * @param filename
     * @return
     */
    public static String getExtFilename(String filename) {
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            return filename.substring(dotPosition + 1, filename.length());
        }
        return "";
    }


    /**
     * 获取上一层路径
     *
     * @param filepath
     * @return
     */
    public static String getParentPath(String filepath) {
        File file = new File(filepath);
        if (file != null) {
            return file.getParent();
        }
        return "";
    }

    public static List<FileInfo> getUSBDevicesN(Context context) {
        List<FileInfo> fileInfoList = new ArrayList<>();
        StorageManager storageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        try {
            Class<?>[] paramClasses = {};
            Method getVolumes = StorageManager.class.getMethod("getVolumes", paramClasses);
            getVolumes.setAccessible(true);
            Object[] params = {};
            List invokeList = (List) getVolumes.invoke(storageManager, params);

            Log.d(TAG, "╔════════════════════════════════════════════════");
            Log.d(TAG, "║  getUSBDevices(), Thread : " + Thread.currentThread().getName());
            Log.d(TAG, "║    getVolumes : " + invokeList);
            Log.d(TAG, "╚════════════════════════════════════════════════");
            if (invokeList != null) {
                Log.d(TAG, "╔════════════════════════════════════════════════");
                Log.d(TAG, "║  getUSBDevices(), Thread : " + Thread.currentThread().getName());
                Log.d(TAG, "║    length=" + invokeList.size());
                Log.d(TAG, "╚════════════════════════════════════════════════");
                for (int i = 0; i < invokeList.size(); i++) {
                    Object obj = invokeList.get(i);
                    // type
                    Method getType = obj.getClass().getMethod("getType");
                    int type = (int) getType.invoke(obj);
                    if (type == 0) {
                        Method getPath = obj.getClass().getMethod("getPath");
                        File path = (File) getPath.invoke(obj);

                        FileInfo fileInfo = new FileInfo();
                        fileInfo.setFileType(FileInfo.FILE_TYPE_U_DISK);
                        fileInfo.setFilePath(path.getAbsolutePath());
                        Log.d(TAG, "╔════════════════════════════════════════════════");
                        Log.d(TAG, "║  getUSBDevices(), Thread : " + Thread.currentThread().getName());
                        Log.d(TAG, "║    path=" + fileInfo.getFilePath());
                        Log.d(TAG, "╚════════════════════════════════════════════════");
//                        fileInfo.setFileName("移动设备" + (i + 1));
                        fileInfo.setFileName(path.getAbsolutePath());
                        fileInfoList.add(fileInfo);
                    }
                }
            }
        } catch (NoSuchMethodException |
                IllegalArgumentException |
                InvocationTargetException |
                IllegalAccessException e) {
            e.printStackTrace();
        }
        return fileInfoList;
    }

    public static final String TAG = "myFileTect";
    public static List<FileInfo> getUSBDevicesL(Context context) {
        List<FileInfo> fileInfoList = new ArrayList<>();
        StorageManager storageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        try {

            Class<?>[] paramClasses = {};
            Method getVolumeList = StorageManager.class.getMethod("getVolumeList", paramClasses);
            getVolumeList.setAccessible(true);
            Object[] params = {};
            Object[] invokes = (Object[]) getVolumeList.invoke(storageManager, params);
            if (invokes != null) {
                for (int i = 0; i < invokes.length; i++) {
                    Object obj = invokes[i];

                    // isPrimary
                    Method isPrimaryMethod = obj.getClass().getMethod("isPrimary", new Class[0]);
                    boolean isPrimary = (boolean) isPrimaryMethod.invoke(obj, new Object[0]);
                    if (!isPrimary) {
                        Method isRemovable = obj.getClass().getMethod("isRemovable", new Class[0]);
                        boolean removable = (boolean) isRemovable.invoke(obj, new Object[0]);

                        Method getPath = obj.getClass().getMethod("getPath", new Class[0]);
                        String path = (String) getPath.invoke(obj, new Object[0]);

                        Log.d(TAG, "╔════════════════════════════════════════════════");
                        Log.d(TAG, "║    removable : 111111111");
                        Log.d(TAG, "║  getUSBDevicesL(), Thread : " + Thread.currentThread().getName());
                        Log.d(TAG, "║    path : " + path);
                        Log.d(TAG, "║    isPrimary : " + isPrimary);
                        Log.d(TAG, "║    removable : " + removable);
                        Log.d(TAG, "╚════════════════════════════════════════════════");

                        File file = new File(path);
                        if(!file.canRead() || !file.canWrite())
                        {
                            Log.d(TAG, "getUSBDevicesL: " + path + " 文件没有读写的权限");

                            continue;
                        }

                        FileInfo fileInfo = new FileInfo();
                        fileInfo.setFileName(path);
                        String fn = getUSBNames(context, storageManager, path);
                        if (fn == null || fn.isEmpty()) {
                            fileInfo.setFileName(context.getResources().getString(R.string.moveable_storage) + i);
                        } else {
                            fileInfo.setFileName(fn);
                        }
                        fileInfo.setFilePath(path);
                        fileInfo.setFileType(FileInfo.FILE_TYPE_U_DISK);
                        fileInfoList.add(fileInfo);
                    }
                }
            }
        } catch (NoSuchMethodException |
                IllegalArgumentException |
                InvocationTargetException |
                IllegalAccessException e) {
            e.printStackTrace();
        }

        // 假数据
//        FileInfo fileInfo = new FileInfo();
//        fileInfo.setFileName("假U盘");
//        fileInfo.setFilePath("/mnt/sdcard/假U盘");
//        fileInfo.setFileType(FileInfo.FILE_TYPE_U_DISK);
//        fileInfoList.add(fileInfo);

        return fileInfoList;
    }


    /**
     * 包含三个"/"则认为是根目录，比如 /mnt/usb/sdb1
     *
     * @param path
     * @return
     */
    public static boolean isRootDir(String path) {
        String path2 = path.replace("/", "");
        return (path.length() - path2.length()) == 3;
    }
    private static String getUSBNames(Context mContext, StorageManager sm, String path) {
        String name = mContext.getString(R.string.moveable_storage);
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                Method getVolumesMethod = StorageManager.class.getMethod("getVolumes");
                List<?> volumeInfos = (List<?>) getVolumesMethod.invoke(sm);
                Class volumeInfoClazz = Class.forName("android.os.storage.VolumeInfo");
                Method getTypeMethod = volumeInfoClazz.getMethod("getType");
                Method getFsUuidMethod = volumeInfoClazz.getMethod("getFsUuid");
                Field fsLabelField = volumeInfoClazz.getDeclaredField("fsLabel");
                Field pathField = volumeInfoClazz.getDeclaredField("path");
                Log.d("paint", "volumeInfos: " + volumeInfos);
                if (volumeInfos != null) {
                    for (Object volumeInfo : volumeInfos) {
                        String uuid = (String) getFsUuidMethod.invoke(volumeInfo);
                        if (uuid != null) {
                            String fsLabelString = (String) fsLabelField.get(volumeInfo);
                            String pathString = (String)pathField.get(volumeInfo);
                            if (path.equals(pathString)) {
                                name = fsLabelString;
                                break;
                            }
                        }
                    }
                }
                if (name == null || name.isEmpty()) {
                    StorageManager storageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
                    Method cc = storageManager.getClass().getMethod("getVolumeList");
                    StorageVolume[] volumes = (StorageVolume[]) cc.invoke(storageManager);
                    if (volumes != null) {
                        Method vGetFsLabel = StorageVolume.class.getDeclaredMethod("getUserLabel");
                        Method vGetPath = StorageVolume.class.getDeclaredMethod("getPath");
                        for (int i = 0; i < volumes.length; i++) {
                            String p = (String) vGetPath.invoke(volumes[i]);
                            if (p != null && p.equals(path)) {
                                return (String) vGetFsLabel.invoke(volumes[i]);
                            }
                        }
                    }
                }


            } catch (Exception e) {
                Log.d("paint", "ee: " + e.getMessage());
            }
        } else {
            if (!"".equals(SystemProperties.get("mstar.hw.init", ""))) {
                MStorageManager storageManager = MStorageManager.getInstance(mContext);
                String[] volumes = storageManager.getVolumePaths();
                if (volumes == null || (volumes.length == 0)) {
                    return null;
                }
                File file = new File("proc/mounts");
                if (!file.exists() || file.isDirectory()) {
                    file = null;
                }
                for (int i = 0; i < volumes.length; ++i) {
                    String state = storageManager.getVolumeState(volumes[i]);
                    if (state == null || !state.equals(Environment.MEDIA_MOUNTED)) {
                        continue;
                    } else {
                        //String path =  "/mnt/usb/sda1";
                        String[] pathPartition = path.split("/");
                        String label = pathPartition[pathPartition.length - 1];
                        String volumeLabel = storageManager.getVolumeLabel(path);
                        if (volumeLabel != null) {
                            // get rid of the long space in the Label word
                            String[] tempVolumeLabel = volumeLabel.split(" ");
                            volumeLabel = "";
                            for (int j = 0; j < tempVolumeLabel.length; j++) {
                                if (j != tempVolumeLabel.length - 1) {
                                    volumeLabel += tempVolumeLabel[j] + " ";
                                    continue;
                                }
                                volumeLabel += tempVolumeLabel[j];
                            }
                        }
                        if (null == volumeLabel) {
                            name = mContext.getString(R.string.moveable_storage);
                        } else {
                            name = volumeLabel;
                        }
                    }
                }
            }
        }
        return name;
    }
}
