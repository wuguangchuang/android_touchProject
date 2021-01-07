package dataInformation;

import java.io.File;
import java.util.concurrent.Semaphore;

public class CommandItem {
    public Touch_info touch_info;
    public File file;
//    public int async;
    public Semaphore sem;
    public Touch_package require;

}
