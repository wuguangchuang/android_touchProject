package com.example.touch;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

import dataInformation.CommandItem;
import dataInformation.Touch_info;
import dataInformation.Touch_package;

public class HostCommandThread extends Thread{
    private static final String TAG = "myText";


    private Queue<CommandItem> mCommandItem;
    private Semaphore sem;
    private Semaphore recvSem;
    public UsbManager usbManager;
    public UsbDeviceConnection mDeviceConnection;
    //代表一个接口的某个节点的类:写数据节点
    public UsbEndpoint usbEpOut;
    //代表一个接口的某个节点的类:读数据节点
    public UsbEndpoint usbEpIn;
    byte[] readData;

    public boolean running;
    private int tryCount;
    private UsbDevice myTouchDevice;
    CommandItem item;
//    RecvDataThread recvDataThread;

    public HostCommandThread(){
        mCommandItem  = new LinkedList<>();
        sem = new Semaphore(0);
        recvSem = new Semaphore(0);
        running = true;
        tryCount = 3;
        readData = new byte[256];
//        recvDataThread = new RecvDataThread();
//        recvDataThread.start();
    }


    @Override
    public void run() {

        byte[] sendByte;
        int ret = -1;
        boolean sendSuccess = false;

        while (running)
        {
            try {
                sem.acquire(1);
//                if(!sem.tryAcquire(1,10, TimeUnit.MILLISECONDS)){
//                    continue;
//                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
            if (mCommandItem.isEmpty()) {
                continue;
            }

            synchronized (this) {
//                Log.d(TAG, "run: 取队首元素");
                //取队首元素
                item = mCommandItem.element();

                mDeviceConnection = item.touch_info.mDeviceConnection;
                usbEpIn = item.touch_info.usbEpIn;
                usbEpOut = item.touch_info.usbEpOut;
            }
            if(usbEpOut == null || usbEpIn == null || mDeviceConnection == null)
            {
                Log.e(TAG, "run: 无触摸端口有问题");
                item.sem.release(1);
                continue;
            }
            int outMax = usbEpOut.getMaxPacketSize();
            sendByte = new byte[outMax];
//            Log.d(TAG, "run: 命令线程1");
            requireClassToArray(item.require,sendByte);

            tryCount = 4;
            sendSuccess = false;
//            clearRecvData();
            while (running && tryCount > 1)
            {
                if(!sendInterruptTransfer(sendByte))
                {
                    tryCount--;
                }
                else
                {
                    sendSuccess = true;
                    break;
                }

            }
            if(running && sendSuccess)
            {
//                Log.d(TAG, "run: 开始读取数据");
            }
            tryCount = 3;
            readData = null;
            while(running && sendSuccess )
            {
                readData = recvInterruptTransfer();
                if(readData != null)
                {
                    if(TouchManager.byteToInt(readData[0]) != 205)
                    {
//                        sendInterruptTransfer(sendByte);
//                        tryCount--;
                        continue;
                    }
//                    else
//                    {
//                        Touch_package reply = replyArrayToClass(readData);
//                        if(!TouchManager.checkCommandReply(item.require,reply)){
//                            continue;
//                        }
//                    }
                    break;
                }
//                else
//                {
//                    tryCount--;
//                }
            }
            item.sem.release(1);
//            Log.d(TAG, "run: 命令线程2");
        }

    }
//    class RecvDataThread extends Thread{
//        boolean running = true;
//        byte[] retData = new byte[64];
//        @Override
//        public void run() {
//
//            while (running)
//            {
//                retData = recvInterruptTransfer();
//                if(retData != null)
//                {
//                    if(TouchManager.byteToInt(readData[0]) == 205)
//                    {
//                        readData = Arrays.copyOf(retData,retData.length);
//                        item.sem.release(1);
//                    }
//                }
//            }
//        }
//    }

    public Touch_package addCommandToQueue(Touch_info touchInfo, Touch_package require){
        if (touchInfo.dev == null) {
//            Log.d(TAG, "addCommandToQueue: : device is not ready");
            return null;
        }
        if (require == null) {
//            Log.d(TAG, "addCommandToQueue: require is NULL");
            return null;
        }
        require.report_id = (byte)0xCD;
        require.version = 0x01;
        require.magic = (byte) (Math.random() * 255);
        require.flow = 1;
        CommandItem item = new CommandItem();
//        Log.d(TAG, "addCommandToQueue: new CommandItem");
        item.touch_info = touchInfo;
        item.require = require;
        item.sem = new Semaphore(0);
        synchronized (this){
            Log.d(TAG, "addCommandToQueue: item.require.master_cmd = "+item.require.master_cmd);
            Log.d(TAG, "addCommandToQueue: item.require.sub_cmd = "+item.require.sub_cmd);
            mCommandItem.add(item);
        }
        sem.release(1);

        try {
            item.sem.acquire(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Touch_package reply = new Touch_package();
        synchronized (this){
            if(readData != null)
            {
                reply = replyArrayToClass(readData);
//                Log.d(TAG, "addCommandToQueue: reply.data= " + Arrays.toString(reply.data));
            }
            else
                reply = null;

//            Log.d(TAG, "addCommandToQueue: 删除队首元素");
            mCommandItem.remove();
        }
        return reply;
    }
    private boolean sendInterruptTransfer(byte[] dataToSend)
    {
        if(usbEpIn == null || usbEpOut == null || mDeviceConnection == null )
        {
            Log.d(TAG, "sendInterruptTransfer: 发送数据失败，请检查设备是否给以权限");
            return false;
        }
        /*
        //异步
        //The write endpoint is null unless we just copy the read endpoint
        int bufferDataLength = usbEpOut.getMaxPacketSize();
//        Log.d(TAG, "sendInterruptTransfer: Max Packet Size:" + bufferDataLength);
        ByteBuffer buffer = ByteBuffer.allocate(bufferDataLength + 1);
        UsbRequest request = new UsbRequest();
//        Log.d(TAG, "sendInterruptTransfer: 发送数据："+Arrays.toString(dataToSend));
        buffer.put(dataToSend);
        request.initialize(mDeviceConnection, usbEpOut);
        if(!request.queue(buffer, bufferDataLength)){
            Log.d(TAG, "sendInterruptTransfer: 数据发送失败");
            return false;
        }
        Log.d(TAG, "sendInterruptTransfer: 数据发送成功");

         */
        //发送数据
        Log.d(TAG, "sendInterruptTransfer: ");
        int ret = mDeviceConnection.bulkTransfer(usbEpOut, dataToSend, dataToSend.length, 3000);
        Log.d(TAG, "sendInterruptTransfer:发送数据 ret = " + ret);
        if(ret >= 0)
        {
            return true;
        }
        else
        {
            return false;
        }

    }

    private byte[] recvInterruptTransfer(){
        if(usbEpIn == null || mDeviceConnection == null)
        {
            return null;
        }
        int inMax = usbEpIn.getMaxPacketSize();
        /*
        //异步
        ByteBuffer byteBuffer = ByteBuffer.allocate(inMax);
        UsbRequest usbRequest = new UsbRequest();
        usbRequest.initialize(mDeviceConnection, usbEpIn);
        usbRequest.queue(byteBuffer, inMax);
//        int tryCount = 20;
        long startTime = System.currentTimeMillis();
        while (running && System.currentTimeMillis() - startTime < 1000)
        {
            Log.d(TAG, "recvInterruptTransfer:开始读取数据");
            if(mDeviceConnection.requestWait() == usbRequest){
                byte[] retData = byteBuffer.array();
                if(TouchManager.byteToInt(retData[0]) == 205)
                    Log.d(TAG, "recvsendInterruptTransfer: 读取数据 = "+Arrays.toString(retData));

                return retData;
            }
            Log.d(TAG, "recvInterruptTransfer: 应答有误");
            continue;
        }
        Log.d(TAG, "recvInterruptTransfer: 没有返回的数据");

         */
        byte[] retData = new byte[inMax];
        int ret = mDeviceConnection.bulkTransfer(usbEpIn, retData, retData.length, 30000);
        if(ret >= 0)
        {
            Log.d(TAG, "sendInterruptTransfer:接收数据 retData = " + Arrays.toString(retData));

            return retData;
        }
        else
        {
            return null;
        }
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
//        reply.data = Arrays.copyOfRange(reply_array,9,reply_array.length);
        if(reply.data_length > 0)
        {
            for(int i = 0;i < reply.data_length;i++)
            {
                reply.data[i] = reply_array[9 + i];
            }
        }
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
            for(int i = 0;i < require.data.length && i < (require_array.length - 9);i++){
                require_array[9 + i] = require.data[i];
            }
        }

    }
}
