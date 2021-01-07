package dataInformation;

public class Hid_device_info {
    /** Platform-specific device path */
    String path;
    /** Device Vendor ID */
    short vendor_id;
    /** Device Product ID */
    short product_id;
    /** Serial Number */
    String serial_number;
    /** Device Release Number in binary-coded decimal,
     also known as Device Version Number */
    short release_number;
    /** Manufacturer String */
    String manufacturer_string;
    /** Product string */
    String product_string;
    /** Usage Page for this Device/Interface
     (Windows/Mac only). */
    short usage_page;
    /** Usage for this Device/Interface
     (Windows/Mac only).*/
    short usage;
    /** The USB interface which this logical device
     represents. Valid on both Linux implementations
     in all cases, and valid on the Windows implementation
     only if the device contains more than one interface. */
    int interface_number;

    char report_id;
}
