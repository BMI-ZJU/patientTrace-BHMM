package cn.edu.zju.longitudinal;

import com.csvreader.CsvReader;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by gzx-zju on 2018/11/26
 */
public class Data {

    public static int[][][] loadDefaultData() throws IOException {
        return loadData("resources/longitudinal data/feature_df.csv");
    }

    public static int[][][] loadData(String path) throws IOException {
        /*
          载入数据，以三维数组的形式返回。第一维代表某一个病人，第二维表示有多少次入院记录，第三维表示事件或特征的值
          三维数组为Int类型
         */
        CsvReader reader = new CsvReader(path, ',', Charset.forName("GBK"));
        List<String[]> lines = new ArrayList<>();

        reader.readHeaders();
        String[] headers = reader.getHeaders();

        int[][][] result;

        Map<String, Integer> patientId2Index = new HashMap<>();

        while (reader.readRecord()) {
            String[] values = reader.getValues();
            lines.add(values);
            String patientId = values[0];
            if (!patientId2Index.containsKey(patientId)) {
                patientId2Index.put(patientId, patientId2Index.size());
            }
        }

        Map<String, List<String[]>> patientIdRecord = lines.stream()
                .collect(Collectors.groupingBy(e -> e[0]));

        result = new int[patientIdRecord.size()][][];

        for (Map.Entry<String, List<String[]>> entry: patientIdRecord.entrySet()) {
            String patientId = entry.getKey();
            int idx = patientId2Index.get(patientId);
            List<String[]> values = entry.getValue();
            int[][] one = new int[values.size()][headers.length - 1];

            for (int i=0; i<values.size(); i++) {
                for (int j=0; j<headers.length-1; j++) {
                    one[i][j] = Integer.valueOf(values.get(i)[j+1]);
                }
            }

            result[idx] = one;
        }

        System.out.println("...");
        return result;
    }

    public static int[] getV() {
        int[] result = new int[109];
        File file = new File("resources/longitudinal data/V.txt");
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tmp;
            int line = 0;
            while ((tmp = reader.readLine()) != null) {
                result[line] = Integer.valueOf(tmp);
                line++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
//        loadData("resources/longitudinal data/feature_df.csv");
        getV();
    }
}
