package com.example.touch;

import android.util.Log;

public class HidrawManager {
    static {
        try {
            System.loadLibrary("HidrawManager");
            android.util.Log.i("JNI", "HidrawManager load success");
        } catch (Exception e) {
            Log.e("JNI", "MyFirstJinTest load error");
            e.printStackTrace();
        }
    }

    public static native int openHidraw(String filePath);
    public static native int writeToHidraw(byte[] sendData);
    public static native byte[] readForHidraw();
    public static native byte[] readWrite(byte[] sendData);
    public static native int removeHidRaw(int vid,int pid);
    public static native int closeFd(int vid,int pid);
    public static native boolean exist(String filePath);
}
