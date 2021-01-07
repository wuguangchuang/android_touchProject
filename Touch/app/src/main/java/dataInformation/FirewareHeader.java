package dataInformation;

public class FirewareHeader {
    public short version;
    public int headerSize;
    public short deviceIdRangeStart; //	此固件中的FirmwareData所对应的MCU的ID段的起始ID	2
    public short deviceIdRangeEnd; //	此固件中的FirmwareData所对应的MCU的ID段的结束ID	2
    public int packSize;    //	此固件中的FirmwareData每一个数据包的数据量	4
    public int packCount;   //	此固件中的FirmwareData数据包数量	4
    public byte[] handShakeCode = new byte[32];    //	握手校验码	32
    public byte verifyCodeSize;     //	固件校验码长度	1
    public byte[] verifyCode = new byte[64];     //	固件校验码，用于设备端校验。	FWVerifyCodeSize
    public int firmwareDataCRC32;    //	FirmwareData的CRC32校验的结果，用于文件校验。	4
}
