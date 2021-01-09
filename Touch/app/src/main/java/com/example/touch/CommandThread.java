package com.example.touch;

import android.hardware.usb.UsbDevice;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import dataInformation.CommandItem;
import dataInformation.Touch_info;
import dataInformation.Touch_package;

public class CommandThread extends Thread{


    private static final String TAG = "myText";

    private HostCommandThread hostCommandThread;
    private Queue<CommandItem> mCommandItem;
    private Semaphore sem;
    private Semaphore recvSem;
    byte[] readData;
    int replylength = 0;

    public boolean running;
    private int tryCount;
    private UsbDevice myTouchDevice;

    public CommandThread(){
        mCommandItem  = new LinkedList<>();
        sem = new Semaphore(0);
        recvSem = new Semaphore(1);
        running = true;
        tryCount = 3;
        readData = new byte[256];
    }


    @Override
    public void run() {
        CommandItem item;
        byte[] sendByte;
        int ret = -1;
        boolean sendSuccess = false;
        File file;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        int readLength = 0;

        while (running) {
            try {
//                sem.acquire(1);
                if(!sem.tryAcquire(1,1, TimeUnit.MILLISECONDS)){
                    continue;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
            if (mCommandItem.isEmpty()) {
                continue;
            }
            try {
                recvSem.acquire(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (this) {
//                Log.d(TAG, "run: 取队首元素");
                //取队首元素

                item = mCommandItem.element();
                file = item.file;
            }
            if (item.touch_info == null) {
                readData = null;
                item.sem.release(1);
                continue;
            }
            if (file == null)
            {
                Log.d(TAG, "run: 没有找到" + item.file.getAbsolutePath() + "设备");
                readData = null;
                item.sem.release(1);
                continue;
            }
            if (!file.exists()) {
                Log.d(TAG, "run: 没有找到设备");
                readData = null;
                item.sem.release(1);
                continue;
            }
            if(!file.canRead() || !file.canWrite()){
                Log.d(TAG, "run: 没有读写的权限，需要修改权限");
                ret = changeHidrawPermission(file.getAbsolutePath());
                if(ret < 0)
                {
                    readData = null;
                    item.sem.release(1);
                    continue;
                }
            }
//            Log.d(TAG, "command: 开始发送命令");
            sendByte = new byte[64];
            requireClassToArray(item.require,sendByte);
//            if(HidrawManager.openHidraw(item.touch_info.filePath) >= 0)
//            {
//                readData = HidrawManager.readWrite(sendByte);
//                if(readData == null)
//                {
//                    Log.d(TAG, "run: 应答数据为空");
//                }
//                HidrawManager.closeFd(item.touch_info.dev.getVendorId(),item.touch_info.dev.getProductId());
//            }
//            else {
//                Log.d(TAG, "run: 文件打开失败");
//            }

            //发送命令给USB设备
            boolean writeSuccess = true;
//            Log.d(TAG, "check_Hid: 驱动设备：" + file.getAbsolutePath());
            for(int i = 0;i < 3;i++)
            {
                try {
                    outputStream = new FileOutputStream(file);
                    inputStream = new FileInputStream(file);
                    try {
//                        Log.d(TAG, "run: 发送命令:" + Arrays.toString(sendByte));
                        outputStream.write(sendByte);
//                        Log.d(TAG, "run: 发送命令成功");
                        writeSuccess = true;
                        break;
                    } catch (IOException e) {
                        readData = null;
                        writeSuccess = false;
//                        item.sem.release(1);
                        e.printStackTrace();
                        Log.d(TAG, "run: " + e.toString());
//                        continue;
                    }
                } catch (FileNotFoundException e) {
                    readData = null;
                    item.sem.release(1);
                    e.printStackTrace();
//                    Log.d(TAG, "getFirewareInfo: " + e.toString());
                }finally {
                    if(outputStream != null){
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
            if(!writeSuccess)
            {
                item.sem.release(1);
                continue;
            }


            tryCount = 2000;
            readData = null;
            byte[] data = new byte[64];
//            long startTime = System.currentTimeMillis();
            while (running )
            {
                try {
                    if(data == null)
                        data = new byte[64];
                    if(inputStream == null)
                    {
                        readData = null;
                        break;
                    }
                    readLength = inputStream.read(data);
//                    Log.d(TAG, "run: hidraw读取数据：" + Arrays.toString(data));
                    if(byteToInt(data[0]) == 205)
                    {
                        synchronized (CommandThread.class){
                            replylength = readLength;

                            readData = new byte[readLength];
                            for(int i = 0;i < readLength;i++)
                            {
                                readData[i] = data[i];
                            }
//                            Log.d(TAG, "run: " + Arrays.toString(readData));
                            Touch_package reply = replyArrayToClass(readData);
//                            if(!TouchManager.checkCommandReply(item.require,reply)){
//                                continue;
//                            }


                        }
                        break;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
            if(inputStream != null)
            {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
//            Log.d(TAG, "命令读写完成");
            item.sem.release(1);

//            Log.d(TAG, "command: 命令线程执行完毕");
        }

    }


    public Touch_package addCommandToQueue(Touch_info touch_info, Touch_package require){
        if (touch_info == null) {
            Log.d(TAG, "addCommandToQueue: : device is not ready");
            return null;
        }
        if (require == null) {
            Log.d(TAG, "addCommandToQueue: require is NULL");
            return null;
        }
        if(touch_info.noTouch)
        {
            if(hostCommandThread == null)
            {
                hostCommandThread = new HostCommandThread();
                hostCommandThread.start();
            }
            Touch_package reply = hostCommandThread.addCommandToQueue(touch_info,require);
            return reply;
        }
        if(touch_info.filePath == null)
        {
            Log.d(TAG, "addCommandToQueue: touch_info.filePath = null" );
        }
        require.report_id = (byte)0xCD;
        require.version = 0x01;
        require.magic = (byte) (Math.random() * 255);
        require.flow = 1;

        CommandItem item = new CommandItem();
//        Log.d(TAG, "addCommandToQueue: new CommandItem");
        if(touch_info.filePath != null)
        {
            File f = new File(touch_info.filePath);
            item.file = f;
        }else {
            item.file = null;
        }

        item.touch_info = touch_info;
        item.require = require;
        item.sem = new Semaphore(0);
        if(touch_info.filePath != null)
        {
            synchronized (CommandThread.class){

                mCommandItem.add(item);
            }
        }

        sem.release(1);

        try {
            item.sem.tryAcquire(1,30000,TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Touch_package reply = new Touch_package();
        synchronized (CommandThread.class){
            if(readData != null)
            {
                reply = replyArrayToClass(readData);
//                Log.d(TAG, "addCommandToQueue: reply.data= " + Arrays.toString(reply.data));
            }
            else
                reply = null;
        }
//        Log.d(TAG, "addCommandToQueue: 删除队首元素");
        mCommandItem.remove();
        recvSem.release(1);
        return reply;
    }

    public Touch_package replyArrayToClass(byte[] reply_array){
        Touch_package reply = new Touch_package();
        reply.report_id = reply_array[0];
        reply.version = reply_array[1];
        reply.magic = reply_array[2];
        reply.flow = reply_array[3];
        reply.reserved1 = reply_array[4];
        reply.master_cmd = reply_array[5];
        reply.sub_cmd = reply_array[6];
        reply.reserved2 = reply_array[7];
        reply.data_length = reply_array[8];
        if(reply.data_length > 0)
        {
            for(int i = 0;i < reply.data_length;i++)
            {
                reply.data[i] = reply_array[9 + i];
            }
        }
//            reply.data = Arrays.copyOfRange(reply_array,9,reply.data_length);
        return reply;
    }
    void requireClassToArray(Touch_package require,byte[] require_array){

        if(require == null )
        {
            return;
        }
        require_array[0] = require.report_id ;
        require_array[1] = require.version  ;
        require_array[2] = require.magic ;
        require_array[3] = require.flow ;
        require_array[4] = require.reserved1 ;
        require_array[5] = require.master_cmd ;
        require_array[6] = require.sub_cmd ;
        require_array[7] = require.reserved2 ;
        require_array[8] = require.data_length ;
        if(require.data != null)
        {
            for(int i = 0;i < require.data.length && (i+9) < require_array.length;i++){
                require_array[9 + i] = require.data[i];
            }
        }

    }
    public char byteToChar(byte l,byte h){
        char ch = (char)((h << 8) | l);
        return ch;
    }
    public static int byteToInt(byte b)
    {
        int ret;
        if(b < 0)
        {
            ret = (256 - Math.abs(b));
        }
        else
        {
            ret = b;
        }
        return ret & 0xff;
    }
    public static int changeHidrawPermission(String filePath){
        int result = -1;
        DataOutputStream dos = null;

        try {
            Process p = Runtime.getRuntime().exec("su");
            dos = new DataOutputStream(p.getOutputStream());
            DataInputStream in = new DataInputStream(p.getInputStream());
            DataInputStream ein = new DataInputStream(p.getErrorStream());


            String cmd = "chmod 777 " + filePath;
            Log.i(TAG, cmd);
            dos.writeBytes(cmd + "\n");
            dos.flush();
//            Log.e(TAG, "changeHidrawPermission: " +in.readUTF());
//            Log.e(TAG, "changeHidrawPermission: " +ein.readUTF());
            dos.writeBytes("exit\n");
            dos.flush();
            p.waitFor();
            result = p.exitValue();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;

    }

}
