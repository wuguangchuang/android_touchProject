package dataInformation;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

public class Touch_info {
    public UsbDevice dev = null;
    public String filePath;
    public int product_id;
    public boolean connected ;
    public byte bootloader;
    public String model = null;

    public boolean noTouch = false;
    public boolean permission = false;
    public UsbDeviceConnection mDeviceConnection = null;
    public UsbInterface usbInterface = null;
    //代表一个接口的某个节点的类:写数据节点
    public UsbEndpoint usbEpOut = null;
    //代表一个接口的某个节点的类:读数据节点
    public UsbEndpoint usbEpIn = null;
    public Touch_info(){
        connected = false;
//        id_str = new byte[33];
        bootloader = -1;
    }
}
