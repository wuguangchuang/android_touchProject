package com.newskyer.meetingpad.fileselector.util;


import android.content.Context;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.IMountService.Stub;
import android.os.storage.StorageVolume;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

public class MStorageManager {
    private static final String TAG = "MStorageManager";
    static final Object mInstanceSync = new Object();
    static MStorageManager mInstance = null;
    Object mService = null;
    Looper mTgtLooper;
    private final AtomicInteger mNextNonce = new AtomicInteger(0);
    private static MStorageManager mMStorageManager = null;

    private int getNextNonce() {
        return this.mNextNonce.getAndIncrement();
    }

    private MStorageManager(Object service, Looper tgtLooper) {
        this.mService = service;
        this.mTgtLooper = tgtLooper;
    }

    public static MStorageManager getInstance(Context context) {
        if (mInstance == null) {
            Object var1 = mInstanceSync;
            synchronized(mInstanceSync) {
                if (mInstance == null) {
                    IBinder b = ServiceManager.getService("mount");
                    mInstance = new MStorageManager(IMountService.Stub.asInterface(b), context.getMainLooper());
                }
            }
        }

        return mInstance;
    }

    public String getVolumeState(String mountPoint) {
        if (this.mService == null) {
            return "removed";
        } else {
//            try {
//                return this.mService.getVolumeState(mountPoint);
//            } catch (RemoteException var3) {
//                Log.e("MStorageManager", "Failed to get volume state", var3);
//                return null;
//            }
            Class<?> clz = null;
            try {
                clz = Class.forName("android.os.storage.IMountService");
                Method method = clz.getMethod("getVolumeState", String.class);
                return (String) method.invoke(mService, mountPoint);
            } catch (Exception e) {
            }
            return "";
        }
    }


    public String[] getVolumePaths() {
        StorageVolume[] volumes = null;
        Class<?> clz = null;
        try {
            clz = Class.forName("android.os.storage.IMountService");
            Method method = clz.getMethod("getVolumeList");
            volumes = (StorageVolume[]) method.invoke(mService);
        } catch (Exception e) {
        }
//        return null;
        if (volumes == null) {
            return null;
        } else {
            int count = volumes.length;
            String[] paths = new String[count];

            for(int i = 0; i < count; ++i) {
                paths[i] = StorageManagerUtils.getPath(volumes[i]);
            }

            return paths;
        }
    }

    public String getVolumeLabel(String mountPoint) {
//        try {
//            return this.mService.getVolumeLabel(mountPoint);
        Class<?> clz = null;
        try {
            clz = Class.forName("android.os.storage.IMountService");
            Method method = clz.getMethod("getVolumeLabel", String.class);
            return (String) method.invoke(mService, mountPoint);
        } catch (Exception e) {
        }
        return "";
//        } catch (RemoteException var3) {
//            Log.e("MStorageManager", "Failed to get volume label", var3);
//            return null;
//        }
    }

    static Object iMountService;
    static {
        try {
//            Method method = Class.forName("android.os.ServiceManager").getMethod("getService", String.class);
//            IBinder binder = (IBinder) method.invoke(null, "mount");
            IBinder b = ServiceManager.getService("mount");
            iMountService = IMountService.Stub.asInterface(b);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    static class IMountService {
        static Object mService = null;
        static class Stub {
            static Object asInterface(IBinder b) {
                if (mService != null)
                    return mService;
                try {
                    Class<?> clz = Class.forName("android.os.storage.IMountService$Stub");
                    Method asInterface = clz.getDeclaredMethod("asInterface", IBinder.class);
                    mService =  asInterface.invoke(null, b);
//                    dbg("mService: " + mService.getClass().getName());
                    return mService;
                } catch (Exception e) {
                }
                return null;
            }
        }
        static String getVolumeLabel(String mp) throws RemoteException {
            Class<?> clz = null;
            try {
                clz = Class.forName("android.os.storage.IMountService");
                Method method = clz.getMethod("getVolumeLabel", String.class);
                return (String) method.invoke(mService, mp);
            } catch (Exception e) {
            }
            return "";
        }
        static String getVolumeState(String mp) throws RemoteException {
            Class<?> clz = null;
            try {
                clz = Class.forName("android.os.storage.IMountService");
                Method method = clz.getMethod("getVolumeState", String.class);
                return (String) method.invoke(mService, mp);
            } catch (Exception e) {
            }
            return "";
        }
        static Parcelable[] getVolumeList() throws RemoteException {
            Class<?> clz = null;
            try {
                clz = Class.forName("android.os.storage.IMountService");
                Method method = clz.getMethod("getVolumeList");
                return (Parcelable[]) method.invoke(mService);
            } catch (Exception e) {
            }
            return null;
        }
    }
}
