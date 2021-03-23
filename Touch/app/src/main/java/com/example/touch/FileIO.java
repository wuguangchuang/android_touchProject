package com.example.touch;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class FileIO {

    //获取内置SD目录路径
    public static String getInterSDPath(){
        String SDPsth = Environment.getExternalStorageDirectory().getPath();
        return SDPsth;
    }

    public static void writeLineFile(String filename, String[] content){
        clearInfoForFile(filename);
        try {
            FileOutputStream out = new FileOutputStream(filename);
            OutputStreamWriter outWriter = new OutputStreamWriter(out, "UTF-8");
            BufferedWriter bufWrite = new BufferedWriter(outWriter);
            for (int i = 0; i < content.length; i++) {
                bufWrite.write(content[i] + "\r\n");
            }
            bufWrite.close();
            outWriter.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> readInfoFromFile(String fileName) {
        File file =new File(fileName);
        if(!file.exists()) {
            return null;
        }
        List<String> resultStr =new ArrayList<String>();
        try {
            BufferedReader bufferedReader =new BufferedReader(new FileReader(file));
            String str =null;
            while(null !=(str=bufferedReader.readLine())) {
                resultStr.add(str);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return resultStr;
    }


    //清空文件
    public static void clearInfoForFile(String fileName) {
        File file =new File(fileName);
        try {
            if(!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            FileWriter fileWriter =new FileWriter(file);
            fileWriter.write("");
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
