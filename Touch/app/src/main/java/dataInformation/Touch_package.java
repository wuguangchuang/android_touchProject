package dataInformation;

import com.example.touch.R;

public class Touch_package {
    public byte report_id;
    public byte version;
    public byte magic;
    public byte flow;
    public byte reserved1;
    public byte master_cmd;
    public byte sub_cmd;
    public byte reserved2;
    public byte data_length;
    public byte[] data ;
    public Touch_package(){
        data = new byte[256];
    }
}
