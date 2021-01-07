package com.example.touch;

import android.util.Log;

public class CCRC_32 {

    final static String TAG = "myText";
    private long[] m_CrcTable = new long[32 * 8];
    private long m_CrcValue;
    private long CRC_INIT_VALUE = 0xFFFFFFFF;
    public CCRC_32(){
        InitialCrcTable();
        m_CrcValue = CRC_INIT_VALUE;
    }
    public CCRC_32(long CrcValue){
        InitialCrcTable();
        m_CrcValue = CrcValue;
    }

    public void Reset(){
        m_CrcValue = CRC_INIT_VALUE;
    }
    public long Calculate(byte[] pData, int dataSize){
        // Calculate the CRC
        long CRC = m_CrcValue;
        int index = 0;
//        Log.d(TAG, "Fireware:Calculate: pdata[0] = " + TouchManager.byteToInt(pData[0])+"\tdataSize = " + dataSize +
//                "\tpdata[dataSize - 1] = " + TouchManager.byteToInt(pData[dataSize-1]));
//        Log.d(TAG, "Fireware:Calculate: "+String.format("pData[1] = %d,pData[2] = %d",TouchManager.byteToInt(pData[1]),TouchManager.byteToInt(pData[2])));
//        Log.d(TAG, "Fireware:Calculate: "+String.format("pData[3] = %d,pData[4] = %d",TouchManager.byteToInt(pData[3]),TouchManager.byteToInt(pData[4])));
//        Log.d(TAG, "Fireware:Calculate: "+String.format("pData[5] = %d,pData[6] = %d",TouchManager.byteToInt(pData[5]),TouchManager.byteToInt(pData[6])));
//        Log.d(TAG, "Fireware:Calculate: "+String.format("pData[7] = %d,pData[8] = %d",TouchManager.byteToInt(pData[7]),TouchManager.byteToInt(pData[8])));

        for(int i = 0;i < dataSize;i++)
        {
//            if(i < 20)
//            {
//                Log.d(TAG, String.format("执行前Fireware:Calculate: m_CrcTable[(int)(CRC ^ TouchManager.byteToInt(pData[%d])) & 0xFF] = 0x%x ,(CRC >> 8)" +
//                        " = 0x%x",i, (m_CrcTable[(int)((CRC & 0xffffffffL) ^ TouchManager.byteToInt(pData[i])) & 0xFF] & 0xffffffffL),(((CRC & 0xffffffffL ) >> 8)& 0xffffffffL)));
//            }
            CRC = ((CRC & 0xffffffffL) >> 8) ^ (m_CrcTable[(int)((CRC & 0xffffffffL) ^ TouchManager.byteToInt(pData[i])) & 0xFF] & 0xffffffffL);
            CRC &= 0xffffffffL;
//            if(i < 20)
//                Log.d(TAG, String.format("执行后Fireware:Calculate: CRC = 0x%x", CRC));
        }
        m_CrcValue = CRC;
//        Log.d(TAG, String.format("Fireware:Calculate: m_CrcValue = 0x%x", m_CrcValue ));
        return GetCrcResult();

    }
    public long GetCrcResult(){
        return ((~m_CrcValue) & 0xffffffffL);
    }
    private void InitialCrcTable(){
        // This is the official polynomial used by CRC-32
        // in PKZip, WinZip and Ethernet.
	    long ulPolynomial = 0x04C11DB7;

        // 256 values representing ASCII character codes.
        for (int i = 0; i <= 0xFF; i++)
        {
            m_CrcTable[i] = Reflect(i,(byte)8) << 24;
//
            for (int j = 0; j < 8; j++)
            {
//                Log.d(TAG, String.format("Fireware :InitialCrcTable: m_CrcTable[%d] & 0xfffffff = ", i) + (m_CrcTable[i] & 0xffffffff));
                if(((m_CrcTable[i] & 0xffffffff) & (1L << 31)) == 0)
                {
//                    Log.d(TAG, String.format("前Fireware:InitialCrcTable: m_CrcTable[%d] = 0x%x",i, m_CrcTable[i]));
//                    Log.d(TAG, String.format("前Fireware:InitialCrcTable: (m_CrcTable[i] & 0xffffffff) << 1) = 0x%x",i, ((m_CrcTable[i] & 0xffffffff) << 1)));
                    m_CrcTable[i] = ((m_CrcTable[i] & 0xffffffff) << 1) ^ 0;
//                    Log.d(TAG, String.format("执行0Fireware:InitialCrcTable: m_CrcTable[%d] = 0x%x",i, m_CrcTable[i]));
                }
                else
                {
//                    Log.d(TAG, String.format("前Fireware:InitialCrcTable: m_CrcTable[%d] = 0x%x",i ,m_CrcTable[i]));
//                    Log.d(TAG, String.format("前Fireware:InitialCrcTable: (m_CrcTable[i] & 0xffffffff) << 1) = 0x%x",i, ((m_CrcTable[i] & 0xffffffff) << 1)));
                    m_CrcTable[i] = ((((m_CrcTable[i]) << 1) & 0xffffffff) ^ ulPolynomial);
//                    Log.d(TAG, String.format("执行1Fireware:InitialCrcTable: m_CrcTable[%d] = 0x%x",i, m_CrcTable[i]));
                }
      //          m_CrcTable[i] = (m_CrcTable[i] << 1) ^ ((m_CrcTable[i] & (1 << 31) != 0) ? ulPolynomial : 0);
            }
//            Log.d(TAG, String.format("中Fireware:InitialCrcTable: m_CrcTable[%d] = ",i) + m_CrcTable[i]);
            m_CrcTable[i] = Reflect(m_CrcTable[i],  (byte)32);
//            Log.d(TAG, String.format("后Fireware:InitialCrcTable: m_CrcTable[%d] = ",i) + m_CrcTable[i]);
        }
//        for(int i = 0;i < 32 * 8;i++)
//        {
//            Log.d(TAG, "Fireware:InitialCrcTable m_CrcTable = " + (m_CrcTable[i] & 0xffffffff)  + "\t" + i);;
//        }

    }
    private long Reflect(long ref,byte ch){
//        Log.d(TAG, "Fireware: Reflect: ref = " + ref);
        long value = 0;
        // Swap bit 0 for bit 7
        // bit 1 for bit 6, etc.
        for (int i = 1; i < (ch + 1); i++)
        {
            if ((ref & 1) != 0)
            {
                value |= 1 << (ch - i);
            }
            ref >>= 1;
        }
//        Log.d(TAG, "Fireware: Reflect: value = " + value);
        return value;
    }
}
