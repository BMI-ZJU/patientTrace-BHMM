package cn.edu.zju.util;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * 去除词典中的每行后面多余的,,,,,,,
     */
    public static void removeRedundantComma(String filePath) throws IOException {
        File file = new File(filePath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
        List<String> content = new ArrayList<>();
        String s = null;
        while ((s = reader.readLine()) != null) {
            content.add(s.replaceAll(",+$", ""));
        }

        reader.close();

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (String sw : content) {
            writer.write(sw);
            writer.newLine();
        }
        writer.flush();
        writer.close();

    }

    public static void main(String[] args) throws IOException {
        removeRedundantComma("resources/save/处方词典.csv");
    }

}
