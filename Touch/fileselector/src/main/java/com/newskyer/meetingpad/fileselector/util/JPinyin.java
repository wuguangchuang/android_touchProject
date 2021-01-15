package com.newskyer.meetingpad.fileselector.util;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

public class JPinyin {

    // private static String pinyinWordCode[];

    private static byte[][] pinyinWordCodes;

    private static short pinyinCodeIndex[][];

    private static String charIndex1[];

    private static String charIndex2[];

    private static String multiVocalSplit;

    private static String multiVocalTable[];

    private static String multiVocalPinYinTable[][];

    // public static final int INSERT_SEPARATOR = 0x1 << 0; // Insert a space
    // between
    // // characters.
    // public static final int CAPITALIZE_FIRST = 0x1 << 1;
    public static final int CAPITALIZE_ALL = 0x1 << 2;

    public static final int ABBREVIATE = 0x1 << 4; // Returns only the first
                                                   // Pinyin
                                                   // letter

    // of each character.
    public static final int DISCARD_UNKNOWN = 0x1 << 8; // Discard all
                                                        // characters that

    // not
    // recognized.
    public static final int MULTIVOCAL = 0x1 << 16;

    static {
        initCode();
        init();
    }

    private static HashMap<String, Integer> multiVocalCache = new HashMap<String, Integer>();

    private static String[] ConvertPinyin(byte[] s) {
        if (s.length > 0 && s[s.length - 1] != 0) {
            byte[] tail = new byte[s.length + 1];
            System.arraycopy(s, 0, tail, 0, s.length);
            tail[s.length] = 0;
            s = tail;
        }

        return new String[] {
                ConvertToFullSpellPinyin(s, 1), ConvertToFullSpellPinyin(s, 0),
        };
    }

    private static String ConvertTextToPinyin(byte[] s, int conversionFlags) {
        if (s.length > 0 && s[s.length - 1] != 0) {
            byte[] tail = new byte[s.length + 1];
            System.arraycopy(s, 0, tail, 0, s.length);
            tail[s.length] = 0;
            s = tail;
        }

        int mode = 4;
        if (!((conversionFlags & ABBREVIATE) == ABBREVIATE)) {
            mode |= 1;
        }
        if (!((conversionFlags & DISCARD_UNKNOWN) == DISCARD_UNKNOWN)) {
            mode |= 2;
        }

        String pinyin = null;
        if (!((conversionFlags & MULTIVOCAL) == MULTIVOCAL)) {
            pinyin = ConvertToFullSpellPinyin(s, mode);
        } else {
            pinyin = MakeMultiVocalString(s, mode);
        }

        if ((conversionFlags & CAPITALIZE_ALL) != 0) {
            pinyin = pinyin.toUpperCase(Locale.getDefault());
        }

        if (((conversionFlags & DISCARD_UNKNOWN) == DISCARD_UNKNOWN)) {
            pinyin = pinyin.replaceAll("[ -]", "");
        }
        return pinyin;
    }

    private static String ConvertToFullSpellPinyin(byte[] text, int mode) {
//        ByteArrayBuffer fullSpellPinyin = new ByteArrayBuffer(text.length * 5);
        ByteArrayOutputStream fullSpellPinyin1 = new ByteArrayOutputStream(text.length * 5);

        if (mode < 0)
            return "";

        boolean flag1 = ((mode & 0x1) == 0x1);
        boolean flag2 = ((mode & 0x2) == 0x2);
        boolean flag3 = ((mode & 0x4) == 0x4);

        int i = 0;
        int length = text.length;
        while (i < length) {
            short current = (short) (0x00FF & text[i]);
            if (current == 0)
                break;

            short next = (short) (0x00FF & text[i + 1]);
            byte[] pinyin = new byte[0];

            if ((current >= 129) && (next >= 64)) {
                // 是否为 GBK 字符
                switch (current) {
                    case 163: // 全角 ASCII
                    {
                        char ch = (char) (next - 128);
                        // 控制不能输出非数字, 字母的字符
                        if ((!flag3) && ((ch < 'a') || (ch > 'z')) && ((ch < 'A') || (ch > 'Z'))
                                && ((ch < '0') || (ch > '9'))) {
                            pinyin = new byte[0];
                        } else {
                            pinyin = new byte[] {
                                (byte) ch
                            };
                        }
                        break;
                    }
                    case 162: // 罗马数字
                    {
                        if ((next > 160) && (next - 160 < 94)) {
                            pinyin = stringToBytes(charIndex1[next - 160]);
                        }
                        // 在罗马数字区, 不能翻译的字符非罗马数字
                        else {
                            pinyin = stringToBytes(flag2 ? "?" : "");
                        }
                        break;
                    }
                    case 166: // 希腊字母
                    {
                        if ((next >= 0xA1) && (next <= 0xB8) && (next - 0xA0 < 24)) {
                            pinyin = stringToBytes(charIndex2[next - 0xA0]); // UPPER?
                        } else if ((next >= 0xC1) && (next <= 0xD8) && (next - 0xC0 < 24)) {
                            pinyin = stringToBytes(charIndex2[next - 0xC0]); // UPPER?
                        }
                        break;
                    }
                    default: // 一般汉字
                    {
                        // 获得拼音索引
                        if (current - 129 < 126) {
                            int index = pinyinCodeIndex[current - 128 - 1][next - 63 - 1];
                            if (index == 0) { // 无此汉字, 不能翻译的字符, GBK 保留
                                pinyin = stringToBytes(flag2 ? "?" : "");
                            } else if (!flag1) { // flag1 = False, 是单拼音
                                pinyin = new byte[] {
                                    pinyinWordCodes[index - 1][0]
                                };
                            } else {
                                pinyin = pinyinWordCodes[index - 1];
                                // pinyin = stringToBytes(pinyinWordCode[index -
                                // 1]);
                            }
                            break;
                        } else {
                            pinyin = stringToBytes(flag2 ? "?" : "");
                        }
                        break;
                    }
                }

//                fullSpellPinyin.append(pinyin, 0, pinyin.length);
                fullSpellPinyin1.write(pinyin, 0, pinyin.length);
                i += 2;
            } else {
                // 在 GBK 字符集外, 即半角字符
                if (flag3
                        || (((current >= 'a') && (current <= 'z'))
                                || ((current >= 'A') && (current <= 'Z')) || ((current >= '0') && (current <= '9')))) {
                    byte b = (byte) toLower((char) current);
//                    fullSpellPinyin.append(b);
                    fullSpellPinyin1.write(b);
                }
                ++i;
            }
        }

//        byte[] b = fullSpellPinyin.toByteArray();
        byte[] b = fullSpellPinyin1.toByteArray();

        try {
            return new String(b, "GBK");
        } catch (UnsupportedEncodingException e) {
            // e.printStackTrace();
        }

        return "";
    }

    private static String[][] ConvertToFullSpellPinyinByMultiVocal(byte[] text, int mode) {
        ArrayList<String[]> stringChar = new ArrayList<String[]>();
        if (mode < 0)
            return stringChar.toArray(new String[0][]);

        ArrayList<byte[]> vocalChar = new ArrayList<byte[]>();
        boolean flag1 = ((mode & 0x1) == 0x1);
        boolean flag2 = ((mode & 0x2) == 0x2);
        boolean flag3 = ((mode & 0x4) == 0x4);

        int i = 0;
        int length = text.length;
        while (i < length) {
            short current = (short) (0x00FF & text[i]);
            if (current == 0)
                break;

            short next = (short) (0x00FF & text[i + 1]);
            byte[] pinyin = new byte[0];

            if ((current >= 129) && (next >= 64)) {
                // 是否为 GBK 字符
                switch (current) {
                    case 163: // 全角 ASCII
                    {
                        char ch = (char) (next - 128);
                        // 控制不能输出非数字, 字母的字符
                        if ((!flag3) && ((ch < 'a') || (ch > 'z')) && ((ch < 'A') || (ch > 'Z'))
                                && ((ch < '0') || (ch > '9'))) {
                            pinyin = new byte[0];
                        } else {
                            pinyin = new byte[] {
                                (byte) ch
                            };
                        }

                        vocalChar.add(pinyin);
                        break;
                    }
                    case 162: // 罗马数字
                    {
                        if ((next > 160) && (next - 160 < 94)) {
                            pinyin = stringToBytes(charIndex1[next - 160]);
                        }
                        // 在罗马数字区, 不能翻译的字符非罗马数字
                        else {
                            pinyin = stringToBytes(flag2 ? "?" : "");
                        }
                        vocalChar.add(pinyin);
                        break;
                    }
                    case 166: // 希腊字母
                    {
                        if ((next >= 0xA1) && (next <= 0xB8) && (next - 0xA0 < 24)) {
                            pinyin = stringToBytes(charIndex2[next - 0xA0]); // UPPER?
                        } else if ((next >= 0xC1) && (next <= 0xD8) && (next - 0xC0 < 24)) {
                            pinyin = stringToBytes(charIndex2[next - 0xC0]); // UPPER?
                        }
                        vocalChar.add(pinyin);
                        break;
                    }
                    default: // 一般汉字
                    {
                        // 获得拼音索引
                        // boolean isMultiVocal = false;
                        char gb[] = {
                                (char) current, (char) next
                        };
                        int tableIndex = GetMultiVocalIndex(new String(gb));
                        if (tableIndex != -1) {
                            HashSet<Character> cleanRepeat = new HashSet<Character>();
                            for (int vocalCount = 0; vocalCount < 3; vocalCount++) {
                                String vocal = multiVocalPinYinTable[tableIndex][vocalCount];
                                char singVocal = vocal.charAt(0);
                                if (vocalCount < 2 || singVocal != 0) {
                                    if (!flag1) // flag1 = False, 是单拼音
                                    {
                                        if (!cleanRepeat.contains(singVocal)) {
                                            cleanRepeat.add(singVocal);
                                            pinyin = new byte[] {
                                                (byte) singVocal
                                            };
                                        }
                                    } else {
                                        pinyin = stringToBytes(vocal);
                                    }

                                    vocalChar.add(pinyin);
                                }
                            }
                        } else {
                            if (current - 129 < 126) {
                                int index = pinyinCodeIndex[current - 128 - 1][next - 63 - 1];
                                if (index == 0) // 无此汉字, 不能翻译的字符, GBK 保留
                                {
                                    pinyin = stringToBytes(flag2 ? "?" : "");
                                } else if (!flag1) // flag1 = False, 是单拼音
                                {
                                    pinyin = new byte[] {
                                        pinyinWordCodes[index - 1][0]
                                    };
                                } else {
                                    pinyin = pinyinWordCodes[index - 1];
                                    // pinyin =
                                    // stringToBytes(pinyinWordCode[index - 1]);
                                }

                                vocalChar.add(pinyin);
                                break;
                            } else {
                                pinyin = stringToBytes(flag2 ? "?" : "");
                                vocalChar.add(pinyin);
                            }
                        }
                        break;
                    }
                }

                ArrayList<String> s = new ArrayList<String>();
                for (byte[] b : vocalChar) {
                    try {
                        s.add(new String(b, "GBK"));
                    } catch (UnsupportedEncodingException e) {
                        // e.printStackTrace();
                    }
                }

                stringChar.add(s.toArray(new String[0]));
                vocalChar.clear();
                /* fullSpellPinyin += pinyin; */
                i += 2;
            } else {
                // 在 GBK 字符集外, 即半角字符
                if (flag3
                        || (((current >= 'a') && (current <= 'z'))
                                || ((current >= 'A') && (current <= 'Z')) || ((current >= '0') && (current <= '9')))) {
                    String c = new String(new char[] {
                        toLower((char) current)
                    });
                    stringChar.add(new String[] {
                        c
                    });
                }
                ++i;
            }

        }

        return stringChar.toArray(new String[0][]);
    }

    public static String getFirstSpell(String text) {
        try {
            byte[] gbkBytes = text.getBytes("GBK");
            int flag = JPinyin.ABBREVIATE;
            return JPinyin.ConvertTextToPinyin(gbkBytes, flag);
        } catch (Exception e) {
            // Log.d("pinyin", "exception: " + e.getMessage());
            // e.printStackTrace();
        }

        return "";
    }

    public static String getFullPinyin(String text) {
        try {
            byte[] gbkBytes = text.getBytes("GBK");
            return JPinyin.ConvertTextToPinyin(gbkBytes, 0);
        } catch (Exception e) {
            // Log.d("pinyin", "exception: " + e.getMessage());
            // e.printStackTrace();
        }

        return "";
    }

    private static int GetMultiVocalIndex(String str) {
        synchronized (multiVocalCache) {
            if (multiVocalCache.size() == 0) {
                for (int loop = 0; loop < multiVocalTable.length; loop++) {
                    multiVocalCache.put(multiVocalTable[loop], loop);
                }
            }
        }

        Object value = multiVocalCache.get(str);
        if (value == null)
            return -1;
        else
            return (Integer) value;
    }

    public static String[] getPinyin(String text) {
        try {
            byte[] gbkBytes = text.getBytes("GBK");
            return JPinyin.ConvertPinyin(gbkBytes);
        } catch (Exception e) {
            // Log.d("pinyin", "exception: " + e.toString());
            // e.printStackTrace();
        }

        return new String[] {
                "", ""
        };
    }

    private static void init() {
        String[] pinyinWordCode = new String[] {
                "a", "aes", "ai", "an", "ang", "ao", "ba", "bai", "baike", "baiwa", "ban", "bang",
                "bao", "be", "bei", "ben", "beng", "bi", "bia", "bian", "biao", "bie", "bin",
                "bing", "bo", "bu", "ca", "cai", "cal", "can", "cang", "cao", "ce", "cen", "ceng",
                "ceok", "ceom", "ceon", "ceor", "cha", "chai", "chan", "chang", "chao", "che",
                "chen", "cheng", "chi", "chong", "chou", "chu", "chua", "chuai", "chuan", "chuang",
                "chui", "chun", "chuo", "ci", "cis", "cong", "cou", "cu", "cuan", "cui", "cun",
                "cuo", "da", "dai", "dan", "dang", "dao", "de", "defa", "dei", "deli", "dem",
                "den", "deng", "deo", "di", "dia", "dian", "diao", "die", "dim", "ding", "diu",
                "dong", "dou", "du", "duan", "dug", "dui", "dul", "dun", "duo", "e", "ei", "en",
                "eng", "eo", "eol", "eom", "eos", "er", "fa", "fan", "fang", "fei", "fen", "feng",
                "fenwa", "fiao", "fo", "fou", "fu", "fui", "ga", "gad", "gai", "gan", "gang",
                "gao", "ge", "gei", "gen", "geng", "geo", "geu", "gib", "go", "gong", "gongli",
                "gou", "gu", "gua", "guai", "guan", "guang", "gui", "gun", "guo", "ha", "hai",
                "hal", "han", "hang", "hao", "haoke", "he", "hei", "hem", "hen", "heng", "heui",
                "ho", "hol", "hong", "hou", "hu", "hua", "huai", "huan", "huang", "hui", "hun",
                "huo", "hwa", "hweong", "i", "ji", "jia", "jialun", "jian", "jiang", "jiao", "jie",
                "jin", "jing", "jiong", "jiu", "jou", "ju", "juan", "jue", "jun", "ka", "kai",
                "kal", "kan", "kang", "kao", "ke", "keg", "kei", "kem", "ken", "keng", "keo",
                "keol", "keop", "keos", "keum", "ki", "kong", "kos", "kou", "ku", "kua", "kuai",
                "kuan", "kuang", "kui", "kun", "kuo", "kweok", "kwi", "la", "lai", "lan", "lang",
                "lao", "le", "lei", "lem", "len", "leng", "li", "lia", "lian", "liang", "liao",
                "lie", "lin", "ling", "liu", "liwa", "lo", "long", "lou", "lu", "luan", "lue",
                "lun", "luo", "lv", "m", "ma", "mai", "man", "mang", "mangmi", "mao", "mas", "me",
                "mei", "men", "meng", "meo", "mi", "mian", "miao", "mie", "min", "ming", "miu",
                "mo", "mol", "mou", "mu", "myeo", "myeon", "myeong", "n", "na", "nai", "nan",
                "nang", "nao", "ne", "nei", "nem", "nen", "neng", "neus", "ng", "ngag", "ngai",
                "ngam", "ni", "nian", "niang", "niao", "nie", "nin", "ning", "niu", "nong", "nou",
                "nu", "nuan", "nue", "nun", "nung", "nuo", "nv", "nve", "o", "oes", "ol", "on",
                "ou", "pa", "pai", "pak", "pan", "pang", "pao", "pei", "pen", "peng", "peol",
                "phas", "phdeng", "phoi", "phos", "pi", "pian", "piao", "pie", "pin", "ping", "po",
                "pou", "ppun", "pu", "q", "qi", "qia", "qian", "qiang", "qianke", "qianwa", "qiao",
                "qie", "qin", "qing", "qiong", "qiu", "qu", "quan", "que", "qun", "ra", "ram",
                "ran", "rang", "rao", "re", "ren", "reng", "ri", "rong", "rou", "ru", "rua",
                "ruan", "rui", "run", "ruo", "sa", "saeng", "sai", "sal", "san", "sang", "sao",
                "se", "sed", "sei", "sen", "seng", "seo", "seon", "sha", "shai", "shan", "shang",
                "shao", "she", "shei", "shen", "sheng", "shi", "shike", "shiwa", "shou", "shu",
                "shua", "shuai", "shuan", "shuang", "shui", "shun", "shuo", "shw", "si", "so",
                "sol", "song", "sou", "su", "suan", "sui", "sun", "suo", "ta", "tae", "tai", "tan",
                "tang", "tao", "tap", "te", "tei", "teng", "teo", "teul", "teun", "ti", "tian",
                "tiao", "tie", "ting", "tiu", "tol", "ton", "tong", "tou", "tu", "tuan", "tui",
                "tun", "tuo", "uu", "wa", "wai", "wan", "wang", "wei", "wen", "weng", "wie", "wo",
                "wu", "xi", "xia", "xian", "xiang", "xiao", "xie", "xin", "xing", "xiong", "xiu",
                "xu", "xuan", "xue", "xun", "ya", "yan", "yang", "yao", "ye", "yen", "yi", "yin",
                "ying", "yo", "yong", "you", "yu", "yuan", "yue", "yug", "yun", "za", "zad", "zai",
                "zan", "zang", "zao", "ze", "zei", "zen", "zeng", "zha", "zhai", "zhan", "zhang",
                "zhao", "zhe", "zhei", "zhen", "zheng", "zhi", "zhong", "zhou", "zhu", "zhua",
                "zhuai", "zhuan", "zhuang", "zhui", "zhun", "zhuo", "zi", "zo", "zong", "zou",
                "zu", "zuan", "zui", "zun", "zuo"
        };

        pinyinWordCodes = new byte[pinyinWordCode.length][];
        for (int i = 0; i < pinyinWordCode.length; ++i) {
            pinyinWordCodes[i] = pinyinWordCode[i].getBytes();
        }

        charIndex1 = new String[] {
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "", "", "", "", "", "", "1",
                "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
                "17", "18", "19", "20", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11",
                "12", "13", "14", "15", "16", "17", "18", "19", "20", "1", "2", "3", "4", "5", "6",
                "7", "8", "9", "10", "", "", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "",
                "", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "", ""
        };

        charIndex2 = new String[] {
                "a", "b", "g", "d", "e", "z", "e", "th", "i", "k", "l", "m", "n", "x", "o", "p",
                "r", "s", "t", "u", "ph", "kh", "ps", "o"
        };

        multiVocalSplit = "\t";

        multiVocalTable = new String[] {
                "艾", "拗", "扒", "柏", "磅", "刨", "剥", "薄", "曝", "掺", "陂", "夯", "辟", "便", "卜", "伯",
                "参", "藏", "曾", "刹", "查", "差", "禅", "长", "玚", "裳", "朝", "车", "称", "乘", "澄", "尺",
                "坻", "重", "仇", "臭", "畜", "褚", "传", "伺", "兹", "攒", "得", "沓", "大", "单", "弹", "澹",
                "叨", "的", "地", "佃", "调", "丁", "都", "度", "沌", "恶", "番", "蕃", "繁", "冯", "逢", "佛",
                "否", "宓", "脯", "伽", "咖", "芥", "盖", "扛", "仡", "革", "贾", "纶", "龟", "还", "和", "郇",
                "会", "系", "亟", "夹", "价", "见", "降", "角", "觉", "嚼", "解", "劲", "炅", "句", "圈", "卡",
                "楷", "壳", "隗", "溃", "了", "乐", "肋", "俩", "靓", "偻", "陆", "露", "络", "落", "率", "绿",
                "抹", "摩", "脉", "埋", "没", "泌", "秘", "乜", "模", "牟", "缪", "娜", "呢", "尿", "弄", "坢",
                "膀", "朴", "骠", "屏", "泊", "瀑", "奇", "蹊", "纤", "茜", "鞘", "且", "茄", "扁", "区", "券",
                "若", "挲", "塞", "色", "召", "什", "莘", "省", "氏", "石", "似", "识", "拾", "匙", "熟", "术",
                "属", "数", "说", "忪", "宿", "覃", "镡", "汤", "忒", "提", "拓", "万", "圩", "尾", "尉", "蔚",
                "玟", "挝", "无", "吓", "巷", "削", "校", "邪", "行", "戌", "吁", "血", "殷", "叶", "於", "员",
                "咋", "择", "笮", "轧", "栅", "翟", "粘", "着", "折", "种", "爪", "琢", "仔", "卒", "柞", "盛", "给"
        };

        multiVocalPinYinTable = new String[][] {
                {
                        "ai", "yi", ""
                }, {
                        "ao", "niu", ""
                }, {
                        "ba", "pa", ""
                }, {
                        "bai", "bo", ""
                }, {
                        "bang", "pang", ""
                }, {
                        "bao", "pao", ""
                }, {
                        "bao", "bo", ""
                }, {
                        "bao", "bo", ""
                }, {
                        "bao", "pu", ""
                }, {
                        "can", "shan", "chan"
                }, {
                        "bei", "pi", "po"
                }, {
                        "ben", "hang", ""
                }, {
                        "bi", "pi", ""
                }, {
                        "bian", "pian", ""
                }, {
                        "bo", "bu", ""
                }, {
                        "bo", "bai", ""
                }, {
                        "can", "shen", ""
                }, {
                        "zang", "cang", ""
                }, {
                        "ceng", "zeng", ""
                }, {
                        "cha", "sha", ""
                }, {
                        "cha", "zha", ""
                }, {
                        "cha", "ci", "chai"
                }, {
                        "chan", "shan", ""
                }, {
                        "chang", "zhang", ""
                }, {
                        "chang", "yang", ""
                }, {
                        "chang", "shang", ""
                }, {
                        "chao", "zhao", ""
                }, {
                        "che", "ju", ""
                }, {
                        "chen", "chen", ""
                }, {
                        "cheng", "sheng", ""
                }, {
                        "cheng", "deng", ""
                }, {
                        "chi", "che", ""
                }, {
                        "chi", "di", ""
                }, {
                        "chong", "zhong", ""
                }, {
                        "chou", "qiu", ""
                }, {
                        "chou", "xiu", ""
                }, {
                        "chu", "xu", ""
                }, {
                        "chu", "zhu", "zhe"
                }, {
                        "chuan", "zhuan", ""
                }, {
                        "ci", "si", ""
                }, {
                        "ci", "zi", ""
                }, {
                        "cuan", "zan", ""
                }, {
                        "de", "dei", ""
                }, {
                        "da", "ta", ""
                }, {
                        "da", "dai", ""
                }, {
                        "dan", "shan", "chan"
                }, {
                        "dan", "tan", ""
                }, {
                        "dan", "tan", ""
                }, {
                        "dao", "tao", ""
                }, {
                        "de", "di", ""
                }, {
                        "di", "de", ""
                }, {
                        "dian", "tian", ""
                }, {
                        "diao", "tiao", ""
                }, {
                        "ding", "zheng", ""
                }, {
                        "dou", "du", ""
                }, {
                        "du", "duo", ""
                }, {
                        "dun", "zhuan", ""
                }, {
                        "e", "wu", ""
                }, {
                        "fan", "pan", ""
                }, {
                        "fan", "bo", ""
                }, {
                        "fan", "po", ""
                }, {
                        "feng", "ping", ""
                }, {
                        "feng", "pang", ""
                }, {
                        "fo", "fu", ""
                }, {
                        "fou", "pi", ""
                }, {
                        "fu", "mi", ""
                }, {
                        "fu", "pu", ""
                }, {
                        "ga", "jia", "qie"
                }, {
                        "ka", "ga", ""
                }, {
                        "gai", "jie", ""
                }, {
                        "gai", "ge", ""
                }, {
                        "kang", "gang", ""
                }, {
                        "ge", "yi", ""
                }, {
                        "ge", "ji", ""
                }, {
                        "gu", "jia", ""
                }, {
                        "lun", "guan", ""
                }, {
                        "gui", "jun", "qiu"
                }, {
                        "hai", "huan", ""
                }, {
                        "he", "huo", "hu"
                }, {
                        "huan", "xun", ""
                }, {
                        "hui", "kuai", ""
                }, {
                        "ji", "xi", ""
                }, {
                        "ji", "qi", ""
                }, {
                        "jia", "ga", ""
                }, {
                        "jia", "jie", ""
                }, {
                        "jian", "xian", ""
                }, {
                        "jiang", "xiang", ""
                }, {
                        "jiao", "jue", ""
                }, {
                        "jiao", "jue", ""
                }, {
                        "jiao", "jue", ""
                }, {
                        "jie", "xie", ""
                }, {
                        "jin", "jing", ""
                }, {
                        "jiong", "gui", ""
                }, {
                        "ju", "gou", ""
                }, {
                        "juan", "quan", ""
                }, {
                        "ka", "qia", ""
                }, {
                        "kai", "jie", ""
                }, {
                        "ke", "qiao", ""
                }, {
                        "kui", "wei", ""
                }, {
                        "kui", "hui", ""
                }, {
                        "le", "liao", ""
                }, {
                        "le", "yue", ""
                }, {
                        "lei", "le", ""
                }, {
                        "liang", "lia", ""
                }, {
                        "liang", "jing", ""
                }, {
                        "lou", "lv", ""
                }, {
                        "lu", "liu", ""
                }, {
                        "lu", "lou", ""
                }, {
                        "luo", "lao", "la"
                }, {
                        "luo", "la", ""
                }, {
                        "shuai", "lv", ""
                }, {
                        "lv", "lu", ""
                }, {
                        "ma", "mo", ""
                }, {
                        "ma", "mo", ""
                }, {
                        "mai", "mo", ""
                }, {
                        "man", "mai", ""
                }, {
                        "mei", "mo", ""
                }, {
                        "mi", "bi", ""
                }, {
                        "mi", "bi", ""
                }, {
                        "mie", "nie", ""
                }, {
                        "mo", "mu", ""
                }, {
                        "mou", "mu", ""
                }, {
                        "mou", "miao", "miu"
                }, {
                        "na", "nuo", ""
                }, {
                        "ni", "ne", ""
                }, {
                        "niao", "sui", ""
                }, {
                        "nong", "long", ""
                }, {
                        "pan", "ban", ""
                }, {
                        "pang", "bang", ""
                }, {
                        "piao", "pu", "po"
                }, {
                        "piao", "biao", ""
                }, {
                        "ping", "bing", ""
                }, {
                        "bo", "po", ""
                }, {
                        "pu", "bao", ""
                }, {
                        "qi", "ji", ""
                }, {
                        "qi", "xi", ""
                }, {
                        "qian", "xian", ""
                }, {
                        "qian", "xi", ""
                }, {
                        "qiao", "shao", ""
                }, {
                        "qie", "ju", ""
                }, {
                        "qie", "jia", ""
                }, {
                        "bian", "pian", ""
                }, {
                        "qu", "ou", ""
                }, {
                        "quan", "juan", ""
                }, {
                        "ruo", "re", ""
                }, {
                        "sa", "suo", ""
                }, {
                        "sai", "se", ""
                }, {
                        "se", "shai", ""
                }, {
                        "zhao", "shao", ""
                }, {
                        "shen", "shi", ""
                }, {
                        "shen", "xin", ""
                }, {
                        "sheng", "xing", ""
                }, {
                        "shi", "zhi", ""
                }, {
                        "shi", "dan", ""
                }, {
                        "shi", "si", ""
                }, {
                        "shi", "zhi", ""
                }, {
                        "shi", "she", ""
                }, {
                        "shi", "chi", ""
                }, {
                        "shou", "shu", ""
                }, {
                        "shu", "zhu", ""
                }, {
                        "shu", "zhu", ""
                }, {
                        "shu", "shuo", ""
                }, {
                        "shuo", "shui", "yue"
                }, {
                        "song", "zhong", ""
                }, {
                        "su", "xiu", ""
                }, {
                        "tan", "qin", ""
                }, {
                        "tan", "xin", ""
                }, {
                        "tang", "shang", ""
                }, {
                        "te", "tui", ""
                }, {
                        "ti", "di", ""
                }, {
                        "tuo", "ta", ""
                }, {
                        "wan", "mo", ""
                }, {
                        "wei", "xu", ""
                }, {
                        "wei", "yi", ""
                }, {
                        "wei", "yu", ""
                }, {
                        "wei", "yu", ""
                }, {
                        "wen", "min", ""
                }, {
                        "wo", "zhua", ""
                }, {
                        "wu", "mo", ""
                }, {
                        "xia", "he", ""
                }, {
                        "xiang", "hang", ""
                }, {
                        "xiao", "xue", ""
                }, {
                        "xiao", "jiao", ""
                }, {
                        "xie", "ye", ""
                }, {
                        "xing", "hang", ""
                }, {
                        "xu", "qu", ""
                }, {
                        "yu", "xu", ""
                }, {
                        "xue", "xie", ""
                }, {
                        "yan", "yin", ""
                }, {
                        "ye", "xie", ""
                }, {
                        "yu", "wu", ""
                }, {
                        "yuan", "yun", ""
                }, {
                        "za", "ze", ""
                }, {
                        "ze", "zhai", ""
                }, {
                        "ze", "zuo", ""
                }, {
                        "zha", "ya", "ga"
                }, {
                        "zha", "shan", ""
                }, {
                        "zhai", "di", ""
                }, {
                        "zhan", "nian", ""
                }, {
                        "zhuo", "zhe", ""
                }, {
                        "zhe", "she", ""
                }, {
                        "zhong", "chong", ""
                }, {
                        "zhua", "zhao", ""
                }, {
                        "zhuo", "zuo", ""
                }, {
                        "zi", "zai", ""
                }, {
                        "zu", "cu", ""
                }, {
                        "zuo", "zha", ""
                }, {
                        "sheng", "cheng", ""
                }, {
                        "gei", "ji", ""
                }
        };
    }

    private static void initCode() {
        short[][][] c = {
                initCode1(), initCode2(), initCode3(), initCode4()
        };
        int count = 0;
        for (short[][] a : c)
            count += a.length;

        pinyinCodeIndex = new short[count][];
        int index = 0;
        for (short[][] a : c) {
            for (short[] aa : a) {
                pinyinCodeIndex[index++] = aa;
            }
        }
    }

    private static short[][] initCode1() {
        return new short[][] {
                {
                        483, 389, 458, 273, 262, 50, 395, 88, 350, 232, 482, 24, 182, 172, 178,
                        213, 42, 517, 144, 180, 117, 477, 477, 456, 182, 157, 508, 161, 394, 478,
                        471, 121, 182, 146, 158, 90, 395, 279, 190, 201, 437, 269, 311, 29, 469,
                        472, 326, 386, 276, 341, 410, 103, 65, 39, 507, 141, 122, 243, 235, 477,
                        186, 249, 507, 0, 483, 408, 415, 128, 471, 499, 471, 68, 475, 460, 180,
                        475, 482, 500, 231, 97, 451, 172, 355, 456, 7, 24, 115, 423, 102, 459, 503,
                        159, 147, 25, 44, 501, 389, 361, 108, 263, 341, 455, 474, 112, 55, 450, 81,
                        508, 320, 483, 84, 96, 456, 477, 463, 172, 3, 478, 328, 393, 117, 422, 522,
                        487, 184, 459, 470, 463, 494, 459, 301, 291, 462, 467, 509, 522, 17, 328,
                        477, 408, 477, 506, 147, 250, 510, 26, 351, 18, 502, 59, 473, 500, 18, 459,
                        351, 395, 13, 166, 151, 460, 125, 107, 266, 24, 155, 168, 141, 352, 59,
                        464, 393, 445, 145, 220, 477, 140, 478, 261, 467, 4, 242, 106, 245, 40, 48,
                        470, 509, 366, 175, 408, 69
                },
                {
                        418, 297, 179, 181, 435, 505, 526, 50, 247, 184, 399, 435, 393, 445, 25,
                        278, 461, 443, 483, 457, 467, 140, 209, 456, 477, 117, 232, 167, 479, 459,
                        376, 320, 457, 262, 458, 466, 81, 184, 507, 220, 408, 168, 461, 175, 21,
                        431, 110, 471, 15, 483, 463, 161, 506, 507, 24, 182, 474, 522, 232, 449,
                        234, 55, 520, 0, 125, 432, 399, 258, 421, 515, 464, 333, 339, 122, 232,
                        415, 346, 109, 507, 520, 245, 411, 236, 167, 89, 518, 16, 456, 184, 277,
                        28, 175, 475, 386, 346, 479, 47, 341, 368, 508, 57, 451, 483, 24, 431, 472,
                        112, 422, 455, 98, 45, 394, 191, 81, 40, 15, 498, 165, 474, 500, 521, 472,
                        482, 467, 498, 59, 117, 117, 507, 262, 172, 477, 462, 470, 408, 92, 499,
                        505, 440, 15, 491, 346, 451, 412, 507, 413, 458, 484, 364, 301, 487, 176,
                        249, 83, 422, 149, 178, 457, 388, 341, 353, 46, 51, 376, 15, 461, 481, 474,
                        421, 417, 473, 107, 24, 460, 490, 136, 376, 225, 481, 493, 520, 322, 411,
                        513, 483, 499, 522, 389, 55, 180, 147
                },
                {
                        501, 348, 478, 81, 462, 241, 15, 330, 179, 231, 242, 251, 341, 459, 421,
                        479, 89, 525, 388, 345, 181, 443, 525, 337, 223, 43, 140, 339, 427, 513,
                        451, 172, 25, 166, 57, 434, 388, 474, 111, 459, 483, 98, 235, 25, 136, 459,
                        459, 265, 475, 179, 340, 345, 112, 509, 3, 374, 477, 187, 299, 421, 477,
                        71, 211, 0, 175, 51, 177, 386, 490, 30, 23, 4, 420, 72, 41, 221, 477, 179,
                        341, 259, 456, 297, 349, 291, 43, 234, 247, 213, 13, 483, 21, 491, 507,
                        408, 482, 149, 348, 347, 229, 427, 451, 240, 51, 42, 460, 433, 462, 229,
                        246, 491, 306, 422, 472, 246, 279, 491, 465, 369, 369, 441, 43, 291, 291,
                        179, 472, 395, 396, 343, 0, 150, 393, 90, 9, 134, 165, 456, 369, 232, 483,
                        147, 432, 336, 172, 477, 254, 357, 472, 254, 498, 181, 137, 181, 254, 509,
                        135, 467, 482, 191, 477, 261, 395, 259, 184, 208, 265, 117, 462, 261, 420,
                        123, 161, 317, 117, 265, 340, 175, 412, 257, 441, 136, 180, 348, 89, 122,
                        478, 3, 229, 31, 266, 516, 65
                },
                {
                        408, 97, 179, 235, 457, 91, 108, 108, 184, 51, 506, 112, 271, 507, 112,
                        112, 189, 122, 333, 211, 147, 361, 55, 172, 341, 66, 172, 70, 449, 186,
                        229, 117, 351, 84, 265, 236, 508, 22, 178, 178, 388, 42, 128, 55, 214, 97,
                        106, 178, 59, 180, 90, 246, 494, 484, 67, 194, 386, 55, 67, 229, 110, 42,
                        339, 0, 55, 518, 123, 337, 97, 348, 517, 175, 172, 472, 168, 97, 507, 456,
                        137, 394, 175, 498, 189, 342, 54, 42, 513, 242, 229, 322, 388, 208, 137,
                        162, 498, 517, 231, 184, 237, 141, 177, 141, 175, 175, 439, 172, 175, 175,
                        507, 42, 523, 268, 229, 510, 471, 180, 199, 462, 507, 477, 510, 268, 223,
                        185, 208, 473, 447, 461, 270, 213, 178, 234, 194, 180, 124, 265, 48, 222,
                        481, 194, 185, 348, 242, 26, 220, 189, 262, 89, 467, 456, 477, 470, 473,
                        394, 233, 242, 330, 395, 172, 342, 177, 352, 460, 477, 469, 108, 185, 439,
                        184, 70, 250, 470, 470, 247, 229, 45, 460, 352, 487, 182, 13, 253, 18, 121,
                        121, 477, 322, 184, 474, 125, 98
                },
                {
                        133, 68, 182, 133, 280, 182, 477, 176, 192, 161, 351, 108, 346, 492, 213,
                        161, 483, 141, 166, 70, 214, 231, 231, 414, 91, 182, 351, 457, 194, 472,
                        351, 470, 292, 522, 395, 457, 449, 449, 462, 388, 172, 401, 213, 457, 462,
                        357, 473, 349, 390, 48, 467, 457, 214, 172, 98, 457, 376, 472, 503, 147,
                        471, 81, 499, 0, 318, 2, 346, 471, 507, 252, 431, 391, 435, 524, 110, 494,
                        484, 229, 83, 347, 6, 141, 472, 229, 43, 341, 229, 472, 472, 484, 159, 262,
                        365, 351, 204, 225, 91, 513, 393, 393, 393, 477, 69, 398, 186, 7, 371, 395,
                        517, 458, 461, 172, 487, 369, 61, 137, 350, 48, 93, 159, 264, 252, 468,
                        518, 97, 475, 313, 168, 477, 50, 347, 462, 335, 162, 159, 483, 306, 469,
                        366, 313, 124, 187, 247, 125, 452, 339, 456, 177, 487, 48, 394, 444, 452,
                        98, 395, 185, 321, 452, 270, 357, 81, 395, 509, 434, 457, 477, 339, 333,
                        518, 467, 477, 461, 471, 351, 459, 445, 335, 22, 117, 473, 168, 420, 68,
                        447, 526, 26, 418, 459, 168, 339, 106
                },
                {
                        98, 507, 510, 470, 461, 210, 395, 433, 275, 468, 448, 223, 439, 465, 482,
                        261, 292, 464, 336, 149, 487, 240, 335, 252, 522, 151, 459, 223, 334, 232,
                        7, 264, 247, 415, 117, 147, 485, 482, 136, 136, 15, 147, 477, 341, 441,
                        472, 449, 229, 350, 45, 493, 471, 90, 339, 81, 347, 255, 159, 428, 203,
                        232, 222, 386, 0, 519, 455, 478, 339, 447, 342, 4, 494, 292, 483, 432, 220,
                        457, 3, 300, 517, 499, 488, 461, 460, 516, 456, 452, 431, 136, 339, 339,
                        70, 475, 518, 441, 65, 151, 471, 339, 503, 232, 459, 479, 137, 494, 143,
                        246, 290, 81, 352, 445, 130, 422, 4, 70, 483, 503, 509, 41, 448, 483, 491,
                        474, 262, 161, 487, 164, 484, 172, 508, 451, 386, 467, 165, 498, 472, 232,
                        483, 377, 189, 345, 472, 388, 321, 416, 480, 451, 479, 327, 15, 131, 493,
                        168, 431, 474, 461, 342, 379, 481, 159, 462, 249, 40, 145, 366, 447, 172,
                        318, 456, 459, 518, 242, 447, 174, 417, 60, 374, 132, 276, 342, 18, 6, 231,
                        524, 510, 268, 421, 177, 49, 177, 189
                },
                {
                        421, 393, 3, 461, 241, 461, 161, 166, 143, 467, 459, 494, 43, 334, 73, 249,
                        161, 119, 422, 475, 374, 177, 461, 162, 250, 357, 461, 461, 172, 214, 461,
                        149, 248, 345, 467, 445, 421, 470, 456, 525, 108, 189, 166, 30, 55, 488,
                        70, 483, 444, 457, 339, 149, 231, 467, 166, 478, 470, 474, 408, 472, 479,
                        68, 500, 0, 517, 299, 485, 462, 345, 484, 3, 481, 451, 483, 321, 72, 463,
                        96, 71, 463, 328, 478, 524, 297, 81, 221, 418, 455, 458, 475, 97, 466, 509,
                        499, 179, 43, 470, 256, 507, 242, 166, 319, 482, 474, 478, 480, 257, 159,
                        503, 229, 237, 145, 279, 268, 472, 229, 242, 240, 268, 70, 46, 332, 328,
                        460, 256, 457, 97, 209, 472, 42, 479, 86, 219, 418, 461, 58, 164, 168, 513,
                        503, 461, 498, 229, 42, 41, 229, 477, 246, 491, 413, 156, 496, 175, 488,
                        510, 221, 295, 356, 239, 166, 478, 296, 442, 192, 484, 181, 329, 487, 61,
                        166, 98, 143, 439, 441, 143, 354, 363, 143, 420, 143, 478, 167, 147, 245,
                        143, 56, 451, 484, 352, 209, 337
                },
                {
                        484, 484, 471, 442, 441, 441, 442, 244, 166, 477, 243, 243, 471, 441, 435,
                        337, 242, 211, 471, 516, 413, 413, 517, 71, 340, 458, 388, 295, 268, 173,
                        507, 470, 477, 347, 257, 364, 444, 111, 18, 464, 221, 180, 172, 81, 464,
                        317, 422, 351, 517, 137, 420, 181, 473, 115, 242, 350, 135, 469, 7, 236,
                        510, 117, 161, 0, 507, 6, 69, 319, 265, 172, 151, 247, 59, 48, 478, 160,
                        94, 502, 117, 140, 474, 97, 141, 40, 473, 462, 398, 24, 159, 68, 188, 71,
                        148, 4, 464, 459, 12, 335, 15, 477, 478, 147, 467, 515, 347, 112, 109, 353,
                        481, 187, 458, 81, 222, 185, 347, 503, 234, 162, 26, 181, 475, 81, 471,
                        352, 415, 506, 449, 184, 245, 506, 206, 389, 89, 421, 28, 440, 17, 459, 97,
                        477, 507, 516, 339, 184, 291, 194, 215, 291, 175, 123, 483, 471, 136, 228,
                        109, 471, 215, 4, 393, 280, 441, 47, 164, 18, 231, 455, 513, 13, 483, 456,
                        178, 368, 475, 128, 520, 483, 165, 98, 474, 117, 172, 257, 389, 445, 478,
                        112, 508, 178, 179, 155, 123
                },
                {
                        57, 459, 333, 225, 464, 165, 92, 449, 468, 457, 172, 211, 479, 481, 189,
                        413, 395, 261, 453, 47, 441, 353, 508, 229, 322, 12, 492, 94, 505, 456,
                        506, 470, 505, 3, 133, 472, 191, 452, 462, 237, 145, 222, 389, 322, 17, 46,
                        242, 242, 313, 341, 257, 268, 513, 403, 241, 21, 33, 507, 501, 191, 83, 46,
                        517, 0, 172, 143, 342, 347, 81, 65, 472, 418, 497, 341, 451, 515, 345, 388,
                        388, 110, 337, 443, 442, 108, 353, 96, 525, 81, 394, 166, 97, 421, 79, 456,
                        111, 165, 421, 68, 475, 510, 175, 483, 342, 345, 198, 477, 328, 83, 176,
                        475, 469, 421, 221, 184, 163, 71, 358, 341, 470, 459, 457, 3, 471, 72, 368,
                        179, 247, 213, 242, 472, 421, 451, 166, 240, 240, 369, 229, 235, 42, 470,
                        472, 225, 7, 449, 376, 514, 477, 250, 510, 514, 161, 215, 161, 467, 215,
                        398, 252, 96, 398, 477, 479, 176, 318, 499, 20, 415, 354, 236, 67, 468,
                        462, 280, 458, 484, 449, 507, 348, 310, 135, 339, 259, 259, 46, 494, 186,
                        124, 423, 420, 472, 18, 169
                },
                {
                        487, 462, 7, 100, 431, 319, 185, 462, 83, 473, 164, 189, 498, 16, 165, 110,
                        84, 470, 199, 6, 453, 420, 456, 6, 176, 231, 97, 487, 176, 395, 111, 168,
                        18, 243, 97, 435, 341, 182, 302, 40, 459, 108, 172, 159, 70, 482, 180, 178,
                        452, 508, 314, 199, 508, 487, 328, 48, 485, 514, 472, 278, 463, 111, 112,
                        0, 484, 91, 25, 517, 502, 291, 484, 440, 468, 507, 98, 268, 18, 393, 98,
                        151, 467, 107, 506, 265, 11, 117, 236, 518, 357, 459, 473, 251, 518, 184,
                        361, 89, 172, 121, 460, 168, 185, 135, 175, 175, 292, 507, 505, 459, 155,
                        140, 470, 210, 472, 266, 234, 320, 471, 482, 472, 459, 431, 447, 352, 411,
                        159, 459, 390, 394, 462, 252, 117, 456, 194, 220, 63, 435, 464, 278, 483,
                        334, 415, 507, 147, 514, 333, 443, 459, 483, 472, 456, 457, 472, 483, 408,
                        229, 184, 515, 339, 459, 517, 89, 242, 98, 98, 247, 262, 61, 335, 184, 28,
                        236, 461, 399, 339, 166, 117, 455, 455, 421, 110, 110, 432, 291, 352, 180,
                        180, 341, 83, 464, 161, 449
                },
                {
                        220, 478, 509, 280, 117, 245, 4, 215, 478, 471, 184, 229, 83, 459, 162,
                        162, 473, 474, 278, 371, 173, 483, 483, 451, 431, 365, 257, 70, 368, 348,
                        166, 455, 341, 57, 263, 117, 178, 92, 477, 508, 165, 262, 472, 479, 468,
                        178, 451, 506, 350, 507, 462, 445, 231, 254, 357, 408, 329, 451, 447, 63,
                        161, 346, 13, 0, 467, 483, 141, 521, 474, 484, 364, 366, 237, 257, 317,
                        487, 249, 214, 393, 505, 484, 417, 364, 266, 479, 413, 294, 423, 318, 222,
                        280, 13, 328, 477, 483, 468, 484, 477, 192, 481, 291, 524, 179, 513, 494,
                        278, 223, 503, 161, 161, 6, 368, 249, 331, 136, 456, 345, 445, 500, 263,
                        459, 459, 268, 233, 231, 162, 141, 79, 507, 467, 477, 162, 457, 214, 474,
                        472, 42, 177, 257, 117, 108, 472, 477, 152, 177, 117, 395, 415, 342, 231,
                        468, 463, 294, 89, 477, 30, 3, 293, 297, 249, 433, 50, 179, 59, 483, 332,
                        364, 366, 291, 472, 420, 479, 341, 485, 262, 18, 393, 464, 291, 91, 237,
                        484, 221, 472, 236, 177, 358, 221, 459, 479, 403
                },
                {
                        462, 352, 261, 229, 243, 472, 510, 221, 186, 518, 463, 408, 420, 482, 513,
                        470, 264, 61, 449, 471, 477, 518, 229, 469, 25, 277, 295, 479, 243, 364,
                        349, 441, 365, 474, 477, 180, 516, 510, 159, 395, 477, 433, 457, 47, 354,
                        133, 461, 498, 395, 393, 165, 261, 208, 28, 491, 484, 350, 151, 505, 175,
                        297, 24, 164, 0, 347, 395, 297, 179, 297, 507, 483, 13, 212, 297, 247, 347,
                        161, 507, 297, 393, 451, 462, 212, 166, 187, 477, 477, 13, 347, 240, 13,
                        112, 247, 94, 334, 513, 334, 194, 473, 513, 470, 510, 94, 72, 178, 261,
                        261, 399, 389, 268, 233, 459, 459, 215, 482, 294, 318, 450, 450, 450, 474,
                        97, 214, 508, 122, 136, 512, 122, 122, 457, 18, 178, 432, 84, 395, 505,
                        462, 291, 457, 446, 251, 241, 333, 462, 110, 462, 247, 35, 462, 184, 186,
                        233, 186, 510, 462, 334, 447, 459, 229, 472, 72, 166, 240, 361, 456, 147,
                        393, 51, 476, 485, 11, 474, 5, 456, 178, 172, 111, 449, 341, 339, 178, 526,
                        526, 473, 184, 123, 469, 334, 229, 433, 522
                },
                {
                        117, 445, 328, 6, 213, 351, 334, 433, 236, 48, 333, 37, 12, 439, 469, 20,
                        151, 194, 246, 98, 295, 85, 242, 100, 106, 121, 352, 477, 271, 395, 4, 451,
                        164, 261, 229, 172, 439, 451, 482, 136, 234, 474, 177, 98, 475, 26, 354,
                        112, 280, 229, 482, 459, 364, 72, 393, 47, 441, 128, 124, 458, 478, 483,
                        279, 0, 191, 472, 353, 49, 418, 235, 162, 184, 220, 265, 215, 215, 522,
                        136, 471, 123, 245, 245, 236, 97, 506, 478, 89, 147, 506, 451, 328, 178,
                        522, 209, 89, 478, 518, 494, 165, 483, 473, 112, 350, 473, 431, 477, 507,
                        395, 98, 510, 500, 247, 472, 257, 147, 172, 164, 435, 456, 483, 520, 221,
                        194, 472, 472, 451, 520, 40, 417, 194, 347, 431, 441, 94, 457, 453, 31,
                        422, 479, 178, 189, 237, 456, 345, 17, 83, 341, 481, 295, 67, 395, 371,
                        411, 520, 176, 233, 192, 42, 85, 34, 87, 441, 241, 500, 500, 6, 32, 351,
                        342, 524, 524, 72, 72, 457, 483, 328, 240, 460, 506, 25, 347, 177, 472,
                        223, 500, 233, 233, 347, 97, 525, 345
                },
                {
                        186, 474, 177, 474, 186, 500, 477, 469, 280, 475, 475, 477, 295, 472, 172,
                        462, 194, 457, 81, 6, 524, 451, 364, 72, 236, 178, 483, 485, 478, 366, 178,
                        234, 457, 240, 240, 479, 457, 184, 42, 479, 451, 472, 280, 352, 44, 64,
                        243, 83, 83, 295, 472, 472, 472, 280, 472, 211, 165, 464, 44, 234, 200,
                        337, 337, 0, 350, 507, 502, 477, 179, 416, 352, 324, 334, 488, 87, 295,
                        111, 314, 507, 161, 70, 69, 447, 117, 268, 477, 477, 333, 340, 185, 366,
                        401, 404, 345, 505, 395, 354, 69, 141, 333, 501, 376, 449, 69, 386, 339,
                        91, 160, 506, 467, 451, 477, 12, 333, 85, 133, 317, 423, 261, 173, 427,
                        166, 508, 393, 21, 143, 494, 271, 12, 180, 42, 507, 456, 18, 18, 501, 261,
                        345, 42, 111, 259, 12, 72, 264, 51, 178, 459, 221, 175, 24, 122, 172, 435,
                        494, 140, 256, 347, 444, 471, 463, 178, 514, 471, 59, 439, 477, 507, 433,
                        507, 461, 441, 141, 209, 259, 482, 26, 24, 47, 220, 172, 411, 399, 348,
                        483, 263, 412, 494, 460, 110, 182, 98
                },
                {
                        451, 237, 458, 412, 507, 26, 348, 182, 182, 241, 478, 457, 242, 477, 51,
                        441, 408, 463, 263, 43, 456, 110, 213, 207, 211, 18, 379, 235, 233, 247,
                        172, 479, 459, 435, 481, 229, 435, 472, 81, 334, 166, 277, 166, 111, 351,
                        472, 492, 477, 106, 376, 106, 395, 84, 161, 456, 443, 176, 7, 393, 501,
                        423, 117, 81, 0, 44, 505, 477, 352, 390, 484, 180, 84, 501, 176, 342, 322,
                        18, 391, 421, 175, 125, 107, 18, 208, 175, 22, 461, 421, 143, 342, 159,
                        291, 143, 449, 186, 172, 242, 166, 166, 477, 477, 477, 485, 485, 452, 472,
                        483, 48, 84, 481, 330, 48, 517, 477, 172, 508, 450, 81, 236, 117, 450, 457,
                        450, 506, 507, 180, 61, 507, 61, 446, 172, 507, 520, 509, 220, 462, 178,
                        175, 431, 458, 329, 117, 451, 318, 457, 506, 330, 431, 73, 507, 22, 508,
                        45, 474, 166, 257, 240, 460, 13, 351, 224, 361, 435, 121, 361, 147, 477,
                        420, 457, 108, 479, 452, 452, 456, 172, 457, 178, 449, 61, 483, 395, 65,
                        420, 516, 347, 167, 465, 450, 459, 192, 184
                },
                {
                        477, 13, 265, 320, 208, 11, 298, 500, 161, 522, 482, 81, 443, 482, 117,
                        457, 18, 482, 468, 61, 24, 165, 469, 328, 399, 457, 421, 481, 268, 205,
                        395, 457, 223, 155, 213, 270, 507, 462, 474, 85, 149, 451, 467, 461, 408,
                        210, 350, 166, 48, 477, 465, 138, 235, 48, 389, 513, 485, 322, 73, 166,
                        461, 252, 481, 0, 484, 328, 485, 483, 483, 194, 503, 235, 177, 346, 431,
                        26, 341, 457, 258, 477, 155, 47, 456, 229, 232, 61, 477, 229, 280, 98, 456,
                        61, 73, 43, 258, 229, 139, 139, 339, 206, 432, 245, 457, 191, 142, 291, 96,
                        143, 500, 484, 179, 172, 235, 483, 151, 352, 421, 431, 295, 58, 258, 463,
                        456, 417, 520, 175, 481, 73, 280, 487, 434, 57, 349, 33, 20, 167, 520, 431,
                        15, 468, 451, 125, 451, 18, 468, 164, 262, 481, 339, 422, 478, 463, 254,
                        340, 194, 3, 346, 472, 368, 520, 408, 479, 353, 159, 487, 413, 339, 474,
                        411, 165, 172, 136, 184, 55, 291, 462, 189, 506, 481, 32, 470, 393, 25,
                        457, 462, 167, 481, 473, 229, 378, 423
                },
                {
                        478, 467, 420, 487, 266, 133, 61, 330, 271, 143, 48, 30, 30, 30, 65, 265,
                        501, 439, 6, 403, 139, 353, 493, 182, 231, 313, 411, 347, 478, 247, 389,
                        442, 251, 459, 391, 348, 81, 507, 247, 185, 339, 339, 483, 333, 233, 411,
                        482, 49, 507, 439, 47, 339, 351, 322, 15, 349, 177, 48, 231, 333, 214, 166,
                        506, 0, 478, 478, 457, 457, 70, 421, 97, 444, 413, 186, 33, 461, 108, 111,
                        223, 223, 459, 265, 233, 456, 30, 186, 63, 459, 421, 394, 328, 477, 459,
                        299, 70, 421, 180, 411, 177, 451, 468, 347, 347, 184, 378, 198, 479, 477,
                        235, 379, 187, 163, 3, 475, 216, 458, 48, 483, 478, 69, 259, 291, 259, 94,
                        339, 268, 459, 258, 50, 507, 306, 51, 473, 25, 507, 213, 213, 482, 117,
                        237, 264, 47, 166, 42, 221, 163, 468, 358, 42, 172, 184, 164, 391, 231,
                        278, 268, 422, 186, 514, 514, 485, 125, 175, 89, 85, 28, 173, 507, 214,
                        500, 342, 125, 175, 483, 482, 457, 500, 457, 457, 351, 161, 161, 98, 477,
                        431, 254, 83, 389, 477, 477, 472
                },
                {
                        350, 229, 108, 366, 490, 501, 485, 483, 456, 147, 393, 498, 477, 339, 456,
                        78, 361, 457, 347, 173, 483, 6, 503, 507, 507, 78, 472, 450, 20, 184, 452,
                        161, 485, 347, 393, 506, 487, 449, 369, 335, 335, 7, 298, 494, 487, 24,
                        507, 278, 337, 474, 505, 498, 473, 340, 291, 475, 48, 328, 173, 257, 351,
                        51, 471, 0, 500, 319, 276, 341, 445, 8, 507, 184, 216, 340, 341, 154, 296,
                        133, 525, 477, 462, 379, 166, 8, 507, 216, 97, 97, 299, 505, 151, 177, 89,
                        366, 234, 498, 242, 391, 186, 234, 184, 471, 459, 483, 472, 25, 128, 431,
                        47, 417, 341, 257, 299, 184, 322, 175, 472, 415, 462, 498, 112, 209, 350,
                        168, 441, 335, 494, 412, 483, 517, 449, 507, 525, 512, 499, 242, 412, 472,
                        12, 451, 449, 347, 391, 265, 258, 117, 72, 455, 352, 485, 520, 432, 441,
                        16, 455, 526, 458, 339, 47, 378, 245, 348, 123, 81, 167, 339, 399, 449,
                        236, 471, 506, 232, 137, 477, 467, 472, 506, 28, 24, 431, 521, 198, 398,
                        178, 266, 128, 259, 378, 322, 306, 175
                },
                {
                        506, 482, 341, 472, 278, 468, 328, 451, 374, 295, 395, 520, 505, 470, 481,
                        20, 473, 164, 472, 523, 467, 340, 172, 431, 219, 219, 46, 182, 441, 167,
                        127, 89, 461, 462, 341, 498, 15, 474, 451, 77, 456, 520, 127, 135, 347,
                        364, 353, 521, 416, 416, 364, 322, 194, 474, 72, 507, 306, 462, 350, 459,
                        179, 264, 477, 0, 94, 388, 418, 498, 334, 229, 423, 209, 507, 447, 458,
                        452, 342, 432, 505, 98, 306, 352, 498, 456, 503, 192, 364, 387, 416, 417,
                        233, 49, 55, 143, 322, 507, 339, 412, 231, 47, 48, 139, 242, 241, 520, 457,
                        161, 511, 342, 422, 162, 507, 342, 141, 479, 345, 507, 295, 251, 42, 313,
                        51, 413, 513, 177, 388, 341, 330, 176, 474, 135, 341, 172, 331, 223, 96,
                        459, 371, 141, 496, 477, 470, 47, 461, 159, 140, 418, 292, 235, 506, 451,
                        193, 172, 32, 463, 421, 107, 45, 186, 461, 16, 268, 517, 451, 337, 347, 96,
                        162, 177, 418, 474, 511, 231, 481, 279, 242, 517, 499, 337, 58, 457, 71,
                        379, 348, 178, 211, 388, 462, 498, 6, 184
                },
                {
                        475, 98, 259, 261, 172, 420, 72, 221, 184, 475, 366, 475, 475, 291, 455,
                        178, 23, 297, 125, 507, 507, 422, 268, 175, 462, 234, 421, 8, 412, 242,
                        485, 359, 507, 473, 225, 372, 399, 64, 292, 459, 320, 229, 220, 164, 479,
                        246, 240, 341, 341, 341, 221, 459, 479, 257, 388, 479, 64, 462, 503, 246,
                        257, 268, 48, 0, 523, 243, 421, 387, 83, 447, 422, 177, 221, 246, 141, 141,
                        339, 470, 193, 477, 147, 11, 334, 83, 208, 265, 456, 151, 33, 398, 143,
                        467, 177, 46, 505, 97, 483, 8, 467, 97, 295, 83, 353, 477, 194, 472, 339,
                        440, 461, 97, 473, 458, 265, 510, 3, 81, 505, 399, 233, 351, 465, 477, 177,
                        388, 177, 517, 477, 231, 18, 420, 461, 461, 469, 339, 339, 186, 499, 446,
                        11, 483, 221, 451, 394, 173, 173, 483, 177, 440, 90, 507, 342, 351, 500,
                        517, 517, 517, 347, 235, 517, 51, 92, 510, 178, 148, 320, 482, 272, 339,
                        328, 237, 117, 109, 180, 502, 477, 390, 175, 105, 507, 108, 330, 108, 500,
                        211, 415, 483, 172, 172, 168, 462, 433
                },
                {
                        479, 81, 467, 42, 473, 395, 265, 265, 516, 57, 456, 15, 11, 178, 394, 161,
                        109, 181, 468, 111, 347, 161, 472, 494, 109, 393, 184, 473, 109, 468, 334,
                        505, 236, 149, 268, 20, 467, 167, 520, 458, 110, 477, 452, 89, 3, 24, 5,
                        240, 213, 433, 395, 165, 468, 214, 467, 177, 179, 507, 439, 159, 121, 460,
                        147, 0, 187, 459, 215, 509, 457, 394, 503, 503, 147, 149, 449, 432, 517,
                        524, 509, 388, 291, 457, 339, 506, 477, 472, 449, 235, 43, 450, 461, 110,
                        468, 477, 487, 166, 467, 265, 475, 479, 399, 451, 348, 254, 278, 221, 473,
                        57, 474, 417, 337, 177, 189, 149, 453, 43, 339, 149, 472, 229, 172, 258,
                        491, 462, 149, 268, 61, 291, 501, 166, 147, 468, 54, 233, 421, 180, 331,
                        235, 457, 477, 178, 165, 69, 475, 475, 229, 421, 439, 461, 110, 393, 502,
                        149, 477, 460, 464, 388, 177, 13, 180, 472, 3, 475, 366, 259, 229, 46, 213,
                        85, 446, 474, 168, 247, 364, 240, 246, 243, 387, 422, 472, 510, 485, 477,
                        161, 399, 32, 394, 497, 341, 467, 216
                },
                {
                        478, 346, 111, 328, 111, 236, 209, 446, 307, 433, 222, 524, 266, 450, 444,
                        339, 479, 520, 450, 439, 222, 223, 240, 332, 399, 429, 361, 7, 97, 433,
                        229, 350, 182, 18, 505, 59, 366, 341, 483, 456, 470, 108, 518, 361, 503,
                        97, 252, 48, 477, 125, 133, 507, 176, 388, 449, 182, 252, 507, 484, 110,
                        89, 459, 57, 0, 399, 162, 463, 298, 50, 411, 485, 179, 172, 254, 328, 5,
                        111, 477, 117, 278, 161, 475, 510, 463, 474, 98, 502, 487, 524, 394, 97,
                        242, 411, 278, 505, 420, 457, 138, 117, 268, 477, 475, 475, 395, 295, 18,
                        477, 291, 219, 151, 317, 108, 508, 69, 59, 479, 117, 399, 390, 411, 393,
                        357, 481, 507, 522, 70, 136, 337, 6, 117, 117, 526, 22, 498, 408, 510, 320,
                        395, 138, 40, 474, 182, 237, 257, 229, 364, 493, 373, 481, 470, 339, 505,
                        477, 460, 505, 229, 379, 191, 24, 361, 8, 361, 24, 518, 50, 477, 59, 525,
                        524, 106, 106, 483, 107, 133, 500, 229, 478, 507, 395, 117, 366, 477, 209,
                        349, 377, 469, 97, 56, 483, 491, 446
                },
                {
                        479, 178, 237, 500, 470, 372, 505, 15, 479, 216, 180, 334, 16, 369, 457,
                        222, 237, 112, 339, 452, 187, 147, 478, 350, 482, 240, 322, 514, 81, 469,
                        441, 493, 482, 18, 81, 147, 507, 361, 15, 459, 164, 449, 306, 173, 433,
                        172, 461, 247, 212, 34, 111, 411, 408, 90, 347, 479, 184, 215, 517, 42,
                        451, 180, 229, 0, 24, 458, 115, 423, 507, 220, 231, 517, 229, 339, 24, 245,
                        411, 341, 339, 28, 42, 503, 110, 320, 335, 167, 47, 493, 234, 483, 483,
                        136, 142, 89, 123, 450, 67, 108, 47, 500, 339, 484, 472, 483, 352, 477,
                        393, 457, 517, 413, 220, 521, 521, 111, 46, 348, 295, 449, 242, 149, 346,
                        509, 184, 184, 47, 526, 342, 471, 328, 517, 518, 23, 322, 87, 51, 43, 258,
                        162, 175, 141, 457, 72, 141, 83, 507, 352, 274, 117, 128, 322, 388, 477,
                        393, 97, 117, 451, 451, 173, 520, 175, 477, 457, 472, 472, 500, 483, 151,
                        455, 329, 18, 474, 210, 467, 371, 473, 219, 472, 16, 166, 214, 178, 214,
                        408, 112, 445, 507, 271, 254, 209, 161, 435
                },
                {
                        483, 482, 411, 484, 473, 505, 329, 475, 340, 475, 405, 483, 451, 257, 431,
                        172, 178, 365, 165, 224, 352, 460, 395, 421, 487, 393, 51, 328, 173, 477,
                        505, 117, 306, 261, 136, 179, 418, 474, 462, 484, 518, 266, 413, 173, 474,
                        178, 165, 147, 341, 249, 484, 364, 395, 507, 452, 435, 364, 422, 499, 408,
                        394, 194, 457, 0, 136, 339, 193, 416, 317, 423, 125, 57, 505, 300, 172,
                        178, 342, 459, 257, 467, 123, 517, 445, 345, 473, 83, 173, 507, 72, 240,
                        377, 457, 172, 231, 166, 481, 341, 143, 121, 121, 442, 162, 393, 524, 322,
                        482, 176, 161, 164, 141, 477, 477, 124, 192, 141, 141, 449, 507, 514, 487,
                        222, 46, 520, 229, 466, 348, 403, 439, 139, 494, 413, 225, 242, 232, 261,
                        247, 177, 413, 194, 21, 242, 233, 503, 498, 399, 251, 294, 473, 433, 322,
                        510, 386, 352, 175, 61, 172, 472, 469, 507, 470, 507, 524, 61, 337, 399,
                        162, 214, 505, 388, 457, 57, 83, 110, 268, 456, 359, 235, 237, 345, 459,
                        370, 108, 500, 223, 487, 405, 443, 47, 422, 259, 461
                },
                {
                        186, 463, 166, 172, 306, 445, 297, 369, 439, 497, 111, 349, 472, 155, 347,
                        136, 237, 223, 124, 457, 394, 518, 376, 172, 90, 180, 175, 51, 68, 399,
                        176, 235, 280, 478, 166, 388, 524, 468, 47, 122, 184, 524, 477, 337, 112,
                        166, 71, 172, 415, 333, 47, 51, 511, 166, 172, 178, 173, 499, 175, 342, 72,
                        477, 21, 0, 411, 391, 229, 478, 423, 420, 262, 339, 442, 24, 168, 172, 341,
                        291, 297, 477, 124, 191, 478, 368, 348, 472, 339, 261, 502, 141, 57, 172,
                        214, 334, 79, 51, 125, 262, 482, 507, 165, 341, 225, 234, 372, 242, 229,
                        64, 247, 264, 166, 313, 247, 507, 124, 91, 484, 485, 110, 517, 412, 231,
                        176, 51, 348, 510, 247, 472, 229, 510, 347, 178, 98, 413, 163, 295, 483,
                        240, 220, 177, 459, 141, 184, 466, 236, 479, 388, 478, 482, 479, 460, 299,
                        25, 500, 231, 184, 403, 391, 524, 61, 352, 351, 31, 183, 483, 246, 229,
                        523, 243, 422, 186, 472, 221, 221, 510, 246, 229, 7, 279, 483, 236, 140,
                        477, 459, 467, 44, 457, 339, 194, 478, 186
                },
                {
                        457, 467, 458, 214, 222, 463, 412, 462, 467, 53, 478, 341, 463, 341, 54,
                        137, 478, 483, 461, 475, 473, 421, 354, 313, 161, 461, 164, 467, 321, 477,
                        461, 467, 446, 231, 51, 477, 98, 483, 58, 164, 26, 26, 184, 341, 507, 379,
                        48, 379, 508, 417, 415, 229, 494, 483, 229, 214, 98, 503, 452, 268, 474,
                        394, 467, 0, 186, 340, 350, 413, 348, 477, 475, 475, 30, 258, 85, 505, 487,
                        452, 50, 431, 179, 389, 478, 84, 182, 214, 64, 70, 91, 176, 231, 23, 91,
                        175, 175, 510, 394, 477, 462, 353, 345, 474, 470, 166, 353, 339, 351, 166,
                        92, 477, 461, 139, 257, 3, 178, 328, 42, 446, 446, 328, 234, 173, 374, 271,
                        445, 470, 106, 364, 459, 184, 350, 306, 446, 320, 184, 97, 18, 376, 254,
                        415, 399, 445, 194, 418, 376, 399, 271, 254, 439, 364, 500, 378, 500, 259,
                        242, 85, 186, 339, 473, 282, 23, 393, 457, 457, 348, 471, 89, 473, 487,
                        506, 24, 71, 404, 224, 291, 108, 350, 314, 494, 262, 84, 517, 54, 449, 108,
                        69, 445, 252, 482, 332, 341
                },
                {
                        483, 483, 441, 182, 507, 507, 341, 180, 180, 444, 187, 159, 352, 20, 147,
                        508, 318, 469, 165, 482, 467, 467, 487, 472, 70, 482, 161, 168, 307, 268,
                        456, 49, 318, 18, 507, 317, 518, 488, 237, 494, 112, 257, 488, 445, 505,
                        505, 477, 107, 432, 408, 213, 479, 184, 477, 173, 508, 166, 16, 494, 510,
                        482, 136, 161, 0, 333, 518, 507, 413, 47, 408, 184, 469, 394, 469, 117,
                        172, 139, 70, 478, 509, 475, 166, 490, 47, 451, 160, 175, 408, 106, 464,
                        117, 518, 507, 478, 456, 193, 446, 472, 431, 270, 225, 477, 261, 352, 334,
                        461, 477, 413, 213, 346, 184, 333, 465, 507, 165, 266, 456, 351, 477, 180,
                        395, 323, 42, 179, 234, 350, 451, 147, 252, 482, 25, 90, 159, 477, 506,
                        221, 147, 229, 128, 231, 57, 159, 477, 439, 223, 458, 49, 181, 415, 47,
                        320, 459, 393, 215, 333, 147, 348, 361, 441, 461, 435, 98, 487, 229, 404,
                        408, 225, 404, 91, 487, 155, 464, 423, 58, 501, 279, 484, 445, 89, 455,
                        184, 391, 232, 167, 418, 346, 73, 185, 161, 143, 472
                },
                {
                        509, 322, 149, 43, 341, 109, 48, 242, 184, 229, 503, 333, 432, 483, 291,
                        242, 261, 180, 236, 245, 351, 483, 393, 161, 161, 484, 220, 348, 341, 507,
                        478, 334, 16, 484, 452, 371, 110, 484, 194, 339, 391, 379, 339, 328, 457,
                        484, 365, 164, 175, 302, 456, 435, 112, 455, 431, 451, 368, 33, 151, 472,
                        159, 261, 254, 0, 479, 472, 348, 394, 257, 490, 167, 277, 141, 48, 98, 231,
                        339, 339, 257, 432, 62, 451, 30, 265, 334, 467, 172, 175, 112, 477, 478,
                        395, 462, 506, 421, 483, 18, 265, 395, 441, 394, 481, 184, 439, 442, 350,
                        350, 473, 240, 168, 484, 278, 317, 482, 352, 514, 232, 42, 472, 516, 151,
                        518, 258, 479, 219, 112, 241, 451, 458, 479, 334, 179, 472, 417, 484, 459,
                        474, 259, 517, 47, 420, 418, 447, 208, 378, 498, 395, 245, 249, 451, 490,
                        456, 452, 342, 494, 395, 3, 487, 478, 413, 417, 395, 3, 317, 467, 453, 31,
                        264, 125, 469, 165, 462, 81, 507, 479, 178, 125, 415, 177, 166, 478, 494,
                        403, 57, 461, 483, 466, 161, 18, 21, 507
                },
                {
                        176, 208, 393, 389, 261, 6, 242, 467, 482, 42, 108, 481, 142, 258, 348,
                        483, 172, 471, 44, 457, 172, 242, 240, 179, 143, 411, 507, 121, 342, 177,
                        61, 57, 513, 313, 427, 475, 457, 261, 422, 422, 421, 231, 447, 420, 122,
                        322, 518, 192, 322, 501, 514, 467, 216, 341, 472, 403, 461, 65, 431, 176,
                        520, 479, 159, 0, 463, 399, 164, 520, 215, 467, 507, 331, 399, 345, 334,
                        473, 166, 178, 456, 314, 172, 451, 461, 341, 471, 457, 416, 96, 265, 370,
                        413, 505, 520, 477, 507, 449, 421, 478, 462, 475, 498, 376, 152, 18, 42,
                        399, 337, 235, 451, 379, 379, 47, 181, 162, 280, 223, 66, 159, 147, 487,
                        237, 159, 117, 149, 151, 459, 175, 388, 457, 483, 242, 297, 483, 235, 394,
                        71, 164, 494, 462, 483, 395, 469, 236, 449, 518, 481, 211, 30, 231, 83,
                        475, 468, 505, 251, 70, 477, 415, 328, 184, 418, 347, 517, 299, 455, 347,
                        321, 379, 386, 451, 51, 418, 411, 435, 379, 510, 231, 291, 457, 399, 261,
                        297, 479, 479, 259, 179, 339, 339, 524, 455, 423, 478
                },
                {
                        478, 94, 59, 168, 348, 221, 470, 194, 451, 23, 136, 341, 479, 23, 216, 110,
                        31, 256, 491, 451, 334, 491, 242, 229, 482, 473, 242, 408, 507, 479, 91,
                        450, 166, 462, 317, 393, 21, 42, 268, 237, 175, 379, 47, 136, 23, 168, 459,
                        242, 347, 364, 229, 180, 461, 479, 415, 451, 448, 469, 510, 403, 220, 94,
                        108, 0, 161, 220, 399, 236, 479, 291, 172, 231, 525, 479, 235, 477, 175,
                        42, 69, 358, 175, 221, 108, 403, 484, 517, 112, 391, 225, 221, 61, 351,
                        481, 341, 107, 186, 472, 479, 459, 491, 243, 472, 229, 261, 388, 421, 71,
                        177, 42, 479, 149, 510, 221, 221, 279, 449, 243, 470, 459, 472, 122, 472,
                        483, 140, 461, 461, 166, 159, 513, 498, 462, 48, 490, 339, 508, 111, 298,
                        452, 337, 477, 328, 189, 317, 472, 318, 271, 233, 140, 463, 140, 140, 20,
                        68, 458, 506, 510, 194, 502, 117, 7, 462, 462, 236, 517, 319, 420, 473,
                        439, 388, 451, 165, 509, 474, 467, 155, 352, 164, 466, 466, 459, 478, 471,
                        509, 474, 395, 451, 439, 469, 490, 189, 458
                },
        };
    }

    private static short[][] initCode2() {
        return new short[][] {
                {
                        468, 506, 334, 166, 140, 45, 166, 46, 446, 234, 117, 181, 462, 337, 435,
                        517, 435, 145, 222, 472, 467, 48, 364, 161, 457, 399, 168, 470, 209, 485,
                        461, 457, 514, 351, 81, 462, 339, 446, 247, 472, 184, 235, 215, 167, 444,
                        457, 65, 456, 159, 184, 117, 455, 61, 112, 333, 349, 371, 477, 349, 463,
                        477, 345, 483, 0, 123, 328, 479, 450, 394, 137, 390, 446, 283, 128, 451,
                        46, 151, 214, 508, 458, 487, 112, 231, 464, 177, 18, 479, 510, 451, 442,
                        388, 457, 468, 302, 42, 472, 181, 181, 257, 451, 498, 179, 349, 365, 164,
                        108, 350, 415, 473, 234, 178, 493, 137, 487, 278, 395, 232, 135, 422, 44,
                        487, 25, 475, 462, 457, 456, 487, 151, 461, 477, 487, 277, 388, 349, 474,
                        261, 341, 479, 456, 133, 472, 342, 18, 21, 520, 242, 175, 241, 322, 415,
                        477, 439, 186, 520, 161, 477, 507, 451, 237, 357, 313, 360, 181, 215, 64,
                        497, 175, 457, 457, 477, 461, 48, 165, 70, 475, 470, 472, 470, 461, 187,
                        79, 444, 393, 345, 111, 457, 483, 235, 439, 390, 111
                },
                {
                        470, 221, 257, 422, 477, 181, 258, 180, 446, 479, 477, 469, 221, 420, 30,
                        457, 353, 520, 341, 166, 510, 236, 483, 477, 462, 502, 166, 68, 305, 24,
                        368, 461, 470, 179, 50, 423, 474, 151, 221, 21, 364, 234, 268, 371, 247,
                        234, 6, 470, 213, 485, 233, 229, 242, 186, 233, 472, 457, 462, 240, 475,
                        30, 358, 485, 0, 221, 61, 439, 139, 184, 45, 261, 422, 221, 510, 221, 236,
                        483, 502, 506, 319, 47, 451, 147, 186, 475, 522, 261, 55, 194, 492, 85,
                        342, 481, 342, 317, 44, 175, 55, 483, 498, 262, 317, 25, 55, 482, 91, 47,
                        298, 224, 445, 361, 252, 109, 123, 472, 492, 15, 408, 482, 125, 271, 499,
                        352, 352, 518, 252, 199, 341, 229, 335, 123, 507, 16, 352, 57, 173, 112,
                        194, 184, 51, 457, 15, 246, 178, 249, 376, 451, 254, 96, 439, 345, 457,
                        229, 91, 234, 315, 330, 25, 457, 50, 451, 359, 50, 7, 172, 41, 517, 151,
                        192, 320, 160, 471, 478, 164, 514, 213, 508, 271, 328, 184, 477, 464, 477,
                        236, 328, 291, 474, 482, 469, 70, 25
                },
                {
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                },
                {
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                },
                {
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 1, 0, 0, 0, 98, 0, 0, 0, 171, 0, 0, 0, 248, 275, 309, 0, 338,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 98, 0, 0, 0, 171,
                        0, 0, 0, 248, 275, 309, 0, 338, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                },
                {
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                },
                {
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                },
                {
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0,
                        0, 98, 0, 98, 0, 171, 0, 0, 248, 275, 0, 309, 0, 0, 0, 0, 0, 0, 0, 0, 309,
                        0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 98, 0, 98, 0, 171, 0, 0, 248, 275, 0,
                        309, 0, 0, 0, 0, 0, 0, 0, 0, 309, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                },
                {
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                },
                {
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                },
                {
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                },
                {
                        209, 459, 468, 268, 502, 178, 470, 388, 418, 439, 223, 517, 457, 458, 15,
                        507, 472, 386, 147, 180, 315, 110, 461, 328, 339, 21, 478, 220, 175, 342,
                        215, 472, 520, 507, 506, 471, 234, 38, 520, 118, 112, 455, 484, 388, 442,
                        471, 462, 173, 329, 482, 474, 416, 334, 266, 412, 249, 484, 69, 483, 395,
                        149, 342, 477, 0, 505, 31, 149, 251, 176, 271, 42, 6, 124, 65, 111, 18, 18,
                        165, 337, 235, 483, 514, 474, 457, 461, 398, 96, 177, 125, 468, 91, 166,
                        211, 459, 459, 297, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0
                },
                {
                        168, 366, 259, 234, 482, 180, 398, 242, 418, 459, 261, 358, 280, 246, 459,
                        339, 186, 474, 518, 242, 413, 350, 119, 224, 7, 159, 81, 54, 122, 483, 339,
                        483, 43, 159, 456, 117, 178, 471, 258, 12, 485, 186, 487, 186, 478, 70,
                        332, 342, 477, 122, 333, 117, 468, 62, 135, 173, 390, 59, 357, 394, 393,
                        477, 522, 0, 237, 18, 505, 179, 177, 175, 229, 140, 459, 509, 472, 466,
                        473, 467, 413, 347, 478, 470, 13, 460, 458, 141, 49, 467, 320, 223, 71,
                        479, 452, 98, 435, 431, 456, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0
                },
                {
                        456, 47, 187, 257, 15, 435, 459, 51, 147, 468, 472, 468, 466, 117, 457,
                        236, 229, 179, 417, 112, 449, 83, 332, 500, 379, 265, 483, 220, 265, 450,
                        483, 432, 51, 320, 47, 98, 43, 17, 242, 352, 84, 320, 342, 517, 347, 107,
                        179, 91, 178, 167, 483, 257, 57, 468, 431, 464, 69, 365, 265, 175, 451,
                        368, 164, 0, 462, 54, 175, 513, 473, 231, 352, 92, 471, 165, 237, 395, 364,
                        417, 474, 452, 456, 505, 179, 479, 249, 423, 237, 229, 222, 432, 342, 67,
                        186, 502, 23, 441, 43, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0
                },
                {
                        215, 513, 61, 477, 339, 180, 493, 350, 6, 231, 258, 478, 162, 451, 456, 79,
                        466, 497, 470, 351, 71, 235, 233, 349, 413, 141, 180, 108, 179, 237, 172,
                        166, 180, 3, 493, 71, 177, 142, 421, 211, 164, 379, 415, 432, 51, 483, 179,
                        242, 329, 399, 524, 221, 457, 518, 468, 368, 455, 121, 225, 91, 229, 507,
                        365, 0, 229, 491, 468, 431, 141, 415, 219, 240, 242, 229, 221, 479, 457,
                        460, 451, 139, 72, 491, 475, 25, 319, 507, 229, 397, 460, 344, 11, 321,
                        109, 70, 113, 0, 447, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0
                },
                {
                        161, 477, 10, 185, 43, 48, 238, 71, 259, 515, 333, 20, 509, 238, 59, 479,
                        339, 459, 241, 81, 313, 513, 235, 456, 70, 453, 479, 472, 432, 147, 43,
                        348, 393, 42, 42, 369, 413, 393, 242, 112, 498, 117, 333, 87, 516, 259, 18,
                        237, 416, 237, 271, 487, 117, 128, 178, 117, 432, 271, 424, 176, 447, 117,
                        278, 0, 271, 271, 172, 432, 121, 18, 68, 507, 244, 317, 477, 162, 483, 483,
                        271, 187, 477, 237, 85, 162, 71, 515, 176, 47, 43, 444, 225, 40, 237, 85,
                        235, 176, 50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                },
                {
                        328, 85, 85, 346, 70, 399, 507, 277, 18, 123, 509, 458, 467, 46, 469, 339,
                        471, 65, 18, 520, 469, 507, 107, 507, 110, 184, 388, 295, 427, 439, 178,
                        483, 166, 421, 48, 257, 180, 461, 441, 252, 461, 414, 337, 97, 398, 477,
                        322, 501, 139, 249, 235, 172, 432, 475, 48, 328, 265, 94, 194, 471, 63,
                        393, 508, 0, 507, 483, 112, 473, 46, 441, 143, 452, 164, 209, 478, 186,
                        457, 139, 477, 55, 225, 308, 83, 501, 393, 63, 477, 520, 412, 379, 84, 241,
                        247, 347, 117, 406, 345, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0
                },
                {
                        237, 223, 459, 110, 421, 478, 151, 459, 139, 451, 299, 483, 451, 481, 225,
                        229, 399, 70, 235, 235, 22, 172, 48, 473, 178, 506, 256, 229, 168, 220,
                        172, 468, 479, 478, 481, 421, 83, 246, 243, 243, 25, 446, 7, 107, 107, 346,
                        172, 493, 254, 314, 59, 236, 268, 172, 322, 124, 98, 147, 18, 50, 341, 3,
                        461, 0, 149, 165, 149, 494, 65, 149, 461, 475, 149, 177, 3, 464, 165, 246,
                        330, 151, 177, 122, 319, 350, 353, 498, 136, 187, 187, 509, 498, 446, 502,
                        91, 339, 479, 15, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4,
                        4, 4, 4, 4, 4, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7,
                        7, 7, 7, 7, 7, 314, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8, 11, 11, 11, 11, 11,
                        11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 12, 12, 12, 12, 12, 12, 12, 12, 12,
                        12, 12, 12, 13, 13, 13, 13, 13
                },
                {
                        502, 151, 151, 25, 449, 483, 108, 117, 350, 72, 242, 500, 229, 179, 467,
                        191, 468, 4, 247, 467, 509, 71, 4, 136, 229, 122, 450, 339, 484, 459, 463,
                        457, 112, 265, 266, 395, 487, 317, 109, 257, 459, 395, 479, 506, 474, 393,
                        168, 68, 505, 213, 467, 393, 257, 268, 510, 505, 395, 85, 291, 518, 44,
                        109, 317, 0, 240, 439, 507, 81, 281, 266, 470, 505, 473, 268, 508, 268,
                        257, 461, 147, 164, 47, 512, 185, 98, 251, 459, 457, 215, 388, 432, 245,
                        449, 228, 395, 349, 234, 506, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13,
                        13, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 16, 16, 16,
                        16, 17, 17, 17, 17, 17, 17, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18,
                        18, 18, 18, 18, 18, 18, 18, 328, 18, 18, 18, 18, 20, 20, 20, 20, 20, 20,
                        20, 20, 20, 20, 20, 20, 21, 21, 21, 21, 22, 22, 22, 22, 23, 23, 23, 23, 23,
                        23, 24, 24, 24, 24, 24, 24, 24
                },
                {
                        229, 220, 524, 185, 328, 167, 242, 494, 509, 483, 167, 249, 458, 464, 166,
                        142, 490, 57, 175, 257, 160, 468, 432, 467, 107, 455, 141, 261, 453, 208,
                        71, 432, 349, 268, 111, 494, 501, 477, 90, 208, 268, 405, 61, 247, 48, 258,
                        141, 164, 405, 457, 337, 393, 233, 45, 459, 475, 469, 456, 451, 175, 475,
                        3, 166, 0, 175, 502, 257, 50, 378, 297, 470, 474, 485, 259, 262, 332, 262,
                        225, 213, 468, 262, 168, 242, 259, 240, 352, 251, 457, 422, 191, 510, 347,
                        483, 406, 517, 186, 393, 24, 24, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25,
                        25, 25, 25, 25, 25, 25, 25, 25, 25, 26, 25, 26, 26, 26, 26, 26, 26, 26, 26,
                        26, 27, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 30, 30, 30, 30, 30, 30,
                        30, 31, 31, 31, 31, 31, 32, 32, 32, 32, 32, 33, 33, 33, 33, 33, 35, 35, 40,
                        40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 41, 41, 41, 42, 42, 42, 42, 42, 42,
                        42, 42, 42, 42, 43, 43
                },
                {
                        160, 479, 509, 177, 497, 485, 7, 87, 339, 518, 456, 503, 340, 342, 70, 186,
                        229, 117, 452, 98, 192, 507, 178, 332, 98, 503, 415, 447, 179, 268, 522,
                        483, 246, 445, 98, 271, 510, 301, 333, 236, 337, 224, 98, 334, 481, 213,
                        199, 352, 510, 213, 98, 340, 242, 451, 3, 478, 472, 333, 223, 159, 348,
                        451, 345, 0, 36, 348, 353, 42, 222, 159, 483, 461, 458, 252, 246, 481, 45,
                        45, 472, 386, 215, 136, 36, 162, 242, 46, 303, 411, 517, 199, 472, 515,
                        206, 47, 339, 520, 348, 43, 43, 43, 501, 43, 43, 43, 43, 43, 43, 43, 44,
                        44, 44, 44, 44, 44, 44, 44, 44, 45, 45, 45, 45, 45, 45, 46, 46, 46, 46, 46,
                        46, 46, 46, 46, 46, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47,
                        47, 48, 48, 48, 395, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 49,
                        49, 49, 49, 49, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 51, 51, 51,
                        51, 51, 51, 51, 51, 51, 51
                },
                {
                        235, 187, 317, 265, 500, 151, 457, 228, 478, 456, 339, 245, 280, 368, 472,
                        87, 445, 479, 194, 451, 406, 505, 92, 458, 71, 431, 280, 432, 339, 96, 112,
                        353, 353, 249, 133, 462, 98, 237, 431, 422, 194, 328, 451, 432, 471, 339,
                        231, 451, 487, 515, 219, 316, 474, 513, 42, 339, 345, 322, 237, 242, 191,
                        55, 46, 0, 478, 225, 330, 339, 510, 65, 520, 58, 245, 172, 388, 223, 497,
                        175, 457, 87, 83, 317, 488, 345, 81, 229, 175, 457, 501, 345, 459, 483,
                        515, 345, 194, 494, 225, 51, 51, 51, 51, 51, 51, 53, 54, 54, 54, 54, 54,
                        54, 54, 55, 55, 514, 55, 55, 55, 56, 56, 56, 56, 56, 57, 57, 57, 57, 57,
                        57, 57, 58, 44, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 59, 61, 61, 61,
                        61, 61, 61, 62, 63, 63, 63, 63, 64, 64, 64, 65, 65, 65, 65, 65, 65, 65, 65,
                        66, 66, 66, 67, 67, 67, 67, 67, 67, 68, 68, 68, 68, 68, 68, 69, 69, 69, 69,
                        69, 69, 69, 69, 69, 69, 69
                },
                {
                        178, 51, 475, 353, 71, 477, 328, 328, 483, 332, 339, 477, 194, 175, 483,
                        368, 319, 59, 25, 473, 249, 463, 213, 225, 225, 507, 229, 246, 108, 353,
                        319, 479, 229, 240, 240, 268, 403, 139, 221, 27, 472, 362, 485, 418, 249,
                        462, 474, 507, 109, 94, 508, 446, 477, 395, 482, 507, 433, 117, 261, 414,
                        257, 41, 247, 0, 483, 456, 510, 141, 458, 507, 124, 124, 404, 179, 393,
                        121, 215, 81, 423, 136, 139, 524, 236, 242, 72, 507, 18, 51, 166, 482, 478,
                        518, 168, 505, 484, 456, 459, 69, 70, 70, 70, 70, 70, 70, 70, 70, 70, 70,
                        70, 70, 70, 70, 70, 71, 71, 71, 71, 71, 72, 72, 72, 72, 72, 72, 72, 72, 72,
                        72, 72, 72, 73, 73, 73, 79, 79, 79, 79, 79, 79, 79, 81, 81, 81, 81, 81, 81,
                        81, 81, 499, 81, 81, 81, 81, 81, 81, 81, 81, 81, 81, 83, 83, 83, 83, 83,
                        83, 83, 83, 83, 83, 83, 83, 83, 83, 83, 83, 84, 84, 84, 84, 84, 84, 84, 84,
                        433, 85, 85, 85, 85, 85, 85, 85
                },
                {
                        473, 507, 477, 257, 408, 81, 15, 505, 481, 172, 124, 422, 408, 249, 418,
                        117, 468, 339, 483, 339, 408, 421, 70, 141, 415, 229, 299, 459, 72, 229,
                        485, 507, 491, 225, 365, 462, 441, 361, 518, 276, 507, 459, 292, 350, 111,
                        254, 487, 507, 180, 507, 483, 209, 11, 328, 291, 229, 482, 328, 25, 236,
                        292, 526, 507, 0, 184, 168, 439, 507, 216, 151, 478, 518, 507, 361, 91,
                        510, 299, 337, 124, 494, 445, 215, 122, 180, 431, 441, 471, 245, 242, 136,
                        526, 516, 12, 339, 507, 215, 228, 87, 87, 87, 87, 87, 87, 87, 87, 87, 88,
                        89, 89, 89, 89, 89, 89, 89, 89, 89, 89, 90, 90, 90, 90, 90, 90, 90, 90, 91,
                        91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 92, 92, 92, 92, 92, 92,
                        94, 94, 94, 94, 96, 96, 96, 96, 96, 444, 96, 96, 96, 97, 97, 97, 97, 97,
                        97, 97, 97, 97, 97, 97, 97, 98, 98, 98, 98, 98, 98, 98, 98, 98, 98, 98, 98,
                        98, 100, 106, 106, 106, 106, 106, 106, 106
                },
                {
                        322, 235, 524, 483, 413, 446, 477, 457, 20, 172, 117, 328, 306, 178, 508,
                        520, 467, 47, 72, 459, 518, 483, 467, 507, 193, 136, 364, 415, 364, 172,
                        192, 388, 261, 507, 172, 242, 413, 172, 479, 452, 350, 217, 477, 165, 346,
                        172, 461, 337, 177, 517, 508, 524, 247, 415, 299, 379, 166, 358, 306, 483,
                        332, 518, 443, 0, 452, 47, 168, 213, 247, 319, 379, 517, 229, 491, 471,
                        483, 393, 180, 474, 223, 474, 13, 24, 447, 510, 319, 84, 456, 447, 55, 474,
                        461, 47, 208, 70, 517, 467, 106, 107, 107, 107, 107, 107, 107, 107, 107,
                        108, 108, 108, 108, 108, 108, 108, 108, 108, 108, 108, 108, 108, 108, 108,
                        108, 108, 109, 109, 109, 109, 109, 109, 109, 109, 109, 109, 109, 110, 110,
                        110, 110, 110, 110, 110, 110, 110, 110, 110, 110, 111, 111, 111, 111, 111,
                        111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 112, 112, 112, 112, 112,
                        112, 112, 112, 112, 112, 112, 112, 112, 112, 112, 115, 116, 117, 117, 117,
                        117, 117, 117, 117, 117, 117, 117, 117, 117, 117
                },
                {
                        413, 139, 517, 455, 447, 475, 349, 474, 474, 433, 44, 432, 84, 241, 233,
                        457, 456, 214, 55, 502, 212, 212, 47, 65, 233, 493, 64, 345, 349, 91, 493,
                        240, 346, 51, 395, 117, 341, 51, 159, 339, 149, 394, 111, 263, 351, 510,
                        236, 285, 24, 8, 408, 159, 505, 84, 477, 399, 180, 351, 333, 229, 513, 35,
                        79, 0, 66, 448, 180, 191, 180, 224, 322, 48, 252, 510, 449, 177, 414, 347,
                        471, 515, 484, 148, 179, 328, 477, 89, 388, 458, 510, 276, 136, 346, 265,
                        13, 388, 320, 236, 117, 117, 117, 117, 117, 117, 117, 117, 117, 117, 117,
                        337, 117, 117, 117, 117, 117, 117, 117, 117, 117, 117, 117, 117, 117, 117,
                        117, 117, 117, 117, 117, 117, 119, 119, 121, 121, 121, 121, 121, 121, 122,
                        122, 122, 122, 122, 122, 122, 122, 122, 122, 122, 123, 123, 123, 123, 123,
                        123, 123, 123, 123, 124, 124, 124, 124, 124, 124, 124, 124, 124, 124, 125,
                        125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 144, 125, 125, 125, 125,
                        125, 126, 127, 127, 128, 128, 128, 128
                },
                {
                        301, 117, 108, 122, 108, 395, 254, 461, 349, 265, 246, 141, 351, 48, 478,
                        474, 18, 216, 177, 487, 366, 172, 148, 508, 220, 71, 33, 117, 441, 229,
                        222, 184, 139, 459, 147, 481, 458, 507, 47, 414, 510, 526, 435, 173, 124,
                        122, 213, 487, 309, 461, 337, 220, 521, 315, 328, 125, 420, 138, 483, 175,
                        502, 161, 506, 0, 498, 509, 242, 235, 354, 117, 498, 136, 341, 187, 515,
                        33, 522, 25, 468, 20, 416, 459, 333, 464, 161, 477, 485, 57, 247, 456, 89,
                        461, 172, 178, 464, 257, 108, 128, 128, 128, 133, 133, 133, 133, 133, 133,
                        133, 133, 133, 133, 133, 133, 133, 133, 133, 135, 135, 135, 135, 135, 135,
                        135, 135, 135, 136, 136, 136, 136, 136, 136, 136, 136, 136, 136, 136, 136,
                        136, 136, 136, 136, 136, 136, 137, 137, 137, 137, 137, 137, 138, 138, 138,
                        139, 139, 139, 139, 139, 139, 139, 139, 139, 139, 139, 140, 140, 140, 141,
                        141, 141, 141, 141, 141, 141, 141, 141, 141, 141, 141, 141, 141, 141, 141,
                        142, 142, 142, 143, 143, 143, 143, 143, 143, 144
                },
                {
                        92, 112, 510, 159, 346, 350, 263, 341, 136, 395, 487, 151, 422, 485, 50,
                        371, 506, 295, 341, 461, 240, 322, 91, 517, 51, 395, 510, 342, 240, 175,
                        26, 166, 18, 510, 61, 472, 491, 523, 328, 330, 483, 513, 499, 387, 143,
                        477, 161, 42, 208, 333, 493, 172, 247, 172, 292, 417, 64, 84, 417, 224,
                        232, 461, 25, 0, 261, 408, 422, 233, 70, 117, 175, 265, 214, 69, 177, 513,
                        223, 461, 242, 395, 491, 339, 315, 339, 328, 122, 184, 242, 242, 472, 71,
                        374, 511, 135, 341, 231, 395, 145, 145, 145, 145, 145, 145, 145, 147, 147,
                        147, 147, 147, 147, 147, 147, 147, 147, 147, 147, 147, 147, 147, 147, 147,
                        147, 147, 16, 148, 148, 149, 149, 149, 149, 149, 149, 149, 149, 149, 151,
                        151, 151, 151, 151, 151, 151, 151, 151, 151, 268, 151, 151, 151, 151, 151,
                        151, 151, 152, 152, 154, 154, 154, 154, 155, 155, 155, 155, 155, 159, 159,
                        159, 159, 159, 159, 159, 159, 159, 160, 160, 160, 160, 160, 160, 160, 161,
                        161, 161, 161, 161, 161, 161, 161, 161, 161, 161
                },
                {
                        221, 214, 483, 485, 149, 505, 420, 431, 295, 423, 477, 339, 427, 513, 509,
                        317, 412, 509, 341, 517, 427, 242, 242, 175, 445, 479, 483, 220, 240, 346,
                        231, 221, 341, 485, 508, 351, 231, 20, 92, 523, 229, 395, 246, 479, 485,
                        517, 483, 108, 393, 503, 393, 307, 151, 291, 66, 501, 341, 499, 328, 11,
                        456, 386, 192, 0, 365, 18, 415, 478, 503, 261, 420, 161, 122, 184, 268,
                        509, 229, 413, 159, 439, 379, 235, 514, 8, 223, 106, 351, 151, 459, 117,
                        229, 485, 242, 184, 339, 8, 501, 161, 161, 161, 161, 161, 161, 161, 162,
                        162, 162, 162, 162, 162, 162, 162, 162, 163, 163, 163, 163, 163, 164, 164,
                        164, 145, 164, 164, 164, 164, 164, 164, 164, 164, 164, 164, 165, 165, 165,
                        165, 165, 165, 165, 165, 165, 165, 165, 165, 165, 165, 166, 166, 166, 166,
                        166, 166, 166, 166, 166, 166, 166, 166, 166, 166, 166, 166, 166, 166, 166,
                        166, 166, 167, 167, 167, 167, 167, 167, 168, 168, 168, 168, 168, 168, 168,
                        168, 168, 168, 172, 172, 172, 172, 172, 172, 172, 172
                },
                {
                        162, 162, 376, 422, 20, 262, 520, 175, 229, 462, 117, 306, 15, 136, 466,
                        173, 514, 422, 376, 111, 176, 268, 376, 376, 306, 457, 232, 211, 25, 164,
                        399, 172, 459, 442, 295, 229, 526, 81, 295, 433, 221, 408, 182, 133, 506,
                        182, 482, 172, 40, 509, 470, 485, 176, 483, 178, 449, 361, 452, 350, 276,
                        518, 440, 298, 0, 116, 178, 399, 516, 328, 505, 386, 159, 507, 172, 111,
                        487, 361, 70, 179, 109, 522, 182, 498, 7, 117, 507, 339, 509, 159, 498,
                        457, 117, 477, 393, 25, 510, 351, 172, 172, 172, 172, 172, 172, 172, 172,
                        172, 172, 172, 172, 172, 172, 172, 172, 172, 172, 172, 172, 172, 172, 172,
                        172, 172, 172, 172, 172, 172, 172, 172, 172, 172, 172, 172, 172, 172, 172,
                        172, 172, 172, 172, 172, 172, 172, 173, 173, 173, 173, 173, 173, 173, 173,
                        173, 173, 173, 173, 173, 173, 173, 173, 173, 175, 175, 175, 175, 175, 175,
                        175, 175, 175, 175, 175, 175, 175, 175, 175, 175, 175, 175, 175, 175, 175,
                        175, 175, 175, 191, 175, 175, 175, 175, 175, 175, 175
                },
                {
                        236, 510, 390, 122, 473, 117, 445, 505, 69, 51, 395, 508, 468, 522, 181,
                        11, 351, 268, 399, 524, 213, 180, 361, 148, 477, 178, 510, 50, 210, 268,
                        186, 213, 161, 59, 164, 128, 423, 462, 209, 461, 352, 121, 246, 470, 322,
                        459, 117, 458, 439, 364, 433, 478, 225, 462, 185, 145, 85, 439, 408, 176,
                        460, 166, 186, 0, 175, 468, 507, 452, 505, 247, 47, 350, 399, 12, 439, 461,
                        449, 459, 128, 466, 431, 466, 462, 159, 457, 117, 435, 415, 94, 215, 117,
                        180, 161, 507, 472, 181, 112, 175, 175, 175, 175, 175, 175, 175, 175, 176,
                        176, 176, 176, 176, 176, 176, 176, 176, 176, 176, 176, 176, 177, 177, 177,
                        177, 177, 177, 177, 177, 177, 177, 177, 177, 177, 177, 474, 177, 177, 177,
                        177, 177, 177, 177, 177, 177, 177, 177, 177, 177, 178, 178, 178, 178, 178,
                        178, 178, 178, 178, 184, 178, 178, 178, 178, 178, 178, 178, 178, 178, 172,
                        178, 178, 178, 178, 178, 178, 178, 179, 179, 179, 179, 179, 179, 179, 179,
                        179, 179, 179, 179, 179, 179, 179, 179, 179, 179
                },
                {
                        172, 467, 361, 520, 393, 97, 234, 247, 232, 423, 352, 390, 339, 516, 339,
                        449, 506, 459, 398, 477, 423, 449, 123, 450, 17, 515, 28, 143, 522, 245,
                        237, 477, 500, 18, 58, 236, 262, 339, 346, 432, 520, 167, 521, 457, 518,
                        464, 232, 179, 110, 369, 265, 483, 520, 108, 247, 467, 479, 389, 339, 467,
                        460, 175, 194, 0, 459, 368, 262, 339, 92, 508, 81, 265, 263, 484, 475, 13,
                        408, 350, 20, 164, 128, 520, 262, 451, 117, 451, 467, 135, 263, 462, 231,
                        520, 329, 487, 478, 431, 137, 179, 179, 180, 180, 180, 180, 180, 180, 180,
                        180, 180, 180, 180, 180, 180, 180, 180, 180, 180, 180, 180, 180, 180, 180,
                        180, 180, 180, 181, 181, 182, 182, 182, 182, 182, 182, 182, 182, 182, 182,
                        182, 182, 182, 182, 182, 182, 182, 184, 184, 184, 184, 184, 184, 184, 184,
                        184, 184, 184, 184, 184, 184, 184, 184, 184, 184, 184, 184, 184, 184, 184,
                        184, 184, 185, 185, 185, 185, 185, 185, 185, 186, 186, 186, 186, 186, 186,
                        177, 186, 186, 186, 187, 187, 187, 187, 187, 187
                },
                {
                        507, 487, 47, 42, 69, 458, 484, 520, 467, 479, 451, 128, 385, 479, 179,
                        477, 515, 291, 12, 161, 317, 509, 175, 417, 352, 403, 487, 458, 415, 457,
                        364, 423, 117, 487, 505, 124, 366, 161, 497, 427, 468, 413, 505, 520, 423,
                        165, 28, 18, 112, 63, 229, 417, 478, 457, 520, 225, 513, 341, 251, 507,
                        247, 271, 330, 0, 231, 468, 520, 172, 461, 415, 247, 17, 477, 493, 271,
                        342, 167, 459, 172, 386, 466, 357, 468, 415, 345, 497, 526, 507, 388, 376,
                        235, 483, 108, 359, 58, 525, 175, 187, 187, 187, 187, 187, 188, 188, 188,
                        125, 189, 189, 189, 189, 189, 191, 191, 191, 191, 191, 191, 192, 192, 192,
                        192, 192, 192, 192, 193, 193, 193, 193, 194, 194, 194, 194, 194, 194, 194,
                        194, 194, 194, 194, 194, 194, 194, 194, 198, 198, 198, 198, 199, 199, 206,
                        206, 206, 206, 208, 208, 208, 208, 209, 209, 209, 209, 209, 209, 209, 210,
                        210, 210, 210, 210, 211, 211, 211, 211, 212, 212, 213, 213, 213, 213, 213,
                        213, 213, 213, 214, 214, 214, 214, 214, 214, 214, 214
                },
        };
    }

    private static short[][] initCode3() {
        return new short[][] {
                {
                        359, 42, 369, 466, 166, 162, 523, 457, 342, 487, 68, 479, 166, 457, 379,
                        175, 176, 164, 493, 61, 462, 517, 18, 421, 477, 299, 415, 477, 387, 467,
                        172, 23, 341, 221, 337, 470, 339, 322, 474, 268, 225, 462, 523, 213, 482,
                        467, 225, 459, 42, 177, 242, 42, 479, 388, 460, 459, 524, 523, 246, 457,
                        221, 225, 231, 0, 159, 361, 510, 505, 85, 457, 479, 423, 487, 459, 415,
                        462, 116, 116, 339, 25, 333, 460, 502, 123, 479, 139, 525, 421, 31, 339,
                        453, 479, 225, 421, 242, 408, 123, 214, 214, 214, 215, 215, 215, 215, 216,
                        216, 216, 216, 219, 219, 219, 219, 219, 219, 219, 220, 220, 220, 221, 221,
                        221, 221, 221, 221, 221, 221, 221, 221, 221, 221, 221, 221, 221, 222, 222,
                        222, 222, 222, 222, 222, 223, 223, 223, 223, 223, 223, 223, 223, 223, 224,
                        224, 225, 225, 225, 225, 225, 225, 225, 225, 225, 225, 225, 228, 228, 228,
                        229, 229, 229, 229, 229, 229, 229, 229, 229, 229, 229, 229, 229, 229, 229,
                        229, 229, 229, 229, 229, 229, 229, 229, 229, 229, 229
                },
                {
                        355, 393, 136, 510, 184, 259, 265, 137, 431, 185, 117, 393, 138, 517, 483,
                        4, 107, 408, 249, 237, 328, 107, 44, 451, 18, 172, 49, 237, 185, 261, 502,
                        246, 328, 172, 172, 243, 261, 68, 473, 482, 482, 111, 7, 473, 136, 482,
                        492, 257, 477, 510, 477, 364, 354, 342, 164, 477, 477, 473, 342, 459, 483,
                        222, 484, 0, 108, 388, 111, 388, 231, 300, 342, 159, 49, 111, 159, 48, 65,
                        117, 458, 321, 219, 334, 242, 507, 483, 457, 462, 457, 194, 166, 166, 461,
                        386, 159, 176, 509, 386, 229, 229, 229, 229, 229, 229, 229, 229, 232, 231,
                        231, 231, 231, 231, 231, 231, 231, 231, 231, 231, 231, 231, 231, 232, 232,
                        232, 232, 232, 232, 232, 232, 232, 232, 232, 233, 233, 233, 233, 233, 233,
                        233, 233, 224, 233, 233, 233, 233, 234, 234, 234, 234, 234, 235, 235, 235,
                        235, 235, 235, 235, 235, 235, 235, 235, 235, 236, 236, 236, 236, 236, 236,
                        236, 236, 236, 236, 236, 236, 236, 236, 237, 237, 237, 237, 237, 237, 237,
                        237, 237, 237, 237, 240, 240, 240, 240, 240
                },
                {
                        48, 468, 48, 520, 449, 166, 160, 151, 330, 231, 351, 6, 235, 321, 345, 6,
                        166, 468, 72, 446, 135, 135, 135, 85, 368, 277, 513, 435, 328, 477, 351,
                        173, 184, 51, 245, 178, 498, 241, 172, 223, 168, 482, 163, 477, 347, 487,
                        70, 159, 507, 446, 505, 45, 506, 482, 517, 433, 307, 471, 503, 381, 149,
                        394, 234, 0, 180, 18, 81, 143, 452, 467, 333, 61, 87, 446, 435, 184, 61,
                        214, 231, 231, 453, 214, 231, 231, 61, 394, 411, 435, 214, 503, 507, 70,
                        297, 346, 291, 435, 435, 240, 240, 240, 240, 241, 241, 241, 241, 241, 241,
                        242, 242, 242, 242, 242, 242, 242, 242, 242, 242, 242, 242, 242, 242, 242,
                        242, 242, 242, 242, 242, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247,
                        247, 247, 247, 247, 243, 243, 243, 243, 243, 243, 244, 244, 245, 245, 245,
                        245, 245, 245, 245, 246, 246, 246, 246, 246, 246, 246, 246, 246, 246, 246,
                        246, 249, 249, 249, 249, 249, 249, 249, 249, 249, 250, 250, 250, 250, 250,
                        250, 251, 251, 251, 251, 251, 251, 251, 251
                },
                {
                        240, 502, 408, 413, 502, 477, 350, 198, 32, 339, 81, 164, 48, 361, 366,
                        484, 40, 347, 111, 320, 318, 482, 357, 452, 339, 328, 457, 457, 198, 85,
                        110, 7, 25, 467, 432, 518, 209, 507, 291, 333, 518, 509, 459, 271, 351,
                        194, 48, 473, 318, 507, 465, 106, 155, 518, 214, 506, 433, 65, 257, 462,
                        462, 250, 462, 0, 296, 452, 452, 466, 199, 482, 155, 319, 388, 435, 257,
                        57, 393, 351, 425, 524, 172, 482, 463, 445, 47, 443, 90, 280, 328, 136,
                        229, 501, 415, 178, 232, 404, 21, 251, 252, 252, 252, 252, 252, 252, 254,
                        254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 256, 257, 257, 257,
                        257, 257, 257, 257, 257, 257, 257, 257, 257, 257, 257, 257, 257, 258, 258,
                        258, 259, 259, 259, 259, 259, 259, 259, 259, 261, 261, 261, 261, 261, 261,
                        261, 261, 261, 261, 261, 261, 261, 261, 262, 262, 262, 262, 262, 262, 262,
                        262, 262, 263, 263, 263, 263, 263, 263, 263, 263, 264, 264, 265, 265, 265,
                        265, 265, 265, 266, 266, 266, 266, 266, 266, 267, 268
                },
                {
                        245, 329, 225, 352, 160, 70, 282, 277, 361, 393, 515, 184, 89, 328, 168,
                        455, 257, 368, 513, 48, 246, 313, 81, 4, 280, 402, 487, 508, 365, 441, 451,
                        186, 173, 92, 18, 43, 472, 447, 398, 422, 413, 515, 477, 233, 172, 328,
                        462, 247, 313, 43, 242, 168, 318, 53, 176, 242, 513, 242, 177, 479, 247,
                        469, 34, 0, 439, 291, 233, 65, 214, 461, 444, 317, 507, 177, 456, 65, 476,
                        460, 415, 111, 511, 70, 211, 299, 186, 51, 185, 219, 231, 444, 339, 65, 23,
                        470, 366, 485, 492, 268, 268, 268, 268, 268, 268, 268, 268, 268, 268, 268,
                        268, 268, 268, 268, 268, 270, 270, 270, 271, 271, 271, 271, 271, 271, 271,
                        271, 271, 271, 271, 271, 271, 271, 271, 276, 276, 276, 276, 276, 276, 276,
                        277, 277, 277, 277, 277, 278, 278, 278, 279, 280, 280, 280, 280, 280, 281,
                        282, 282, 284, 285, 291, 291, 291, 291, 291, 291, 291, 291, 291, 291, 291,
                        292, 292, 292, 292, 292, 292, 292, 293, 293, 294, 294, 295, 295, 295, 295,
                        295, 295, 295, 296, 297, 297, 297, 297
                },
                {
                        459, 21, 464, 215, 234, 472, 242, 168, 488, 246, 351, 492, 243, 291, 491,
                        459, 455, 181, 235, 181, 177, 172, 149, 466, 262, 295, 125, 175, 507, 507,
                        466, 420, 459, 40, 457, 483, 464, 184, 182, 463, 391, 182, 432, 395, 421,
                        337, 337, 139, 339, 432, 458, 72, 108, 314, 420, 108, 108, 242, 236, 458,
                        349, 318, 210, 0, 117, 493, 112, 229, 483, 222, 446, 25, 341, 184, 165,
                        520, 20, 271, 85, 90, 12, 40, 477, 31, 241, 69, 469, 474, 79, 71, 342, 242,
                        477, 172, 175, 455, 339, 297, 297, 298, 298, 298, 298, 299, 299, 299, 299,
                        301, 301, 301, 307, 302, 303, 303, 306, 306, 306, 306, 309, 313, 313, 313,
                        313, 313, 313, 313, 314, 314, 314, 314, 314, 314, 315, 315, 315, 315, 315,
                        315, 317, 317, 317, 317, 317, 317, 317, 317, 318, 318, 318, 318, 318, 319,
                        319, 319, 319, 319, 319, 319, 320, 320, 320, 320, 320, 320, 320, 320, 320,
                        321, 321, 322, 322, 322, 322, 322, 322, 322, 322, 322, 322, 322, 322, 322,
                        322, 328, 328, 328, 328, 328, 328, 328, 328
                },
                {
                        242, 242, 42, 403, 175, 333, 472, 472, 32, 224, 435, 433, 177, 322, 477,
                        40, 262, 122, 483, 458, 518, 166, 415, 507, 444, 451, 339, 452, 361, 117,
                        510, 162, 186, 172, 6, 521, 254, 362, 148, 478, 482, 477, 346, 328, 431,
                        81, 229, 481, 15, 477, 510, 291, 314, 24, 466, 474, 459, 159, 526, 89, 85,
                        295, 161, 0, 257, 394, 137, 261, 451, 117, 518, 507, 172, 61, 446, 469,
                        246, 229, 364, 507, 46, 483, 466, 518, 234, 456, 172, 141, 59, 135, 140,
                        294, 483, 518, 320, 271, 508, 328, 328, 328, 328, 328, 328, 328, 328, 328,
                        329, 329, 329, 329, 330, 330, 330, 330, 331, 331, 332, 332, 332, 332, 332,
                        333, 333, 333, 333, 333, 333, 333, 333, 333, 334, 334, 334, 334, 334, 334,
                        334, 334, 335, 337, 337, 337, 337, 337, 337, 337, 337, 337, 337, 337, 337,
                        337, 337, 13, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339,
                        339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339, 339,
                        339, 339, 339, 339, 339, 339, 339, 339, 339, 340
                },
                {
                        341, 510, 164, 117, 121, 418, 54, 180, 106, 4, 345, 48, 333, 341, 223, 399,
                        514, 68, 33, 483, 91, 451, 229, 90, 117, 361, 478, 337, 487, 415, 47, 299,
                        456, 22, 457, 128, 510, 268, 514, 526, 445, 350, 417, 46, 322, 457, 464,
                        479, 45, 187, 472, 67, 147, 67, 173, 450, 482, 365, 461, 459, 452, 178,
                        278, 0, 271, 441, 286, 252, 59, 81, 351, 89, 521, 219, 242, 451, 361, 215,
                        337, 518, 124, 143, 245, 50, 56, 500, 258, 229, 13, 347, 185, 347, 81, 386,
                        179, 502, 507, 340, 340, 341, 341, 341, 341, 341, 341, 341, 341, 341, 341,
                        341, 341, 341, 341, 341, 341, 341, 341, 341, 341, 341, 341, 342, 342, 342,
                        342, 342, 342, 342, 342, 345, 345, 345, 345, 345, 345, 345, 345, 345, 345,
                        345, 345, 345, 345, 345, 346, 346, 346, 346, 346, 347, 347, 347, 347, 347,
                        347, 347, 347, 347, 347, 347, 348, 348, 348, 348, 348, 348, 348, 348, 348,
                        348, 348, 348, 348, 349, 349, 350, 350, 350, 350, 350, 350, 350, 350, 351,
                        351, 351, 351, 351, 351, 351, 351
                },
                {
                        128, 210, 347, 472, 450, 17, 509, 175, 235, 421, 432, 72, 161, 151, 57, 43,
                        164, 117, 220, 386, 472, 477, 433, 339, 449, 33, 505, 494, 182, 18, 477,
                        259, 14, 319, 87, 350, 194, 460, 449, 483, 483, 117, 231, 468, 278, 33,
                        455, 57, 461, 329, 271, 4, 479, 216, 216, 176, 262, 526, 526, 522, 365,
                        457, 475, 0, 472, 351, 175, 117, 247, 175, 321, 159, 159, 160, 472, 441,
                        518, 348, 261, 165, 393, 121, 509, 341, 451, 25, 451, 172, 92, 474, 417,
                        352, 505, 470, 395, 221, 520, 351, 351, 351, 351, 351, 352, 352, 352, 352,
                        352, 352, 352, 352, 352, 352, 352, 353, 353, 353, 353, 353, 353, 353, 353,
                        354, 354, 357, 357, 357, 357, 358, 358, 358, 358, 358, 359, 359, 359, 360,
                        360, 361, 361, 361, 361, 361, 361, 361, 361, 361, 361, 362, 362, 363, 364,
                        364, 364, 364, 364, 364, 364, 364, 364, 364, 365, 365, 365, 366, 366, 366,
                        366, 366, 366, 366, 366, 366, 366, 368, 368, 369, 369, 369, 370, 370, 371,
                        371, 372, 372, 372, 374, 374, 374, 374, 376, 376
                },
                {
                        474, 484, 257, 487, 399, 513, 139, 357, 469, 446, 451, 482, 412, 478, 395,
                        57, 395, 487, 505, 366, 229, 353, 484, 229, 184, 457, 51, 467, 441, 237,
                        455, 83, 341, 522, 334, 67, 484, 51, 483, 317, 337, 276, 457, 111, 487,
                        371, 31, 261, 416, 266, 412, 237, 457, 136, 222, 125, 67, 422, 246, 468,
                        517, 141, 520, 0, 142, 526, 433, 33, 320, 446, 393, 222, 236, 433, 466,
                        254, 439, 510, 4, 231, 520, 333, 467, 179, 442, 178, 451, 443, 32, 483,
                        477, 518, 18, 242, 26, 501, 225, 376, 376, 377, 377, 377, 378, 378, 378,
                        378, 379, 379, 379, 382, 383, 386, 386, 386, 40, 386, 386, 386, 386, 386,
                        387, 387, 388, 388, 388, 388, 388, 388, 388, 388, 388, 388, 388, 388, 388,
                        388, 388, 388, 389, 389, 389, 389, 389, 389, 389, 389, 390, 390, 390, 390,
                        390, 390, 390, 390, 390, 390, 390, 391, 391, 391, 391, 391, 391, 391, 391,
                        391, 391, 391, 391, 393, 393, 393, 393, 393, 393, 393, 393, 393, 393, 393,
                        393, 393, 393, 393, 393, 394, 394, 394, 394, 394, 394
                },
                {
                        342, 251, 472, 236, 457, 330, 142, 147, 81, 242, 391, 389, 81, 470, 25, 81,
                        498, 393, 468, 161, 6, 261, 247, 508, 334, 176, 261, 61, 294, 166, 187,
                        478, 388, 478, 143, 46, 161, 386, 208, 341, 249, 492, 494, 229, 451, 172,
                        470, 394, 259, 313, 42, 83, 369, 225, 483, 345, 510, 210, 175, 250, 487,
                        13, 482, 0, 242, 474, 98, 431, 110, 186, 110, 366, 111, 214, 405, 471, 467,
                        117, 422, 456, 89, 408, 461, 457, 372, 487, 390, 175, 416, 236, 483, 458,
                        408, 299, 468, 487, 483, 394, 394, 394, 394, 394, 395, 395, 395, 395, 395,
                        395, 395, 395, 395, 395, 395, 395, 393, 395, 395, 395, 395, 395, 395, 395,
                        395, 395, 395, 395, 395, 395, 395, 395, 395, 395, 395, 395, 395, 395, 395,
                        395, 395, 395, 395, 395, 395, 395, 395, 395, 395, 395, 395, 398, 398, 398,
                        398, 398, 398, 398, 398, 398, 398, 399, 399, 399, 399, 399, 399, 399, 399,
                        399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399, 399,
                        399, 399, 399, 399, 399, 399, 399, 399, 399
                },
                {
                        461, 149, 3, 166, 166, 172, 518, 460, 449, 264, 228, 176, 30, 393, 379,
                        231, 194, 484, 68, 507, 422, 500, 416, 472, 108, 87, 462, 136, 510, 175,
                        193, 372, 470, 474, 8, 94, 332, 484, 297, 509, 455, 330, 339, 493, 46, 505,
                        106, 291, 479, 61, 461, 339, 107, 175, 483, 214, 20, 517, 261, 221, 179,
                        349, 346, 0, 446, 459, 413, 247, 477, 467, 462, 477, 219, 225, 177, 81,
                        507, 15, 474, 268, 164, 319, 412, 421, 443, 349, 345, 451, 237, 166, 313,
                        193, 487, 13, 229, 510, 510, 399, 400, 400, 401, 401, 401, 401, 402, 402,
                        403, 403, 403, 404, 404, 404, 404, 405, 405, 405, 405, 406, 406, 406, 406,
                        408, 408, 408, 408, 408, 408, 408, 408, 408, 408, 408, 408, 408, 395, 408,
                        408, 411, 411, 411, 411, 411, 411, 411, 411, 412, 412, 412, 412, 413, 413,
                        413, 413, 413, 413, 413, 413, 413, 466, 413, 413, 414, 414, 414, 415, 415,
                        415, 415, 415, 415, 415, 415, 415, 415, 415, 416, 416, 416, 417, 417, 417,
                        417, 417, 417, 417, 417, 418, 418, 418, 418, 418
                },
                {
                        3, 235, 468, 347, 220, 494, 456, 369, 369, 347, 242, 413, 443, 252, 487,
                        333, 483, 470, 172, 181, 468, 350, 413, 181, 112, 25, 460, 477, 459, 483,
                        184, 459, 459, 478, 342, 479, 240, 440, 162, 485, 236, 474, 257, 221, 214,
                        221, 172, 422, 251, 225, 225, 166, 411, 507, 451, 214, 163, 229, 172, 225,
                        163, 246, 172, 0, 214, 242, 175, 375, 427, 225, 352, 461, 477, 243, 258,
                        22, 507, 461, 184, 161, 467, 67, 117, 467, 242, 161, 149, 177, 184, 13,
                        472, 500, 500, 214, 23, 457, 399, 418, 418, 418, 418, 420, 420, 420, 420,
                        420, 420, 420, 420, 420, 421, 421, 421, 421, 421, 421, 421, 421, 421, 421,
                        421, 421, 421, 421, 421, 421, 421, 421, 422, 422, 422, 422, 422, 422, 422,
                        422, 422, 422, 422, 422, 422, 423, 423, 423, 423, 423, 423, 423, 423, 423,
                        423, 423, 425, 427, 427, 427, 427, 431, 431, 431, 431, 431, 431, 431, 431,
                        431, 431, 431, 431, 431, 431, 431, 432, 432, 432, 432, 432, 432, 432, 432,
                        433, 433, 433, 433, 433, 434, 434, 434, 435, 435, 435
                },
                {
                        84, 350, 87, 446, 186, 503, 475, 483, 147, 518, 460, 109, 98, 7, 48, 341,
                        452, 485, 485, 187, 339, 439, 507, 484, 353, 482, 347, 339, 508, 271, 450,
                        111, 111, 148, 117, 357, 319, 291, 425, 333, 81, 184, 229, 117, 498, 467,
                        328, 328, 459, 84, 22, 24, 500, 434, 136, 479, 246, 59, 166, 252, 117, 234,
                        477, 0, 459, 229, 477, 333, 346, 391, 477, 450, 268, 346, 141, 349, 223,
                        464, 178, 350, 173, 485, 45, 15, 147, 468, 505, 337, 459, 232, 18, 484,
                        482, 178, 70, 83, 166, 435, 435, 435, 435, 435, 435, 435, 439, 439, 439,
                        439, 439, 439, 439, 439, 439, 439, 439, 439, 439, 440, 440, 440, 440, 441,
                        441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 442, 442, 443, 443, 443,
                        443, 443, 443, 444, 444, 444, 445, 445, 445, 445, 445, 445, 445, 445, 445,
                        445, 445, 447, 447, 447, 447, 447, 447, 447, 448, 448, 449, 449, 449, 449,
                        449, 449, 449, 449, 449, 449, 449, 449, 449, 449, 449, 449, 449, 450, 450,
                        450, 450, 450, 450, 450, 450, 450, 450, 451
                },
                {
                        411, 184, 339, 483, 187, 457, 245, 229, 85, 423, 215, 147, 147, 12, 110,
                        451, 444, 487, 417, 341, 451, 291, 451, 232, 89, 98, 11, 517, 450, 30, 261,
                        446, 219, 178, 435, 254, 467, 262, 178, 395, 468, 472, 365, 451, 117, 484,
                        257, 451, 368, 462, 458, 479, 395, 508, 422, 510, 520, 431, 484, 259, 219,
                        91, 350, 0, 457, 455, 487, 483, 479, 176, 11, 408, 59, 457, 453, 231, 317,
                        364, 172, 456, 466, 147, 477, 328, 162, 477, 91, 285, 458, 161, 166, 249,
                        477, 452, 479, 427, 508, 451, 451, 451, 451, 451, 451, 451, 451, 451, 451,
                        451, 451, 451, 451, 451, 451, 451, 451, 451, 451, 451, 451, 451, 451, 451,
                        451, 451, 451, 451, 451, 451, 451, 452, 452, 452, 452, 452, 452, 452, 452,
                        452, 452, 453, 453, 453, 455, 455, 455, 455, 455, 455, 455, 455, 455, 456,
                        456, 456, 456, 456, 456, 456, 456, 456, 456, 456, 456, 456, 456, 456, 456,
                        456, 456, 456, 456, 456, 456, 456, 456, 456, 456, 456, 456, 456, 457, 457,
                        457, 457, 457, 457, 457, 457, 457, 457, 457
                },
                {
                        31, 409, 339, 433, 389, 81, 242, 451, 507, 46, 351, 328, 483, 175, 241,
                        347, 478, 176, 452, 461, 251, 503, 503, 249, 483, 237, 61, 229, 251, 461,
                        43, 268, 524, 408, 350, 425, 507, 322, 345, 351, 22, 233, 141, 457, 339,
                        513, 110, 233, 186, 186, 478, 431, 177, 359, 461, 456, 508, 470, 408, 51,
                        47, 71, 229, 0, 445, 477, 180, 68, 339, 172, 460, 391, 347, 479, 41, 229,
                        495, 468, 510, 494, 462, 252, 462, 339, 364, 175, 149, 517, 178, 23, 151,
                        108, 225, 178, 219, 265, 229, 457, 457, 457, 457, 457, 457, 457, 457, 457,
                        457, 457, 457, 457, 457, 457, 457, 457, 457, 459, 457, 457, 457, 457, 457,
                        458, 458, 458, 458, 458, 458, 458, 458, 458, 458, 386, 458, 458, 459, 459,
                        459, 459, 459, 459, 459, 459, 459, 459, 459, 459, 459, 459, 459, 459, 459,
                        459, 459, 459, 459, 459, 459, 459, 459, 459, 460, 460, 460, 460, 460, 460,
                        460, 460, 460, 460, 460, 460, 460, 460, 460, 460, 460, 460, 460, 460, 461,
                        461, 461, 469, 461, 461, 461, 461, 461, 461, 461
                },
                {
                        350, 295, 242, 91, 461, 510, 240, 229, 240, 318, 475, 328, 389, 475, 479,
                        399, 457, 30, 351, 352, 30, 251, 178, 510, 517, 165, 307, 320, 508, 250,
                        106, 194, 264, 457, 191, 484, 351, 236, 468, 399, 439, 460, 483, 161, 451,
                        72, 49, 451, 72, 516, 122, 483, 477, 117, 321, 178, 508, 70, 477, 508, 178,
                        507, 462, 0, 357, 507, 187, 41, 477, 357, 445, 236, 319, 474, 526, 18, 390,
                        184, 210, 469, 505, 477, 314, 117, 81, 117, 142, 507, 507, 357, 477, 445,
                        276, 135, 468, 503, 351, 461, 461, 461, 461, 461, 461, 461, 462, 462, 462,
                        462, 462, 462, 173, 462, 462, 462, 462, 462, 462, 462, 462, 462, 462, 462,
                        462, 462, 462, 463, 463, 463, 463, 463, 463, 463, 463, 463, 463, 464, 464,
                        464, 464, 464, 464, 464, 464, 464, 148, 464, 464, 464, 464, 464, 465, 465,
                        465, 465, 465, 465, 465, 466, 466, 466, 466, 466, 466, 466, 466, 466, 467,
                        467, 467, 467, 467, 467, 467, 467, 467, 467, 467, 467, 467, 51, 467, 467,
                        467, 467, 467, 468, 468, 468, 468, 468, 468
                },
                {
                        483, 291, 25, 446, 306, 339, 209, 361, 176, 525, 268, 178, 106, 366, 510,
                        141, 478, 188, 464, 380, 215, 294, 399, 462, 215, 229, 185, 393, 178, 505,
                        237, 172, 477, 26, 514, 404, 354, 229, 466, 421, 215, 423, 484, 236, 48,
                        43, 72, 232, 320, 110, 484, 472, 91, 507, 477, 339, 143, 198, 339, 431,
                        431, 117, 508, 0, 462, 85, 215, 442, 482, 482, 484, 117, 483, 442, 472,
                        477, 319, 70, 487, 418, 135, 163, 364, 484, 277, 181, 417, 317, 377, 294,
                        479, 178, 163, 209, 231, 229, 395, 468, 468, 468, 468, 469, 469, 469, 469,
                        469, 469, 470, 470, 470, 470, 470, 470, 470, 470, 470, 470, 470, 470, 470,
                        470, 471, 471, 471, 471, 471, 471, 471, 471, 471, 471, 471, 471, 471, 471,
                        471, 471, 472, 472, 472, 472, 472, 472, 472, 472, 472, 472, 472, 472, 472,
                        472, 472, 472, 472, 472, 472, 472, 472, 472, 472, 472, 472, 472, 472, 472,
                        472, 472, 472, 472, 472, 473, 473, 473, 473, 473, 473, 473, 473, 473, 473,
                        473, 473, 473, 473, 473, 473, 473, 474, 474, 474, 474
                },
                {
                        247, 477, 85, 462, 459, 451, 21, 32, 172, 388, 335, 18, 337, 175, 513, 175,
                        67, 172, 70, 488, 108, 110, 460, 470, 22, 359, 251, 221, 6, 494, 166, 32,
                        415, 299, 70, 231, 71, 399, 500, 18, 221, 337, 507, 419, 399, 447, 395, 15,
                        462, 25, 46, 220, 240, 457, 459, 221, 503, 69, 184, 491, 395, 175, 477, 0,
                        221, 471, 457, 112, 117, 114, 7, 151, 172, 172, 459, 139, 20, 472, 467,
                        186, 329, 254, 261, 261, 331, 395, 408, 172, 505, 186, 261, 433, 231, 474,
                        507, 187, 457, 474, 474, 474, 474, 474, 474, 474, 474, 474, 474, 474, 475,
                        475, 475, 475, 475, 475, 475, 475, 475, 475, 475, 475, 475, 475, 475, 477,
                        477, 477, 477, 477, 477, 477, 477, 477, 477, 477, 477, 477, 477, 477, 477,
                        477, 477, 477, 477, 477, 477, 477, 477, 477, 477, 477, 477, 477, 477, 477,
                        477, 477, 477, 477, 477, 477, 477, 477, 477, 477, 477, 477, 477, 477, 477,
                        477, 477, 477, 477, 477, 477, 477, 478, 478, 478, 478, 478, 478, 478, 478,
                        478, 478, 478, 478, 478, 478, 478
                },
                {
                        388, 451, 457, 432, 483, 221, 98, 91, 347, 318, 172, 266, 479, 135, 351,
                        500, 179, 139, 79, 175, 246, 351, 175, 451, 186, 351, 246, 221, 393, 172,
                        139, 472, 432, 350, 179, 63, 507, 44, 172, 70, 507, 462, 352, 125, 395,
                        141, 462, 167, 350, 464, 291, 339, 242, 498, 18, 464, 389, 133, 507, 469,
                        51, 457, 477, 0, 242, 186, 457, 472, 457, 472, 87, 117, 350, 350, 177, 172,
                        108, 470, 84, 159, 41, 423, 467, 178, 477, 361, 470, 478, 388, 339, 445,
                        172, 470, 478, 98, 111, 471, 478, 479, 479, 479, 479, 479, 479, 479, 479,
                        479, 479, 479, 479, 479, 479, 479, 479, 479, 479, 480, 481, 481, 481, 481,
                        481, 481, 481, 481, 481, 481, 481, 481, 481, 481, 481, 482, 482, 482, 482,
                        482, 482, 482, 482, 482, 482, 482, 482, 482, 482, 482, 482, 482, 482, 482,
                        482, 483, 483, 483, 483, 483, 483, 483, 483, 483, 483, 483, 483, 483, 483,
                        483, 483, 483, 483, 483, 483, 483, 483, 483, 483, 483, 483, 483, 483, 483,
                        467, 483, 483, 483, 483, 483, 483, 483, 483, 483
                },
                {
                        474, 411, 393, 478, 478, 186, 461, 281, 46, 482, 507, 465, 109, 463, 263,
                        391, 472, 372, 516, 467, 477, 477, 413, 48, 151, 393, 151, 467, 505, 510,
                        506, 135, 518, 500, 136, 117, 175, 85, 236, 431, 473, 301, 317, 509, 122,
                        477, 184, 474, 498, 477, 477, 351, 502, 333, 18, 465, 351, 25, 68, 522,
                        423, 510, 59, 0, 503, 481, 467, 470, 477, 165, 151, 395, 346, 461, 395,
                        154, 91, 160, 141, 352, 166, 178, 162, 121, 473, 451, 393, 509, 439, 261,
                        266, 246, 166, 472, 465, 137, 106, 483, 483, 483, 483, 483, 483, 484, 484,
                        484, 484, 484, 484, 484, 484, 484, 484, 484, 484, 484, 484, 484, 484, 484,
                        484, 484, 484, 485, 485, 485, 485, 474, 485, 485, 485, 485, 485, 487, 487,
                        487, 487, 487, 487, 487, 487, 487, 487, 487, 487, 488, 488, 488, 490, 490,
                        490, 490, 490, 490, 490, 491, 491, 491, 491, 492, 492, 492, 493, 493, 493,
                        493, 493, 493, 493, 493, 493, 493, 493, 493, 493, 493, 494, 494, 494, 494,
                        495, 496, 497, 497, 35, 497, 498, 498, 498, 498, 498
                },
                {
                        24, 433, 477, 225, 510, 213, 351, 456, 172, 507, 361, 63, 222, 98, 213,
                        477, 435, 70, 15, 42, 482, 199, 345, 347, 400, 4, 483, 461, 47, 178, 459,
                        456, 456, 124, 411, 26, 166, 180, 485, 505, 485, 91, 162, 43, 404, 178,
                        194, 351, 61, 461, 415, 450, 459, 110, 220, 418, 477, 291, 478, 509, 328,
                        517, 42, 0, 46, 516, 172, 339, 421, 515, 451, 184, 348, 89, 506, 526, 521,
                        341, 517, 232, 175, 172, 458, 245, 393, 21, 162, 329, 483, 462, 467, 329,
                        395, 468, 395, 167, 162, 498, 498, 498, 388, 498, 488, 498, 498, 498, 499,
                        499, 499, 499, 499, 499, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500,
                        500, 500, 500, 500, 500, 500, 500, 501, 501, 501, 501, 501, 501, 501, 501,
                        501, 501, 501, 501, 501, 501, 501, 502, 502, 502, 502, 502, 502, 502, 502,
                        502, 502, 503, 503, 503, 503, 503, 503, 503, 503, 503, 503, 505, 505, 505,
                        505, 505, 505, 505, 505, 505, 505, 505, 505, 505, 505, 505, 505, 506, 506,
                        506, 506, 506, 506, 506, 506, 506, 506, 506
                },
                {
                        98, 508, 431, 462, 117, 337, 435, 221, 339, 483, 518, 513, 457, 166, 478,
                        440, 459, 278, 46, 112, 510, 473, 472, 165, 468, 125, 306, 467, 270, 475,
                        451, 464, 427, 509, 388, 334, 443, 165, 168, 125, 479, 261, 461, 261, 458,
                        342, 505, 469, 431, 413, 12, 48, 500, 477, 176, 484, 462, 461, 423, 474,
                        474, 507, 483, 0, 330, 61, 229, 268, 268, 389, 503, 267, 175, 494, 522,
                        231, 247, 493, 467, 142, 457, 517, 6, 6, 179, 503, 477, 461, 176, 251, 502,
                        459, 447, 70, 467, 497, 379, 505, 506, 506, 506, 507, 507, 507, 507, 507,
                        507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507,
                        507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507, 507,
                        507, 507, 507, 507, 507, 507, 507, 507, 508, 508, 508, 508, 508, 508, 508,
                        508, 508, 508, 508, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509, 509,
                        509, 509, 509, 510, 510, 510, 510, 510, 510, 510, 510, 510, 510, 510, 510,
                        510, 510, 510, 510, 510, 510, 510, 510, 510, 510
                },
                {
                        457, 498, 94, 506, 461, 221, 98, 479, 186, 172, 525, 345, 25, 166, 513,
                        456, 496, 498, 507, 345, 421, 496, 337, 394, 468, 493, 421, 71, 415, 459,
                        172, 177, 500, 300, 477, 3, 500, 166, 462, 477, 477, 388, 358, 300, 341,
                        94, 418, 161, 509, 149, 477, 479, 191, 483, 175, 166, 91, 503, 468, 491,
                        225, 393, 451, 0, 42, 229, 477, 20, 503, 472, 98, 50, 451, 50, 474, 42,
                        358, 478, 221, 46, 462, 295, 164, 491, 477, 71, 500, 472, 91, 445, 361,
                        465, 465, 18, 509, 393, 467, 510, 510, 510, 510, 511, 502, 512, 513, 513,
                        513, 513, 523, 513, 514, 514, 514, 514, 514, 514, 514, 515, 515, 515, 515,
                        515, 515, 516, 516, 517, 517, 517, 517, 517, 517, 517, 517, 503, 517, 517,
                        518, 518, 518, 518, 518, 518, 518, 518, 490, 518, 518, 518, 518, 518, 518,
                        520, 520, 520, 520, 520, 520, 520, 521, 521, 521, 521, 522, 522, 522, 522,
                        522, 522, 522, 522, 523, 523, 524, 524, 524, 524, 525, 525, 526, 526, 526,
                        498, 526, 526, 526, 526, 0, 0, 0, 0, 0
                },
                {
                        341, 159, 458, 172, 159, 147, 240, 457, 457, 233, 147, 91, 240, 339, 229,
                        79, 20, 399, 459, 112, 507, 507, 472, 472, 51, 166, 444, 477, 477, 472, 7,
                        160, 98, 51, 472, 215, 121, 184, 337, 457, 164, 510, 173, 111, 457, 168,
                        452, 164, 81, 520, 111, 477, 4, 328, 276, 328, 135, 276, 482, 268, 408,
                        164, 254, 0, 268, 4, 483, 291, 18, 483, 173, 442, 254, 457, 477, 483, 51,
                        421, 164, 186, 15, 505, 487, 117, 28, 133, 425, 477, 148, 449, 332, 168,
                        108, 421, 449, 499, 507, 51, 172, 456, 121, 292, 372, 328, 127, 47, 125,
                        280, 98, 399, 483, 331, 18, 445, 474, 474, 507, 81, 463, 478, 214, 483,
                        124, 423, 83, 172, 277, 295, 172, 339, 261, 15, 379, 136, 494, 391, 67,
                        472, 186, 408, 475, 472, 351, 334, 141, 214, 20, 494, 137, 482, 33, 477,
                        452, 180, 209, 141, 189, 219, 172, 472, 449, 211, 330, 186, 345, 168, 477,
                        439, 450, 70, 87, 501, 224, 372, 477, 271, 361, 483, 328, 471, 447, 456,
                        43, 31, 192, 510, 297, 188, 482, 477, 135
                },
                {
                        106, 510, 395, 18, 518, 106, 141, 329, 107, 250, 425, 394, 213, 117, 434,
                        477, 48, 254, 151, 111, 242, 235, 166, 121, 329, 518, 173, 467, 495, 177,
                        121, 492, 175, 479, 470, 505, 391, 23, 23, 350, 391, 54, 492, 509, 220,
                        491, 59, 46, 389, 432, 320, 128, 459, 250, 175, 415, 117, 421, 61, 61, 507,
                        172, 501, 0, 91, 179, 465, 57, 487, 13, 490, 220, 112, 31, 172, 394, 477,
                        523, 117, 135, 374, 494, 233, 477, 8, 46, 449, 507, 515, 21, 487, 497, 70,
                        491, 472, 337, 388, 439, 445, 291, 119, 172, 106, 482, 210, 191, 510, 477,
                        433, 41, 177, 299, 270, 50, 472, 229, 350, 229, 483, 333, 481, 408, 112,
                        341, 371, 315, 517, 399, 246, 455, 18, 431, 139, 206, 184, 111, 472, 462,
                        172, 451, 520, 247, 422, 23, 306, 48, 457, 180, 175, 177, 182, 439, 468,
                        70, 439, 444, 391, 341, 522, 485, 64, 81, 457, 470, 159, 143, 42, 214, 13,
                        337, 159, 117, 117, 413, 408, 452, 472, 25, 142, 254, 462, 243, 335, 24,
                        479, 246, 225, 232, 161, 234, 459
                },
                {
                        449, 479, 179, 514, 459, 492, 18, 91, 399, 472, 446, 468, 240, 514, 492,
                        487, 18, 509, 112, 487, 457, 465, 457, 47, 47, 458, 422, 521, 229, 388,
                        349, 478, 459, 518, 186, 347, 81, 59, 505, 441, 81, 457, 500, 186, 178,
                        351, 51, 168, 469, 433, 97, 122, 417, 63, 457, 502, 413, 478, 351, 175,
                        353, 517, 451, 0, 242, 71, 350, 518, 431, 351, 48, 165, 345, 345, 177, 493,
                        485, 106, 522, 456, 194, 186, 46, 318, 485, 314, 339, 445, 477, 292, 236,
                        257, 209, 518, 340, 510, 351, 411, 333, 508, 266, 472, 178, 159, 388, 313,
                        184, 281, 136, 151, 81, 502, 351, 477, 213, 225, 137, 178, 166, 393, 135,
                        352, 506, 167, 467, 345, 124, 213, 99, 521, 517, 451, 483, 393, 42, 415,
                        46, 175, 469, 475, 98, 483, 468, 4, 81, 518, 329, 268, 71, 413, 395, 261,
                        503, 175, 496, 345, 186, 472, 500, 46, 70, 179, 526, 456, 341, 180, 11, 83,
                        526, 15, 464, 121, 507, 295, 521, 56, 328, 451, 165, 451, 457, 147, 349,
                        213, 252, 456, 109, 24, 328, 15, 475
                },
                {
                        500, 317, 510, 291, 477, 172, 97, 97, 510, 525, 512, 460, 85, 329, 507,
                        178, 28, 68, 351, 399, 441, 63, 199, 295, 461, 26, 525, 271, 399, 177, 345,
                        260, 175, 339, 455, 451, 178, 353, 295, 184, 295, 245, 242, 228, 455, 352,
                        522, 346, 477, 81, 520, 322, 506, 483, 96, 57, 481, 507, 498, 46, 445, 422,
                        117, 0, 522, 360, 498, 443, 342, 500, 83, 431, 172, 237, 491, 18, 49, 242,
                        233, 422, 507, 413, 457, 214, 172, 517, 342, 507, 317, 520, 231, 493, 357,
                        443, 184, 459, 508, 81, 420, 173, 507, 510, 211, 346, 470, 487, 229, 479,
                        124, 457, 117, 328, 421, 472, 185, 472, 478, 501, 334, 388, 521, 236, 112,
                        51, 164, 250, 351, 390, 151, 125, 259, 467, 462, 412, 462, 186, 175, 388,
                        71, 43, 408, 20, 16, 350, 16, 98, 107, 399, 172, 481, 151, 467, 456, 125,
                        505, 213, 328, 477, 229, 339, 11, 122, 240, 83, 242, 45, 81, 445, 291, 271,
                        6, 471, 85, 89, 189, 388, 389, 280, 121, 478, 47, 395, 143, 470, 234, 484,
                        507, 4, 477, 328, 292
                },
                {
                        25, 63, 186, 235, 418, 345, 345, 233, 96, 139, 68, 18, 18, 184, 345, 96,
                        50, 172, 456, 485, 292, 507, 485, 507, 51, 92, 451, 240, 235, 459, 451,
                        221, 358, 462, 295, 418, 351, 353, 64, 523, 457, 214, 339, 235, 70, 111,
                        431, 97, 133, 222, 361, 246, 3, 172, 184, 206, 446, 472, 257, 192, 351,
                        247, 223, 0, 445, 507, 472, 431, 72, 479, 483, 45, 498, 141, 187, 485, 463,
                        69, 468, 108, 361, 388, 213, 399, 444, 339, 420, 98, 276, 339, 254, 368,
                        213, 341, 513, 159, 161, 322, 441, 378, 69, 209, 85, 478, 228, 160, 125,
                        484, 251, 481, 232, 48, 463, 328, 477, 32, 177, 277, 91, 341, 172, 449,
                        465, 339, 460, 117, 484, 487, 110, 172, 229, 98, 184, 328, 507, 369, 459,
                        43, 61, 347, 456, 341, 339, 388, 20, 510, 208, 477, 268, 122, 331, 240, 7,
                        271, 184, 357, 348, 48, 117, 236, 294, 478, 254, 479, 349, 265, 433, 341,
                        477, 359, 18, 59, 184, 439, 166, 510, 435, 345, 117, 361, 464, 352, 166,
                        470, 266, 339, 177, 49, 176, 246, 479
                },
                {
                        351, 213, 81, 236, 69, 6, 505, 108, 213, 473, 322, 15, 136, 136, 319, 510,
                        364, 98, 7, 509, 507, 474, 194, 503, 507, 395, 333, 106, 133, 184, 461,
                        140, 471, 189, 352, 509, 518, 507, 391, 232, 483, 390, 482, 484, 478, 503,
                        449, 117, 348, 509, 477, 236, 503, 500, 232, 518, 166, 450, 58, 166, 191,
                        477, 322, 0, 341, 142, 292, 333, 139, 15, 245, 315, 232, 368, 365, 172,
                        473, 459, 54, 62, 405, 471, 482, 159, 399, 117, 518, 117, 487, 16, 500,
                        483, 487, 423, 136, 505, 458, 470, 127, 179, 250, 416, 159, 509, 191, 18,
                        395, 455, 482, 98, 257, 482, 229, 441, 459, 117, 415, 482, 81, 393, 139,
                        232, 479, 57, 180, 339, 457, 411, 179, 277, 339, 7, 399, 43, 434, 483, 164,
                        18, 117, 441, 70, 65, 472, 522, 71, 175, 449, 479, 136, 147, 340, 112, 393,
                        460, 451, 42, 189, 339, 214, 457, 98, 13, 314, 435, 241, 315, 468, 173,
                        505, 395, 366, 268, 100, 15, 453, 149, 172, 229, 12, 175, 406, 222, 479,
                        483, 413, 259, 90, 457, 231, 63, 235
                },
                {
                        484, 242, 461, 44, 513, 451, 468, 469, 503, 177, 500, 26, 233, 111, 108,
                        235, 125, 379, 191, 164, 477, 172, 515, 106, 483, 175, 159, 225, 320, 229,
                        229, 242, 235, 69, 509, 232, 487, 59, 524, 20, 219, 59, 477, 20, 20, 446,
                        11, 59, 20, 329, 299, 299, 505, 58, 477, 362, 108, 395, 366, 42, 122, 483,
                        477, 0, 339, 498, 450, 441, 516, 431, 460, 431, 98, 508, 45, 291, 507, 505,
                        510, 423, 477, 351, 166, 89, 482, 277, 477, 178, 234, 470, 96, 181, 482,
                        350, 81, 180, 477, 351, 208, 467, 233, 166, 470, 186, 369, 524, 172, 259,
                        108, 339, 159, 462, 159, 451, 477, 453, 412, 18, 149, 420, 366, 470, 459,
                        124, 229, 168, 351, 155, 108, 295, 261, 299, 477, 213, 231, 68, 477, 457,
                        492, 319, 482, 233, 119, 122, 431, 258, 442, 46, 117, 332, 298, 178, 177,
                        488, 477, 247, 187, 432, 475, 3, 276, 172, 143, 8, 184, 335, 234, 341, 139,
                        85, 498, 471, 347, 483, 4, 468, 24, 214, 484, 399, 100, 53, 175, 406, 500,
                        306, 377, 246, 479, 507, 147
                },
                {
                        472, 456, 231, 417, 166, 136, 220, 16, 67, 510, 322, 482, 509, 179, 483,
                        58, 431, 418, 484, 306, 422, 415, 472, 48, 431, 395, 505, 482, 487, 98,
                        168, 418, 451, 278, 474, 50, 470, 418, 395, 507, 484, 413, 249, 139, 501,
                        503, 27, 48, 413, 96, 395, 241, 507, 67, 359, 341, 468, 483, 477, 98, 233,
                        395, 250, 0, 468, 500, 427, 106, 20, 20, 229, 484, 482, 246, 229, 435, 339,
                        388, 483, 366, 449, 117, 192, 23, 463, 393, 484, 66, 168, 12, 184, 20, 447,
                        213, 141, 395, 209, 503, 462, 242, 525, 64, 122, 164, 328, 464, 517, 168,
                        523, 279, 477, 443, 69, 395, 26, 48, 172, 208, 72, 224, 498, 1, 474, 117,
                        271, 477, 420, 229, 98, 18, 15, 143, 347, 478, 488, 188, 119, 137, 236, 89,
                        297, 97, 280, 482, 408, 213, 172, 393, 166, 68, 234, 477, 461, 18, 59, 140,
                        166, 466, 477, 315, 211, 97, 172, 264, 261, 498, 299, 127, 270, 250, 48,
                        223, 128, 100, 498, 417, 493, 457, 526, 172, 112, 494, 306, 263, 235, 513,
                        509, 423, 161, 65, 386
                },
                {
                        145, 458, 172, 160, 464, 457, 141, 306, 222, 472, 47, 90, 247, 117, 483,
                        222, 173, 128, 25, 457, 351, 461, 339, 348, 509, 521, 333, 225, 291, 482,
                        335, 460, 184, 481, 345, 477, 257, 371, 15, 399, 483, 487, 160, 214, 460,
                        460, 412, 422, 266, 457, 366, 51, 518, 521, 475, 456, 460, 487, 345, 481,
                        254, 44, 242, 0, 233, 513, 161, 345, 449, 345, 467, 79, 18, 470, 18, 497,
                        451, 506, 254, 235, 97, 259, 475, 378, 211, 112, 259, 216, 231, 491, 42,
                        482, 339, 472, 42, 491, 352, 480, 70, 25, 87, 222, 229, 400, 58, 498, 68,
                        278, 229, 214, 178, 481, 214, 182, 412, 478, 48, 178, 241, 209, 455, 166,
                        347, 6, 413, 91, 194, 295, 151, 46, 417, 125, 1, 100, 149, 82, 3, 3, 417,
                        152, 439, 48, 320, 225, 32, 330, 339, 479, 17, 412, 81, 261, 322, 186, 233,
                        337, 53, 177, 309, 347, 242, 35, 79, 149, 179, 469, 477, 374, 328, 366, 40,
                        168, 279, 451, 175, 278, 245, 161, 236, 482, 483, 348, 483, 164, 451, 507,
                        320, 422, 50, 494, 143
                },
        };
    }

    private static short[][] initCode4() {
        return new short[][] {
                {
                        457, 491, 507, 483, 478, 524, 254, 505, 90, 505, 484, 117, 432, 340, 334,
                        50, 524, 58, 482, 478, 337, 524, 145, 515, 242, 221, 472, 423, 500, 421,
                        515, 459, 91, 421, 350, 57, 487, 107, 194, 412, 50, 67, 487, 481, 5, 498,
                        176, 330, 46, 483, 229, 493, 477, 176, 421, 334, 299, 477, 472, 477, 293,
                        366, 398, 0, 472, 236, 261, 261, 293, 463, 177, 395, 261, 472, 20, 395,
                        477, 457, 179, 350, 477, 233, 72, 502, 236, 334, 350, 7, 505, 507, 7, 243,
                        117, 277, 84, 459, 345, 451, 455, 251, 501, 117, 108, 172, 339, 341, 339,
                        351, 471, 459, 6, 34, 221, 7, 161, 194, 89, 173, 466, 69, 135, 254, 265,
                        477, 89, 345, 470, 506, 223, 220, 411, 472, 136, 461, 143, 206, 186, 364,
                        474, 448, 490, 451, 483, 67, 241, 518, 257, 394, 411, 172, 501, 235, 79,
                        23, 477, 83, 48, 318, 63, 470, 473, 160, 220, 457, 43, 165, 474, 507, 177,
                        351, 376, 108, 350, 4, 140, 249, 298, 487, 458, 319, 110, 364, 211, 398,
                        416, 18, 185, 229, 483, 459, 478
                },
                {
                        208, 54, 518, 108, 483, 456, 147, 123, 339, 252, 363, 81, 408, 457, 477,
                        41, 477, 441, 457, 307, 341, 350, 175, 502, 475, 478, 314, 109, 505, 464,
                        90, 485, 508, 117, 328, 369, 463, 98, 186, 96, 135, 478, 341, 11, 457, 361,
                        44, 298, 111, 487, 477, 347, 328, 143, 159, 478, 187, 84, 477, 508, 457,
                        121, 363, 0, 168, 420, 192, 484, 242, 288, 452, 97, 518, 291, 441, 395,
                        265, 328, 194, 236, 24, 420, 161, 25, 328, 483, 408, 526, 26, 509, 432,
                        173, 505, 395, 522, 507, 184, 414, 477, 143, 246, 291, 391, 63, 261, 161,
                        40, 451, 451, 257, 280, 501, 180, 186, 233, 462, 470, 164, 54, 168, 416,
                        478, 89, 395, 422, 444, 457, 361, 483, 48, 477, 460, 25, 483, 167, 40, 412,
                        268, 466, 179, 376, 513, 279, 328, 456, 141, 319, 466, 460, 445, 4, 483,
                        18, 128, 6, 179, 42, 462, 235, 479, 399, 72, 66, 42, 456, 507, 313, 49,
                        456, 189, 43, 55, 411, 20, 298, 161, 51, 322, 68, 473, 526, 291, 117, 44,
                        477, 477, 439, 472, 33, 189, 470
                },
                {
                        434, 477, 477, 468, 502, 319, 151, 379, 394, 522, 522, 25, 510, 48, 488,
                        334, 439, 341, 117, 499, 254, 472, 117, 229, 485, 328, 473, 11, 25, 178,
                        351, 467, 506, 271, 457, 457, 81, 173, 271, 421, 393, 477, 408, 213, 188,
                        15, 517, 464, 159, 177, 48, 199, 246, 333, 395, 270, 194, 478, 187, 509,
                        49, 460, 439, 0, 268, 225, 172, 483, 467, 361, 525, 507, 406, 229, 459,
                        464, 352, 328, 477, 510, 460, 266, 210, 474, 459, 459, 466, 187, 40, 223,
                        172, 328, 366, 261, 477, 478, 140, 194, 487, 15, 411, 341, 214, 215, 477,
                        431, 352, 346, 464, 110, 43, 450, 50, 161, 65, 487, 214, 98, 228, 515, 345,
                        18, 413, 341, 481, 180, 345, 49, 51, 235, 259, 432, 166, 402, 472, 451,
                        159, 265, 192, 418, 247, 215, 182, 222, 483, 43, 457, 452, 167, 98, 351,
                        353, 151, 432, 353, 191, 514, 317, 342, 376, 339, 408, 40, 112, 484, 271,
                        262, 96, 261, 136, 20, 452, 148, 451, 224, 122, 399, 240, 242, 473, 408,
                        97, 236, 254, 246, 468, 317, 445, 159, 265, 180
                },
                {
                        4, 88, 482, 379, 193, 341, 408, 289, 84, 147, 485, 507, 199, 350, 461, 503,
                        482, 492, 431, 67, 137, 159, 508, 441, 247, 259, 222, 449, 518, 487, 15,
                        413, 483, 42, 435, 25, 147, 173, 159, 185, 112, 42, 449, 507, 445, 468,
                        456, 483, 433, 213, 517, 244, 464, 347, 393, 147, 244, 475, 184, 497, 184,
                        459, 434, 0, 252, 337, 229, 317, 485, 47, 124, 229, 425, 24, 510, 505, 441,
                        237, 524, 184, 43, 484, 175, 123, 84, 423, 43, 245, 210, 236, 328, 242,
                        229, 342, 335, 185, 265, 164, 451, 234, 173, 505, 478, 166, 510, 172, 467,
                        166, 423, 470, 176, 237, 161, 470, 366, 413, 456, 220, 451, 517, 185, 34,
                        12, 457, 257, 164, 510, 339, 457, 411, 91, 517, 328, 262, 122, 110, 61,
                        393, 139, 242, 402, 462, 472, 262, 350, 412, 165, 467, 321, 175, 468, 455,
                        257, 472, 347, 194, 391, 252, 479, 337, 229, 366, 418, 167, 18, 466, 117,
                        422, 318, 266, 165, 479, 461, 221, 32, 161, 246, 164, 231, 510, 477, 242,
                        468, 122, 399, 408, 388, 390, 439, 42, 220
                },
                {
                        524, 322, 4, 328, 459, 471, 515, 229, 194, 206, 418, 215, 91, 515, 56, 518,
                        506, 16, 295, 520, 97, 472, 87, 477, 341, 515, 172, 483, 179, 139, 254, 43,
                        444, 457, 231, 423, 136, 457, 399, 505, 247, 259, 242, 162, 21, 119, 220,
                        198, 109, 456, 277, 449, 161, 73, 459, 446, 168, 232, 107, 258, 189, 473,
                        395, 0, 231, 143, 459, 91, 441, 451, 449, 117, 365, 172, 98, 187, 505, 431,
                        498, 161, 473, 92, 458, 483, 199, 394, 165, 451, 117, 502, 40, 346, 395,
                        159, 214, 432, 345, 415, 229, 70, 42, 231, 366, 337, 18, 149, 517, 147,
                        462, 479, 485, 111, 149, 7, 13, 141, 71, 261, 482, 46, 297, 341, 341, 456,
                        233, 341, 164, 175, 175, 521, 471, 456, 181, 494, 477, 106, 173, 180, 69,
                        160, 318, 26, 229, 350, 461, 431, 354, 214, 451, 164, 242, 54, 165, 350,
                        458, 6, 135, 418, 237, 459, 235, 184, 462, 263, 415, 219, 470, 166, 442,
                        507, 193, 507, 172, 98, 42, 457, 184, 42, 180, 301, 261, 117, 18, 483, 51,
                        406, 110, 472, 456, 483, 18
                },
                {
                        345, 160, 440, 520, 164, 475, 265, 175, 92, 175, 411, 214, 161, 468, 503,
                        178, 505, 20, 508, 518, 466, 475, 257, 315, 3, 121, 341, 257, 417, 418,
                        318, 458, 231, 417, 189, 522, 475, 300, 453, 364, 422, 417, 342, 229, 406,
                        515, 25, 317, 372, 328, 377, 123, 518, 456, 479, 165, 433, 237, 189, 416,
                        395, 412, 449, 0, 149, 505, 505, 246, 477, 484, 422, 295, 457, 173, 125,
                        249, 185, 411, 522, 417, 446, 112, 452, 276, 242, 417, 313, 522, 442, 466,
                        139, 468, 231, 412, 251, 268, 246, 179, 518, 141, 298, 483, 408, 68, 509,
                        388, 346, 471, 359, 399, 243, 177, 332, 40, 229, 333, 447, 459, 417, 81,
                        451, 98, 180, 21, 178, 43, 18, 42, 301, 6, 484, 435, 456, 135, 268, 328, 3,
                        332, 48, 229, 472, 342, 330, 43, 225, 501, 457, 388, 18, 294, 268, 403,
                        119, 119, 117, 301, 518, 178, 186, 13, 492, 408, 117, 521, 477, 301, 420,
                        461, 162, 329, 229, 339, 194, 515, 30, 507, 456, 6, 237, 388, 21, 61, 42,
                        172, 460, 177, 483, 509, 125, 449, 213
                },
                {
                        18, 451, 237, 81, 376, 520, 477, 242, 199, 342, 65, 339, 43, 422, 251, 481,
                        42, 112, 180, 21, 399, 247, 466, 61, 240, 491, 491, 32, 229, 458, 457, 192,
                        403, 17, 501, 341, 47, 242, 162, 172, 337, 415, 342, 334, 235, 379, 466,
                        459, 47, 214, 408, 237, 280, 165, 331, 415, 108, 345, 352, 473, 422, 460,
                        483, 0, 177, 525, 233, 346, 223, 96, 463, 491, 339, 175, 508, 79, 471, 479,
                        96, 186, 300, 491, 337, 434, 446, 501, 87, 388, 189, 175, 110, 415, 242,
                        185, 166, 483, 231, 487, 328, 399, 122, 462, 117, 509, 117, 51, 69, 209,
                        148, 176, 128, 461, 431, 236, 339, 110, 389, 142, 97, 398, 237, 352, 449,
                        518, 194, 460, 431, 263, 166, 408, 20, 135, 515, 265, 179, 505, 366, 124,
                        229, 477, 175, 23, 330, 251, 225, 267, 378, 462, 233, 497, 176, 341, 345,
                        164, 523, 474, 172, 54, 490, 481, 87, 172, 451, 23, 265, 186, 194, 240, 83,
                        69, 334, 265, 173, 106, 133, 467, 471, 155, 474, 246, 457, 166, 231, 339,
                        479, 339, 161, 215, 472, 61, 449
                },
                {
                        517, 378, 341, 517, 225, 18, 434, 468, 475, 97, 143, 422, 351, 111, 68,
                        477, 3, 520, 470, 84, 510, 155, 515, 172, 295, 151, 168, 348, 23, 479, 214,
                        297, 467, 175, 175, 341, 40, 507, 264, 229, 225, 172, 523, 213, 389, 322,
                        219, 91, 485, 58, 247, 21, 13, 242, 459, 212, 240, 98, 242, 175, 221, 25,
                        341, 0, 474, 42, 460, 175, 457, 139, 31, 295, 225, 64, 351, 317, 246, 523,
                        243, 526, 477, 186, 422, 510, 221, 457, 473, 464, 319, 464, 252, 471, 507,
                        459, 483, 477, 43, 46, 184, 254, 483, 484, 458, 280, 3, 422, 179, 165, 479,
                        65, 61, 468, 501, 337, 30, 351, 242, 18, 491, 452, 451, 487, 423, 456, 390,
                        339, 40, 249, 229, 328, 263, 474, 369, 175, 51, 47, 61, 461, 109, 314, 510,
                        277, 507, 503, 240, 182, 333, 242, 458, 461, 482, 507, 445, 507, 236, 135,
                        81, 229, 445, 47, 193, 223, 471, 359, 507, 505, 140, 339, 435, 216, 182,
                        162, 155, 141, 178, 243, 185, 4, 467, 108, 136, 117, 186, 518, 417, 236,
                        51, 111, 91, 341, 502
                },
                {
                        507, 479, 165, 518, 25, 422, 364, 17, 237, 468, 219, 42, 501, 501, 182, 6,
                        85, 186, 233, 291, 258, 249, 402, 388, 388, 258, 472, 18, 147, 18, 388,
                        341, 192, 17, 159, 370, 376, 459, 459, 175, 265, 458, 404, 90, 498, 280,
                        500, 322, 458, 236, 139, 18, 370, 151, 449, 125, 125, 107, 51, 460, 141,
                        265, 384, 0, 215, 232, 247, 435, 386, 184, 485, 485, 42, 351, 235, 422,
                        387, 215, 472, 452, 472, 483, 167, 483, 452, 460, 13, 460, 351, 474, 452,
                        317, 478, 451, 478, 216, 353, 246, 56, 232, 143, 175, 81, 184, 62, 393,
                        278, 498, 231, 221, 172, 332, 184, 350, 92, 56, 46, 247, 40, 184, 468, 257,
                        479, 505, 110, 418, 416, 462, 124, 65, 124, 406, 23, 364, 510, 462, 179,
                        342, 339, 51, 422, 510, 161, 122, 485, 348, 445, 186, 345, 347, 242, 525,
                        457, 184, 484, 225, 472, 235, 25, 40, 482, 6, 268, 63, 389, 432, 487, 231,
                        330, 70, 172, 23, 477, 361, 98, 136, 194, 242, 507, 477, 505, 161, 229,
                        474, 395, 507, 352, 242, 503, 292, 450
                },
                {
                        221, 391, 352, 325, 432, 295, 418, 189, 151, 353, 55, 449, 90, 339, 214,
                        422, 449, 330, 459, 457, 166, 42, 328, 422, 164, 418, 452, 446, 147, 189,
                        391, 166, 418, 164, 224, 463, 477, 507, 478, 473, 90, 98, 394, 320, 199,
                        487, 507, 328, 47, 98, 351, 81, 236, 507, 24, 97, 399, 415, 106, 141, 483,
                        187, 345, 0, 464, 57, 456, 458, 388, 394, 337, 505, 459, 72, 320, 477, 94,
                        245, 478, 184, 505, 242, 394, 459, 478, 510, 473, 362, 458, 49, 472, 478,
                        483, 81, 451, 295, 515, 58, 518, 62, 242, 235, 451, 175, 342, 173, 172,
                        172, 191, 79, 121, 175, 492, 313, 236, 26, 17, 497, 328, 334, 119, 219,
                        122, 149, 421, 124, 494, 463, 487, 141, 151, 491, 254, 483, 43, 291, 339,
                        394, 475, 44, 472, 166, 26, 147, 141, 468, 214, 3, 266, 444, 470, 474, 457,
                        279, 16, 395, 213, 477, 507, 518, 121, 179, 505, 220, 350, 172, 70, 117,
                        42, 172, 457, 81, 483, 135, 179, 351, 175, 176, 332, 254, 136, 456, 136,
                        172, 184, 175, 329, 193, 346, 417, 8
                },
                {
                        4, 178, 339, 472, 166, 487, 456, 422, 172, 72, 6, 457, 478, 372, 359, 235,
                        443, 79, 328, 415, 483, 472, 111, 291, 106, 172, 72, 478, 507, 240, 457,
                        229, 229, 353, 507, 477, 347, 341, 164, 135, 524, 161, 488, 483, 50, 415,
                        147, 455, 403, 164, 184, 488, 481, 172, 457, 237, 229, 306, 469, 488, 172,
                        172, 276, 0, 116, 457, 271, 111, 318, 487, 48, 473, 4, 456, 83, 71, 161,
                        84, 271, 46, 498, 236, 339, 509, 159, 500, 478, 510, 444, 236, 89, 479,
                        456, 236, 236, 159, 478, 125, 25, 254, 271, 65, 175, 376, 399, 43, 242,
                        337, 351, 331, 72, 459, 54, 89, 471, 478, 194, 487, 108, 48, 177, 91, 85,
                        482, 484, 143, 485, 455, 364, 165, 180, 368, 420, 133, 516, 276, 474, 341,
                        240, 89, 188, 242, 173, 393, 509, 526, 137, 505, 351, 507, 180, 140, 89,
                        472, 211, 372, 145, 329, 505, 261, 444, 246, 67, 319, 449, 294, 180, 472,
                        110, 483, 520, 87, 175, 62, 278, 262, 447, 98, 399, 47, 479, 125, 247, 23,
                        427, 507, 53, 136, 259, 378, 388
                },
                {
                        250, 250, 487, 237, 259, 23, 456, 451, 216, 457, 477, 70, 427, 483, 240,
                        69, 172, 318, 473, 451, 446, 457, 172, 456, 259, 225, 229, 417, 3, 110, 69,
                        240, 236, 477, 112, 229, 13, 151, 151, 151, 24, 348, 432, 505, 47, 348,
                        232, 180, 432, 110, 262, 13, 432, 166, 475, 87, 40, 341, 361, 81, 91, 456,
                        361, 0, 347, 298, 478, 372, 276, 447, 522, 11, 477, 474, 423, 18, 178, 159,
                        319, 24, 478, 418, 423, 178, 4, 154, 133, 340, 435, 479, 415, 433, 468,
                        206, 17, 418, 501, 231, 235, 483, 457, 477, 386, 463, 457, 21, 372, 184,
                        412, 21, 21, 399, 135, 136, 161, 110, 172, 221, 483, 320, 254, 500, 180,
                        291, 237, 477, 473, 451, 96, 342, 395, 161, 510, 468, 420, 475, 473, 456,
                        147, 258, 44, 472, 161, 483, 451, 92, 13, 468, 20, 443, 237, 251, 389, 487,
                        477, 483, 108, 415, 459, 186, 64, 408, 423, 467, 457, 229, 161, 181, 161,
                        110, 395, 408, 459, 507, 351, 161, 117, 526, 261, 507, 59, 505, 433, 339,
                        42, 457, 517, 457, 358, 425, 421
                },
                {
                        328, 216, 219, 498, 12, 101, 350, 350, 391, 390, 271, 175, 81, 312, 423,
                        418, 462, 317, 125, 24, 216, 422, 241, 166, 345, 469, 172, 175, 176, 42,
                        418, 161, 459, 341, 91, 447, 175, 221, 451, 361, 117, 447, 352, 125, 451,
                        390, 147, 43, 216, 365, 487, 391, 451, 125, 117, 423, 135, 487, 124, 18,
                        469, 415, 91, 0, 447, 91, 117, 182, 459, 462, 459, 172, 488, 224, 322, 479,
                        479, 487, 322, 4, 478, 460, 161, 475, 87, 348, 214, 460, 405, 147, 467,
                        477, 467, 136, 411, 214, 339, 94, 173, 166, 307, 296, 473, 518, 353, 341,
                        265, 425, 339, 94, 254, 258, 123, 483, 483, 418, 469, 263, 172, 122, 71,
                        162, 45, 96, 471, 517, 20, 112, 107, 3, 229, 240, 498, 439, 81, 219, 445,
                        117, 464, 252, 458, 345, 499, 89, 280, 125, 455, 339, 94, 15, 87, 46, 509,
                        178, 81, 468, 20, 503, 142, 377, 348, 351, 96, 79, 176, 27, 259, 25, 191,
                        507, 117, 117, 467, 262, 208, 96, 263, 70, 394, 484, 477, 415, 518, 48,
                        270, 220, 175, 81, 417, 471, 291
                },
                {
                        148, 483, 449, 111, 96, 81, 83, 317, 334, 236, 45, 180, 225, 347, 345, 98,
                        98, 451, 462, 216, 393, 477, 477, 194, 94, 483, 333, 225, 433, 173, 440,
                        166, 214, 173, 246, 435, 47, 479, 487, 161, 147, 180, 443, 443, 332, 220,
                        443, 518, 518, 56, 87, 220, 421, 147, 341, 212, 522, 459, 347, 477, 374,
                        431, 98, 0, 98, 472, 452, 472, 483, 513, 472, 459, 463, 477, 484, 377, 432,
                        432, 176, 214, 225, 223, 330, 512, 251, 63, 474, 149, 345, 136, 470, 472,
                        166, 388, 366, 259, 23, 415, 328, 369, 412, 214, 254, 194, 266, 330, 47,
                        191, 235, 136, 87, 18, 352, 432, 108, 505, 391, 449, 442, 117, 123, 136,
                        229, 472, 328, 221, 229, 172, 497, 151, 139, 185, 179, 119, 477, 334, 502,
                        233, 441, 54, 388, 258, 41, 307, 26, 420, 184, 11, 341, 109, 192, 440, 168,
                        314, 483, 506, 136, 194, 334, 26, 25, 485, 271, 421, 83, 406, 395, 468,
                        418, 18, 291, 328, 97, 193, 223, 106, 482, 47, 173, 280, 475, 47, 84, 478,
                        189, 510, 87, 88, 162, 352, 144
                },
                {
                        459, 332, 242, 235, 295, 352, 435, 181, 335, 479, 483, 225, 112, 88, 137,
                        117, 458, 500, 319, 372, 117, 420, 234, 172, 468, 461, 184, 21, 408, 451,
                        473, 474, 412, 189, 412, 108, 237, 457, 237, 330, 330, 237, 21, 21, 21,
                        233, 21, 379, 112, 466, 473, 500, 408, 474, 237, 110, 108, 110, 395, 30,
                        172, 87, 408, 0, 445, 500, 516, 361, 483, 473, 395, 478, 108, 108, 416,
                        478, 510, 477, 526, 18, 178, 423, 13, 59, 434, 408, 13, 395, 97, 145, 361,
                        432, 177, 173, 24, 474, 439, 386, 84, 506, 379, 49, 422, 4, 366, 223, 220,
                        425, 199, 497, 229, 124, 98, 67, 244, 237, 189, 175, 222, 347, 184, 1, 342,
                        306, 16, 73, 194, 215, 136, 168, 320, 185, 421, 518, 346, 189, 408, 98, 40,
                        412, 164, 3, 241, 342, 110, 257, 268, 125, 185, 276, 237, 477, 173, 23, 21,
                        422, 251, 246, 481, 522, 468, 81, 42, 186, 337, 242, 96, 221, 337, 64, 342,
                        79, 168, 517, 477, 40, 21, 508, 393, 67, 507, 18, 518, 268, 399, 247, 172,
                        117, 222, 194, 361
                },
                {
                        59, 460, 473, 185, 106, 224, 457, 25, 282, 98, 26, 187, 90, 413, 483, 457,
                        474, 215, 143, 395, 175, 515, 24, 459, 26, 475, 421, 110, 501, 451, 139,
                        98, 302, 487, 161, 165, 166, 500, 160, 151, 464, 111, 451, 136, 40, 411,
                        422, 25, 124, 457, 214, 237, 412, 459, 475, 452, 268, 422, 251, 18, 483,
                        466, 179, 0, 376, 443, 513, 388, 48, 70, 477, 339, 359, 47, 451, 460, 500,
                        111, 145, 259, 472, 268, 42, 460, 246, 491, 279, 87, 445, 97, 173, 224,
                        457, 90, 187, 143, 136, 505, 172, 379, 292, 117, 358, 141, 177, 149, 457,
                        334, 85, 161, 481, 182, 484, 13, 505, 136, 89, 242, 351, 48, 408, 106, 507,
                        137, 466, 243, 25, 229, 161, 483, 459, 431, 456, 263, 4, 15, 57, 161, 98,
                        59, 257, 456, 474, 175, 479, 503, 237, 233, 177, 182, 483, 161, 242, 139,
                        24, 87, 178, 229, 388, 229, 482, 122, 194, 70, 498, 319, 510, 468, 173,
                        471, 477, 507, 223, 456, 67, 459, 386, 510, 110, 136, 451, 483, 483, 70,
                        219, 477, 160, 67, 241, 173, 378
                },
                {
                        475, 25, 111, 25, 291, 18, 334, 441, 147, 110, 175, 4, 3, 459, 487, 111,
                        332, 463, 249, 483, 333, 341, 81, 445, 503, 48, 470, 510, 507, 320, 463,
                        363, 372, 487, 452, 507, 70, 247, 482, 25, 13, 211, 445, 477, 351, 337,
                        351, 181, 334, 502, 484, 322, 509, 184, 510, 301, 184, 328, 522, 173, 236,
                        505, 499, 0, 117, 473, 395, 18, 445, 445, 408, 237, 249, 329, 423, 507,
                        364, 427, 89, 470, 352, 393, 181, 106, 145, 25, 510, 478, 246, 509, 70,
                        145, 237, 184, 411, 347, 252, 48, 268, 11, 172, 165, 21, 246, 479, 499,
                        240, 478, 50, 11, 220, 477, 83, 328, 83, 351, 477, 411, 457, 349, 516, 20,
                        474, 433, 90, 194, 483, 470, 184, 483, 477, 40, 276, 361, 179, 257, 317,
                        71, 340, 125, 198, 231, 47, 231, 175, 21, 51, 431, 18, 184, 97, 68, 15, 13,
                        247, 20, 221, 48, 503, 342, 366, 317, 399, 467, 187, 66, 179, 225, 518, 44,
                        408, 168, 223, 422, 313, 241, 176, 300, 268, 85, 87, 70, 236, 297, 143,
                        214, 6, 421, 147, 339, 148
                },
                {
                        232, 147, 441, 468, 443, 187, 98, 47, 464, 408, 242, 515, 509, 391, 329,
                        215, 423, 220, 520, 194, 339, 339, 472, 110, 378, 472, 125, 474, 456, 329,
                        61, 329, 341, 110, 165, 341, 168, 483, 431, 352, 458, 520, 214, 365, 408,
                        137, 445, 443, 412, 341, 47, 507, 237, 322, 427, 457, 32, 91, 472, 484,
                        521, 461, 388, 0, 339, 507, 403, 242, 457, 246, 501, 268, 474, 30, 330, 61,
                        351, 18, 507, 483, 467, 162, 25, 413, 461, 235, 500, 96, 237, 445, 35, 83,
                        461, 434, 472, 246, 500, 178, 151, 479, 194, 147, 98, 513, 295, 251, 377,
                        149, 366, 332, 161, 341, 350, 172, 41, 166, 125, 259, 117, 328, 369, 459,
                        149, 178, 133, 90, 478, 48, 147, 136, 194, 229, 482, 357, 498, 350, 236,
                        47, 482, 349, 173, 280, 507, 408, 351, 435, 216, 339, 177, 473, 270, 393,
                        503, 390, 456, 229, 51, 117, 342, 348, 339, 457, 483, 110, 143, 143, 477,
                        328, 433, 352, 449, 222, 259, 57, 364, 278, 117, 214, 194, 117, 412, 483,
                        482, 241, 350, 20, 270, 347, 6, 251, 252
                },
                {
                        180, 477, 475, 503, 332, 509, 472, 492, 247, 427, 460, 172, 403, 184, 457,
                        164, 229, 330, 363, 478, 330, 464, 347, 520, 413, 214, 403, 451, 451, 451,
                        483, 122, 477, 192, 13, 15, 499, 431, 461, 210, 443, 128, 329, 483, 415,
                        334, 461, 318, 186, 67, 268, 233, 241, 461, 492, 431, 23, 212, 242, 124,
                        345, 193, 345, 0, 223, 378, 215, 431, 109, 466, 357, 70, 215, 23, 107, 328,
                        107, 431, 13, 18, 365, 117, 106, 364, 351, 133, 485, 322, 511, 390, 417,
                        431, 229, 23, 431, 322, 411, 249, 484, 457, 48, 422, 318, 395, 165, 32,
                        330, 422, 457, 460, 508, 501, 401, 254, 322, 166, 317, 388, 168, 259, 42,
                        231, 264, 229, 91, 351, 116, 479, 348, 458, 395, 510, 483, 172, 91, 172,
                        175, 502, 518, 161, 349, 334, 68, 394, 494, 135, 229, 408, 433, 173, 20,
                        48, 208, 18, 459, 472, 352, 506, 187, 395, 123, 314, 390, 461, 348, 494,
                        346, 510, 371, 341, 445, 18, 70, 206, 484, 461, 505, 214, 165, 160, 135,
                        110, 229, 18, 48, 413, 264, 90, 242, 92, 141
                },
                {
                        506, 520, 405, 175, 97, 161, 219, 339, 231, 505, 322, 249, 376, 251, 251,
                        383, 467, 234, 341, 341, 279, 216, 297, 23, 358, 90, 90, 280, 460, 457, 90,
                        147, 90, 90, 182, 483, 483, 472, 229, 463, 141, 520, 237, 462, 389, 257,
                        339, 339, 119, 467, 483, 443, 339, 232, 122, 330, 18, 339, 467, 50, 472,
                        500, 483, 0, 72, 361, 178, 7, 159, 445, 84, 172, 483, 162, 346, 417, 148,
                        444, 268, 178, 393, 11, 484, 328, 247, 452, 161, 242, 488, 109, 111, 276,
                        482, 329, 268, 151, 458, 83, 491, 79, 25, 220, 509, 483, 483, 49, 457, 295,
                        307, 54, 388, 477, 18, 508, 11, 109, 125, 242, 510, 494, 457, 390, 451,
                        259, 398, 32, 49, 259, 347, 294, 173, 350, 386, 18, 81, 342, 417, 178, 422,
                        457, 459, 261, 7, 229, 433, 457, 59, 30, 235, 520, 376, 160, 491, 59, 467,
                        365, 350, 176, 127, 172, 477, 236, 457, 510, 110, 175, 329, 151, 477, 261,
                        507, 339, 339, 474, 72, 117, 351, 182, 346, 234, 518, 491, 278, 503, 176,
                        48, 87, 122, 509, 477, 136
                },
                {
                        462, 147, 328, 236, 445, 25, 350, 333, 117, 18, 172, 451, 351, 84, 25, 482,
                        142, 328, 292, 506, 420, 319, 117, 498, 184, 136, 395, 89, 69, 418, 340,
                        399, 160, 505, 106, 4, 451, 502, 510, 478, 234, 246, 439, 477, 477, 24,
                        451, 177, 209, 462, 459, 125, 166, 223, 117, 193, 466, 97, 187, 431, 262,
                        390, 498, 0, 417, 347, 483, 282, 503, 142, 128, 413, 456, 350, 393, 337,
                        164, 433, 229, 386, 386, 193, 259, 47, 229, 521, 457, 481, 393, 518, 339,
                        506, 460, 282, 57, 172, 84, 526, 445, 459, 266, 507, 472, 387, 47, 441,
                        225, 215, 320, 161, 431, 467, 145, 422, 223, 26, 177, 457, 184, 229, 470,
                        395, 67, 96, 349, 469, 63, 22, 13, 418, 175, 117, 342, 507, 117, 388, 229,
                        445, 173, 25, 420, 214, 345, 18, 459, 459, 172, 177, 232, 172, 58, 163, 48,
                        507, 83, 25, 507, 175, 85, 53, 508, 184, 97, 67, 329, 365, 295, 317, 339,
                        51, 186, 337, 108, 63, 510, 235, 42, 234, 523, 462, 507, 84, 268, 466, 268,
                        328, 161, 186, 389, 136, 524
                },
                {
                        346, 136, 509, 89, 220, 110, 291, 477, 215, 242, 182, 43, 180, 245, 236,
                        521, 229, 259, 520, 507, 292, 161, 483, 81, 395, 393, 164, 431, 160, 506,
                        510, 219, 520, 495, 20, 20, 164, 352, 495, 451, 451, 483, 57, 365, 498,
                        165, 231, 472, 350, 350, 175, 18, 98, 473, 117, 457, 459, 458, 451, 161,
                        395, 371, 468, 0, 452, 341, 149, 456, 318, 378, 237, 249, 395, 395, 215,
                        518, 427, 418, 474, 125, 481, 341, 339, 452, 371, 393, 231, 6, 224, 166,
                        265, 172, 433, 351, 175, 393, 251, 133, 413, 507, 518, 348, 232, 483, 229,
                        452, 435, 172, 320, 110, 386, 478, 3, 459, 250, 46, 184, 13, 433, 518, 478,
                        483, 58, 455, 265, 484, 445, 515, 416, 187, 184, 246, 351, 50, 349, 243,
                        456, 491, 270, 6, 237, 15, 463, 482, 109, 7, 333, 292, 242, 413, 117, 160,
                        420, 141, 178, 451, 106, 172, 177, 460, 470, 128, 229, 231, 175, 395, 433,
                        142, 386, 164, 172, 348, 236, 521, 110, 215, 43, 136, 291, 292, 84, 395,
                        518, 111, 85, 98, 350, 117, 165, 20, 378
                },
                {
                        457, 350, 21, 172, 172, 510, 176, 466, 513, 481, 501, 192, 469, 22, 483,
                        351, 460, 25, 177, 470, 413, 165, 525, 445, 388, 108, 186, 235, 470, 263,
                        457, 497, 460, 111, 139, 160, 211, 495, 378, 500, 122, 141, 479, 229, 43,
                        225, 399, 3, 366, 172, 483, 161, 399, 229, 234, 246, 264, 505, 460, 98,
                        242, 139, 229, 0, 459, 72, 172, 444, 7, 151, 482, 498, 25, 328, 193, 439,
                        495, 160, 211, 498, 187, 481, 339, 393, 18, 43, 431, 452, 451, 352, 176,
                        318, 341, 451, 457, 161, 122, 6, 339, 418, 139, 474, 224, 21, 469, 251,
                        265, 481, 141, 388, 525, 229, 68, 473, 68, 345, 251, 175, 184, 365, 135,
                        15, 178, 440, 209, 136, 81, 160, 125, 194, 18, 241, 340, 212, 23, 91, 257,
                        7, 472, 232, 461, 450, 48, 460, 472, 434, 423, 481, 19, 215, 254, 357, 433,
                        172, 518, 466, 352, 182, 23, 164, 234, 256, 166, 261, 172, 187, 510, 261,
                        339, 6, 391, 235, 69, 51, 482, 458, 477, 351, 91, 229, 348, 30, 4, 111,
                        482, 456, 472, 457, 350, 147, 498
                },
                {
                        500, 139, 353, 477, 117, 229, 507, 26, 472, 117, 502, 172, 112, 366, 472,
                        395, 112, 266, 13, 484, 507, 161, 347, 141, 111, 452, 500, 395, 483, 116,
                        474, 186, 186, 328, 164, 505, 13, 472, 471, 506, 109, 112, 452, 313, 69,
                        125, 366, 236, 264, 117, 445, 452, 229, 20, 507, 125, 484, 59, 351, 461,
                        48, 70, 184, 0, 474, 136, 508, 483, 473, 483, 471, 434, 483, 432, 479, 94,
                        456, 106, 137, 3, 507, 472, 155, 461, 173, 234, 510, 473, 477, 159, 242,
                        366, 270, 125, 361, 461, 466, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0
                },
                {
                        509, 48, 246, 155, 292, 98, 243, 173, 172, 441, 185, 445, 337, 456, 185,
                        483, 25, 187, 187, 18, 457, 187, 184, 441, 180, 431, 98, 98, 213, 161, 456,
                        393, 220, 177, 317, 242, 328, 399, 117, 471, 517, 322, 350, 341, 15, 84,
                        242, 353, 175, 184, 441, 471, 484, 339, 229, 475, 515, 206, 97, 215, 394,
                        339, 180, 0, 477, 477, 348, 518, 220, 89, 339, 442, 128, 184, 351, 477,
                        525, 172, 399, 446, 48, 263, 365, 471, 350, 431, 161, 431, 98, 178, 254,
                        117, 57, 441, 472, 151, 484, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0
                },
                {
                        329, 215, 257, 161, 479, 507, 456, 184, 89, 342, 109, 161, 479, 484, 459,
                        453, 395, 151, 51, 422, 458, 371, 237, 172, 161, 341, 470, 147, 59, 59,
                        477, 474, 472, 172, 229, 432, 208, 431, 431, 477, 441, 249, 461, 124, 432,
                        46, 172, 442, 503, 6, 474, 477, 313, 48, 507, 237, 481, 247, 18, 403, 517,
                        483, 456, 0, 186, 478, 431, 408, 177, 477, 162, 18, 479, 413, 165, 108,
                        177, 233, 472, 124, 182, 459, 459, 441, 250, 525, 483, 479, 242, 442, 459,
                        469, 477, 328, 510, 246, 457, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0
                },
                {
                        477, 172, 494, 483, 500, 475, 473, 328, 297, 161, 261, 479, 259, 81, 485,
                        483, 225, 26, 242, 151, 240, 403, 485, 479, 352, 351, 229, 243, 395, 342,
                        461, 236, 469, 155, 509, 456, 215, 341, 128, 484, 413, 151, 350, 477, 453,
                        237, 172, 477, 477, 164, 500, 259, 403, 242, 179, 236, 175, 459, 67, 175,
                        175, 472, 482, 0, 63, 319, 63, 319, 510, 175, 261, 483, 237, 46, 354, 235,
                        291, 182, 354, 180, 408, 460, 472, 173, 261, 229, 501, 180, 339, 236, 472,
                        63, 250, 151, 44, 117, 262, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0
                },
                {
                        262, 117, 319, 351, 351, 270, 117, 472, 220, 262, 48, 112, 351, 262, 268,
                        268, 521, 304, 111, 165, 179, 140, 432, 440, 162, 213, 159, 48, 152, 477,
                        70, 457, 444, 268, 341, 500, 472, 257, 472, 485, 422, 472, 472, 472, 505,
                        505, 516, 30, 477, 257, 500, 472, 91, 242, 111, 265, 484, 63, 351, 502,
                        447, 510, 507, 0, 259, 6, 22, 445, 18, 502, 261, 505, 518, 136, 89, 111,
                        484, 43, 124, 339, 484, 422, 427, 399, 110, 452, 110, 84, 445, 508, 351,
                        394, 395, 395, 435, 457, 180, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0
                },
                {
                        167, 457, 472, 441, 408, 459, 225, 474, 456, 456, 209, 462, 151, 466, 453,
                        498, 299, 279, 518, 499, 172, 518, 172, 172, 48, 46, 46, 151, 471, 478,
                        462, 13, 494, 462, 518, 48, 472, 498, 433, 236, 236, 51, 352, 462, 478,
                        295, 182, 474, 58, 487, 483, 51, 477, 291, 498, 521, 351, 487, 472, 483,
                        98, 455, 477, 0, 67, 521, 83, 51, 179, 471, 151, 478, 252, 318, 455, 318,
                        472, 252, 240, 133, 194, 68, 236, 68, 350, 350, 22, 56, 151, 186, 462, 485,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                },
                {
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                }
        };
    }

    private static String MakeMultiVocalString(byte[] s, int mode) {
        StringBuilder pinyin = new StringBuilder();
        ArrayList<String> eachString = new ArrayList<String>();
        String[][] stringMultivocal = ConvertToFullSpellPinyinByMultiVocal(s, mode);
        int i = stringMultivocal.length;

        if (i == 0)
            return pinyin.toString();

        do {
            i--;
            if (eachString.size() == 0) {
                for (int j = 0; j < stringMultivocal[i].length; j++) {
                    eachString.add(stringMultivocal[i][j]);
                }
            } else {
                int nCurrentSize = eachString.size();
                int j = stringMultivocal[i].length;
                do {
                    j--;
                    if (j == 0) {
                        for (int k = 0; k < nCurrentSize; k++)
                            eachString.set(k, stringMultivocal[i][j] + eachString.get(k));
                    } else {
                        for (int k = 0; k < nCurrentSize; k++)
                            eachString.add(stringMultivocal[i][j] + eachString.get(k));
                    }
                } while (j != 0);
            }
            stringMultivocal[i] = new String[0];
        } while (i != 0);

        stringMultivocal = null;

        for (i = 0; i < eachString.size(); i++) {
            pinyin.append(eachString.get(i));
            if (i != eachString.size() - 1) {
                pinyin.append(multiVocalSplit);
            }
        }

        eachString.clear();
        return pinyin.toString();
    }

    private static byte[] stringToBytes(String ascii) {
        byte[] bytes = new byte[ascii.length()];
        for (int i = 0; i < ascii.length(); ++i)
            bytes[i] = (byte) ascii.charAt(i);
        return bytes;
    }

    private static char toLower(char ch) {
        if (ch >= 'A' && ch <= 'Z') {
            return (char) (ch + ('a' - 'A'));
        } else {
            return ch;
        }
    }
}
