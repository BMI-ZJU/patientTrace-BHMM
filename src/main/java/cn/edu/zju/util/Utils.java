package cn.edu.zju.util;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static String[][] readPatientRecord(String path) throws IOException {
        CsvReader reader = new CsvReader(path);
        List<String[]> lines = new ArrayList<>();

        while (reader.readRecord()) {
            lines.add(reader.getValues());
        }

        String[][] content = new String[0][];
        content = lines.toArray(content);
        content = transposition(content);
        return content;
    }

    private static String[][] transposition(String[][] origin){
        int row = origin.length;
        int column = origin[0].length;

        String[][] result = new String[column][row];
        for (int i=0; i<row; i++) {
            for (int j=0; j<column; j++) {
                result[j][i] = origin[i][j];
            }
        }

        return result;
    }


    //字符转换
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
    private static void removeRedundantComma(String filePath) throws IOException {
        File file = new File(filePath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
        List<String> content = new ArrayList<>();
        String s;
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

    public static Map<String, String> loadDict(String path){
        Map<String, String> dict = new HashMap<>();
        try {
            CsvReader reader = new CsvReader(path, ',', Charset.forName("utf-8"));

            while (reader.readRecord()) {
                String[] values = reader.getValues();
                String value = values[0];
                for (int i=1; i<values.length; i++) {
                    dict.put(values[i], value);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dict;
    }

    public static void main(String[] args) throws IOException {
    }

}
