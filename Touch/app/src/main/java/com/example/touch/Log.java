package com.example.touch;

public class Log {
    private String TAG = "myText";
    private String fileName1 = "touch_log1.txt";
    private String fileName2 = "touch_log2.txt";
    public  void D(String string)
    {
        android.util.Log.d(TAG, string);

    }
    private void saveStringToFile(String string)
    {
        String fileName = fileName1;
    }
}
