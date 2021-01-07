package dataInformation;

import com.example.touch.R;

public class Mcu_info {
    byte type_l;
    byte type_h;
    byte[] id ;
    public Mcu_info(){
        id = new byte[16];
    }
}
