package com.newskyer.meetingpad.fileselector.file.model;

/**
 * @author liziyang
 * @since 2018/1/24
 */
public class FileInfo {

    public static final int FILE_TYPE_INNER_DISK = 0;
    public static final int FILE_TYPE_U_DISK = 1;

    public static final int FILE_TYPE_DIR = 2;
    public static final int FILE_TYPE_PICTURE = 3;
    public static final int FILE_TYPE_MUSIC = 4;
    public static final int FILE_TYPE_VIDEO = 5;
    public static final int FILE_TYPE_DOC = 6;
    public static final int FILE_TYPE_NCC = 7;
    public static final int FILE_TYPE_PDF = 8;
    public static final int FILE_TYPE_NZ = 9;
    public static final int FILE_TYPE_LIMITED_PAGE = 10;

    private float width = 0;
    private float height = 0;
    private String documentPath = "";

    private String fileName;

    private String filePath;

    private int fileType;

    private boolean isDir;
    private long lastModified;
    private boolean isCollect = false;
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isDir() {
        return isDir;
    }

    public void setDir(boolean dir) {
        isDir = dir;
    }
    public void setLastModified(long lastModified){
        this.lastModified = lastModified;
    }
    public long getLastModified(){
        return lastModified;
    }

    public void setCollect(boolean cecent) {
        isCollect = cecent;
    }

    public boolean isCollect() {
        return isCollect;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }
}
