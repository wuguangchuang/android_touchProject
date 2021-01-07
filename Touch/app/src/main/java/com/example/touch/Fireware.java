package com.example.touch;

import android.text.method.Touch;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import dataInformation.FirewareFileHeader;
import dataInformation.FirewareHeader;
import dataInformation.FirewarePackage;

public class Fireware {
    private File file;
    private boolean ready = false;
    private FirewareFileHeader mFileHeader;
    private byte[] mFileHeaderArray = new byte[18];
    private InputStream inputStream;
    public List<FirewarePackage> mFirewares = new ArrayList<>();
    final public static String TAG = "mytext";

    public Fireware(String path){

        int ret;
        file = new File(path);
        byte []fileData;
        RandomAccessFile randomAccessFile;
        if(!file.exists())
        {
            Log.d(TAG, String.format("Fireware: %s file is not exists",path));
            return;
        }
        if(!file.canRead())
        {
            Log.d(TAG, "Fireware: can not open fireware file:");
            return;
        }
        else {
            try {
                inputStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }
            try {
                ret = inputStream.read(mFileHeaderArray);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            if (ret < 0) {
                Log.e(TAG, "Fireware: inputStream.read文件读取失败" );
                return;
            }
            mFileHeader = new FirewareFileHeader();
            ArrayToFirewareFileHeader(mFileHeaderArray,mFileHeader);
            Log.d(TAG, "Fireware: " + path);
            Log.d(TAG, "Fireware: " + String.format("version: 0x%04x, header size: %d, file size: %d, fireware count: %d",
                    mFileHeader.version, mFileHeader.headerSize, mFileHeader.fileSize, mFileHeader.firewareCount));

            int binSize = mFileHeader.fileSize;
            if(inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            fileData = new byte[binSize];
            try {
                randomAccessFile = new RandomAccessFile(file,"r");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }
            int remain = binSize;
            int readSize = 1024;
            int index = 0;
            int offset = 18;
//            while(remain > 0)
//            {
//                try {
//                    randomAccessFile.seek(offset);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

//                byte[] readdata = new byte[1024];
//                readSize = readSize > remain?remain:readSize;
//                try {
//                    ret = randomAccessFile.read(readdata,0,readSize);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    Log.d(TAG, "Fireware: " + e.toString());
//                    return;
//                }
//                for (int i = 0;i < ret;i++)
//                {
//                    fileData[index + i] = readdata[i];
//                }
//                index += ret;
//                remain -= ret;
//            }
            try {
                    randomAccessFile.seek(offset);
            } catch (IOException e) {
                    e.printStackTrace();
                }
            try {
                ret = randomAccessFile.read(fileData);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Fireware: " + e.toString());
                return;
            }

            if(ret < 0)
            {
                Log.d(TAG, "Fireware: 读取固件数据出错");
            }
            CCRC_32 crc = new CCRC_32();
            crc.Reset();
            crc.Calculate(fileData, binSize);

            if ((crc.GetCrcResult() &0xffffffffL) != (mFileHeader.crc32 & 0xffffffffL)) {
                Log.e(TAG, "Fireware: file crc error");
                Log.d(TAG, "Fireware: " + String.format("1calculate crc: 0x%x", (crc.GetCrcResult() &0xffffffffL)));
                Log.d(TAG, "Fireware: " + String.format("1file info crc: 0x%x", mFileHeader.crc32));
                return;
            }

            FirewarePackage fireware;
            byte[] firewareArray;
            FirewareHeader header;
            byte[] headerArray ;
            offset = mFileHeaderArray.length;
            try {
                randomAccessFile.seek(offset);
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < mFileHeader.firewareCount; i++) {
                fireware = new FirewarePackage();
                firewareArray = new byte[64];
                headerArray = new byte[51];
                try {
                    ret = randomAccessFile.read(headerArray,0,51);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(ret < 0)
                {
                    Log.e(TAG, "1Fireware: 读取固件头部出错" );
                    return;
                }
//                offset += ret;
                header =  new FirewareHeader();
                ArrayToFirewareHeader(headerArray,header);

                try {
                    ret = randomAccessFile.read(header.verifyCode,0,header.verifyCodeSize);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(ret < 0)
                {
                    Log.e(TAG, "2Fireware: 读取header.verifyCode出错" );
                    return;
                }
                byte[] firmwareDataCRC32 = new byte[4];
                try {
                    ret = randomAccessFile.read(firmwareDataCRC32,0,4);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(ret < 0)
                {
                    Log.e(TAG, "3Fireware: 读取header.firmwareDataCRC32出错" );
                    return;
                }
                header.firmwareDataCRC32 = TouchManager.toDWord(firmwareDataCRC32[0],firmwareDataCRC32[1],firmwareDataCRC32[2],firmwareDataCRC32[3]);

                Log.d(TAG, "Fireware: "+String.format("Fireware %d", i));
                Log.d(TAG, "Fireware: " + String.format("version: 0x%04x, header size: %d, device range [0x%04x - 0x%04x]",
                        header.version, header.headerSize,
                        header.deviceIdRangeStart, header.deviceIdRangeEnd));;
                Log.d(TAG, "Fireware: " + String.format("package size: %d, package count: %d, verify code size: %x",
                        header.packSize, header.packCount, header.verifyCodeSize));

                fireware.header = header;
                fireware.data = new byte[header.packCount * header.packSize];
//                try {
//                    randomAccessFile.seek(offset);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                try {
                    ret = randomAccessFile.read(fireware.data,0,fireware.data.length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(ret < 0)
                {
                    Log.e(TAG, "4Fireware: 读取固件数据出错" );
                    return;
                }
                offset += ret;
                crc.Reset();
//                Log.d(TAG, "Fireware: " + String.format("2fireware  crc: 0x%x", fireware.header.firmwareDataCRC32));
                if ((crc.Calculate(fireware.data, fireware.header.packCount * fireware.header.packSize) & 0xffffffffL)
                    != (fireware.header.firmwareDataCRC32 & 0xffffffffL)) {
                    Log.e(TAG, "Fireware: " + String.format("2calculate crc: 0x%x", crc.GetCrcResult() & 0xffffffffL));
                    Log.e(TAG, "Fireware: " + String.format("2fireware  crc: 0x%x", fireware.header.firmwareDataCRC32));
                    return;
                }
                mFirewares.add(fireware);
            }

        }
        Log.d(TAG, "Fireware: fireware is ready");
        ready = true;

    }


    public FirewareFileHeader getFileHeader(){
        return mFileHeader;
    }
    public FirewarePackage getFirewarePackage(int index){
        if (!isReady())
            return null;
        if (index < 0) return null;
        FirewarePackage fp = mFirewares.get(index);
        return fp;
    }
    public int firewareCount(){
        if (!isReady())
            return -1;
        return mFileHeader.firewareCount;
    }
    public boolean isReady(){
        return ready;
    }
    void ArrayToFirewareFileHeader(byte[] data,FirewareFileHeader fileHeade){
        byte[] version = new byte[2];
        version[0] = data[0];
        version[1] = data[1];
//        fileHeade.version = ByteUtils.byte2ToShort(version);
        fileHeade.version = (short) TouchManager.toWord(version[0],version[1]);

        byte[] headerSize = new byte[4];
        headerSize[0] = data[2];
        headerSize[1] = data[3];
        headerSize[2] = data[4];
        headerSize[3] = data[5];
        fileHeade.headerSize = TouchManager.toDWord(headerSize[0],headerSize[1],headerSize[2],headerSize[3]);

        byte[] fileSize = new byte[4];
        fileSize[0] = data[6];
        fileSize[1] = data[7];
        fileSize[2] = data[8];
        fileSize[3] = data[9];
        fileHeade.fileSize = TouchManager.toDWord(fileSize[0],fileSize[1],fileSize[2],fileSize[3]);

        byte[] firewareCount = new byte[4];
        firewareCount[0] = data[10];
        firewareCount[1] = data[11];
        firewareCount[2] = data[12];
        firewareCount[3] = data[13];
        fileHeade.firewareCount = TouchManager.toDWord(firewareCount[0],firewareCount[1],firewareCount[2],firewareCount[3]);

        byte[] crc32 = new byte[4];
        crc32[0] = data[14];
        crc32[1] = data[15];
        crc32[2] = data[16];
        crc32[3] = data[17];
        fileHeade.crc32 = TouchManager.toDWord(crc32[0],crc32[1],crc32[2],crc32[3]);;
    }
    void ArrayToFirewareHeader(byte[] data,FirewareHeader header){
        int offset = 0;
        byte[] version = new byte[2];
        version[0] = data[0];
        version[1] = data[1];
        header.version = (short) TouchManager.toWord(version[0],version[1]);

        byte[] headerSize = new byte[4];
        headerSize[0] = data[2];
        headerSize[1] = data[3];
        headerSize[2] = data[4];
        headerSize[3] = data[5];
        header.headerSize = TouchManager.toDWord(headerSize[0],headerSize[1],headerSize[2],headerSize[3]);

        byte[] deviceIdRangeStart = new byte[2];
        deviceIdRangeStart[0] = data[6];
        deviceIdRangeStart[1] = data[7];
        header.deviceIdRangeStart = (short) TouchManager.toWord(deviceIdRangeStart[0],deviceIdRangeStart[1]);

        byte[] deviceIdRangeEnd = new byte[2];
        deviceIdRangeEnd[0] = data[8];
        deviceIdRangeEnd[1] = data[9];
        header.deviceIdRangeEnd = (short) TouchManager.toWord(deviceIdRangeEnd[0],deviceIdRangeEnd[1]);

        byte[] packSize = new byte[4];
        packSize[0] = data[10];
        packSize[1] = data[11];
        packSize[2] = data[12];
        packSize[3] = data[13];
        header.packSize = TouchManager.toDWord(packSize[0],packSize[1],packSize[2],packSize[3]);
        byte[] packCount = new byte[4];
        packCount[0] = data[14];
        packCount[1] = data[15];
        packCount[2] = data[16];
        packCount[3] = data[17];
        header.packCount = TouchManager.toDWord(packCount[0],packCount[1],packCount[2],packCount[3]);

        int i = 0;
        int index = 18;
        for(i = 0;i < 32;i++)
        {
            header.handShakeCode[i] = data[index + i];
        }
        index += 32;
        header.verifyCodeSize = data[index];
//        for(i = 0;i < header.verifyCode.length;i++)
//        {
//            header.verifyCode[i] = data[index + i];
//        }
//        index += header.verifyCode.length;
//        byte[] firmwareDataCRC32 = new byte[4];
//        firmwareDataCRC32[0] = data[index++];
//        firmwareDataCRC32[1] = data[index++];
//        firmwareDataCRC32[2] = data[index++];
//        firmwareDataCRC32[3] = data[index++];
//        header.firmwareDataCRC32 = TouchManager.toDWord(firmwareDataCRC32[0],firmwareDataCRC32[1],firmwareDataCRC32[2],firmwareDataCRC32[3]);

    }

}
