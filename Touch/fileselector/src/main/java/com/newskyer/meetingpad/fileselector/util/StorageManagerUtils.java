package com.newskyer.meetingpad.fileselector.util;

import android.content.Context;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import java.lang.reflect.Method;

import static android.content.Context.STORAGE_SERVICE;

/**
 *
 * author: liuxu
 * date: 2014-10-27
 *
 * There are some useful methods in StorageManager, like:
 * StorageManager.getVolumeList()
 * StorageManager.getVolumeState()
 * StorageManager.getVolumePaths()
 * But for now these methods are not visible in SDK (marked as \@hide).
 * one requirement for these methods is to get secondary storage or
 * OTG disk info.
 *
 * here we use java reflect mechanism to retrieve these methods and data.
 *
 * Demo: ActivityStorageUtilsDemo
 */
public final class StorageManagerUtils {

    private StorageManagerUtils() {
    }

    public static StorageManager getStorageManager(Context cxt) {
        StorageManager sm = (StorageManager)
                cxt.getSystemService(Context.STORAGE_SERVICE);
        return sm;
    }

    public static StorageVolume[] getVolumeList(Context ctx) {
        StorageManager storageManager = (StorageManager) ctx.getSystemService(STORAGE_SERVICE);
        try {
            Method getVolumeList = StorageManager.class.getDeclaredMethod("getVolumeList");
            return (StorageVolume[]) getVolumeList.invoke(storageManager);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getPath(StorageVolume volume) {
        try {
            Class<?> class_StorageVolume = Class.forName("android.os.storage.StorageVolume");
            Method getPath = class_StorageVolume.getMethod("getPath");
            return (String) getPath.invoke(volume);
        } catch (Exception e) {
        	return "";
        }
    }

    public static String getUuid(StorageVolume volume) {
        try {
            Class<?> class_StorageVolume = Class.forName("android.os.storage.StorageVolume");
            Method getUuid = class_StorageVolume.getMethod("getUuid");
            return (String) getUuid.invoke(volume);
        } catch (Exception e) {
            return "";
        }
    }

}
