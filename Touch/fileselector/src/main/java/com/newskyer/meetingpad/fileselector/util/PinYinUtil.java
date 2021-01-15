package com.newskyer.meetingpad.fileselector.util;

import android.text.TextUtils;

import java.io.UnsupportedEncodingException;

public class PinYinUtil {
    public static String alphaConvertToNumber(char[] pinyin) {
        if (pinyin == null || pinyin.length == 0)
            return "";
        StringBuilder number = new StringBuilder("");
        for (char alpha : pinyin) {
            switch (alpha) {
                case 'a':
                case 'b':
                case 'c':
                    number.append('2');
                    break;
                case 'd':
                case 'e':
                case 'f':
                    number.append('3');
                    break;
                case 'g':
                case 'h':
                case 'i':
                    number.append('4');
                    break;
                case 'j':
                case 'k':
                case 'l':
                    number.append('5');
                    break;
                case 'm':
                case 'n':
                case 'o':
                    number.append('6');
                    break;
                case 'p':
                case 'q':
                case 'r':
                case 's':
                    number.append('7');
                    break;
                case 't':
                case 'u':
                case 'v':
                    number.append('8');
                    break;
                case 'w':
                case 'x':
                case 'y':
                case 'z':
                    number.append('9');
                    break;
                default:
                    if (alpha >= 0 || alpha <= 9)
                        number.append(alpha);
            }
        }
        return number.toString();
    }

    /**
     * 字符串的全角字符转换为半角字符(Single Byte Char)
     *
     * @param input
     * @return
     */
    public static String changToSBC(String input) {
        if (TextUtils.isEmpty(input)) {
            return "";
        }
        char[] c = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == 12288) {
                c[i] = (char) 32;
                continue;
            }
            if (c[i] > 65280 && c[i] < 65375)
                c[i] = (char) (c[i] - 65248);
        }
        return new String(c);
    }

    /**
     * 汉字转换位汉语拼音首字母，英文字符不变
     *
     * @param chines 汉字
     * @return 拼音
     */
    public static String converterToFirstSpell(String chines) {
        if (TextUtils.isEmpty(chines)) {
            return "";
        }
        return JPinyin.getFirstSpell(chines);
    }

    /**
     * 汉字转换位汉语全拼和简拼，英文字符不变
     *
     * @param chines 汉字
     * @return String[0] 全拼 String[1] 简拼
     */
    public static String[] convertToSpell(String chines) {
        if (TextUtils.isEmpty(chines)) {
            return new String[]{
                    "", ""
            };
        }
        if (hasDBC(chines))
            chines = changToSBC(chines);
        String[] result = JPinyin.getPinyin(chines);
        return result;
    }

    /**
     * 将字符串中的中文转化为拼音,其他字符不变
     *
     * @param inputString
     * @return
     */
    public static String getPingYin(String inputString) {
        if (TextUtils.isEmpty(inputString)) {
            return "";
        }
        return JPinyin.getFullPinyin(inputString);
    }

    /**
     * 单位换算
     *
     * @param fileSize
     * @return
     */
    public static String getSizeText(long fileSize) {
        if (fileSize <= 0) {
            return "0.0M";
        }
        if (fileSize > 0 && fileSize < 100 * 1024) {
            // 大于0小于100K时，直接返回“0.1M”（产品需求）
            return "0.1M";
        }
        float result = fileSize;
        String suffix = "M";
        result = result / 1024 / 1024;
        return String.format("%.1f", result) + suffix;
    }

    public static boolean hasChinese(String words) {
        if (TextUtils.isEmpty(words)) {
            return false;
        }
        try {
            byte[] array = words.getBytes("GBK");
            for (int i = 0; i < array.length; i++) {
                if (array[i] < 0) {
                    return true;
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 判断字符是否含有全角字符(Double Byte Char)
     */
    public static boolean hasDBC(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        int length = str.length();
        int bytLength = str.getBytes().length;

        // 都是半角的情况
        if (bytLength == length) {
            return false;
        } else {
            return true;
        }
    }
}
