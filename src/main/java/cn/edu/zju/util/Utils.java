package cn.edu.zju.util;

import java.io.*;

public class Utils {

    public static void writeObject(String path, Object o) {
        try{
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(path));
            os.writeObject(o);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Object readObject(String path) {
        try {
            ObjectInputStream is = new ObjectInputStream(new FileInputStream(path));
            return is.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final char SBC_SPACE = 12288; // 全角空格
    private static final char DBC_SPACE = 32;  // 半角空格
    private static final char ASCII_START = 33;
    private static final char ASCII_END = 126;
    private static final char UNICODE_START = 65281;
    private static final char UNICODE_END = 65374;
    private static final char DBC_SBC_STEP = 65248;

    private static char sbc2dbc(char src) {
        if (src == SBC_SPACE) {
            return DBC_SPACE;
        }

        if (src >= UNICODE_START && src <= UNICODE_END) {
            return (char) (src - DBC_SBC_STEP);
        }

        return src;
    }

    /**
     * 全角字符串 --> 半角字符串
     * @param src 全角字符串
     * @return 半角字符串
     */
    public static String sbc2dbc(String src) {
        if (src == null) {
            return null;
        }

        char[] c = src.toCharArray();
        for (int i=0; i<c.length; i++) {
            c[i] = sbc2dbc(c[i]);
        }
        return new String(c);
    }

    private static char dbc2sbc(char src) {
        if (src == DBC_SPACE) {
            return SBC_SPACE;
        }
        if (src> ASCII_START && src <= ASCII_END) {
            return (char) (src + DBC_SBC_STEP);
        }
        return src;
    }

    /**
     * 半角字符串 --> 全角字符串
     * @param src 半角字符串
     * @return 全角字符串
     */
    public static String dbc2sbc(String src) {
        if (src == null) {
            return null;
        }

        char[] c = src.toCharArray();
        for (int i=0; i<c.length; i++) {
            c[i] = dbc2sbc(c[i]);
        }
        return new String(c);
    }

    public static void main(String[] args) {
        String o = "1：2";
        System.out.println(sbc2dbc(o));
        System.out.println(dbc2sbc(o));
    }

}
