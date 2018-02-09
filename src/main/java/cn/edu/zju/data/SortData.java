package cn.edu.zju.data;

import cn.edu.zju.util.*;
import com.csvreader.CsvReader;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import static cn.edu.zju.util.Utils.readObject;
import static cn.edu.zju.util.Utils.writeObject;

/**
 * Created by gzx-zju on 2017/12/11.
 * 对数据做一些整理<br>
 *     比如
 * <ul>
 *     <li>不同医嘱，处方，手术名的归一化</li>
 *     <li>相同处方名的，不同剂量，强度“归一化”</li>
 * </ul>
 *
 * 维持一个 医嘱dict，处方dict，手术dict
 */
public class SortData {

    // 将所有的处方名，医嘱，手术名列出来
    public static void listAll() throws IOException {
        String rootPath = "resources/patientCSV";
        File root = new File(rootPath);
        File[] files = root.listFiles();
        assert files != null;

        Map<String, Integer> event2index = new HashMap<>();
        Map<String, List<Double>> eventIntensity = new HashMap<>();
        List<String> events = new ArrayList<>();
        int eventN = 0;
        for (File file : files) {
            CsvReader reader = new CsvReader(file.getPath(), ',', Charset.forName("gbk"));
            reader.readHeaders();
            while(reader.readRecord()) {
                String[] values = reader.getValues();
                String event = values[0];
                if (! event2index.containsKey(event)) {
                    event2index.put(event, eventN);
                    eventIntensity.put(event, new ArrayList<>());
                    events.add(event);
                    eventN++;
                }
                List<Double> intensities = eventIntensity.get(event);
                for (int i=1; i<values.length; i++) {
                    if (!intensities.contains(Double.parseDouble(values[i]))) {
                        intensities.add(Double.parseDouble(values[i]));
                    }
                }
            }
            reader.close();
        }

        String eventsList = "resources/save/events.csv"; // event list
        String eventsE2I = "resources/save/event2index.model"; // Event to index
        String eventsEI = "resources/save/eventIntensity.model"; // Event intensities

        writeObject(eventsE2I, event2index);
        writeObject(eventsEI, eventIntensity);

        File el = new File(eventsList);
        BufferedWriter out = new BufferedWriter(new FileWriter(el));
        Map<String, Scaler> eventScaler = new HashMap<>();

        for (String e : events) {
            List<Double> item = eventIntensity.get(e);
            item.sort(Double::compareTo);
            if (item.size() <= 6) {
                Map<Double, Integer> valueDict = new HashMap<>();
                for (int i = 0; i < item.size(); i++) {
                    valueDict.put(item.get(i), i);
                }
                eventScaler.put(e, new CategoryScaler(valueDict));
            } else {
                double max = item.get(item.size() - 1);
                eventScaler.put(e, new LinearScaler(max));
            }
            String content = item.stream().map(String::valueOf).reduce(e, (x, y) -> x + "," + y);
            out.write(content + "\n");
        }
        writeObject("resources/save/eventScaler.model", eventScaler);
        out.flush();
        out.close();
    }

    public static void reverseScaler() {
        Map<String, List<Double>> eventIntensity = (Map<String, List<Double>>) readObject("resources/save/eventIntensity.model");
        assert eventIntensity != null;

        Map<String, ReverseScaler> eventReverseScaler = new HashMap<>();

        for (Map.Entry<String, List<Double>> entry : eventIntensity.entrySet()) {
            String event = entry.getKey();
            List<Double> items = entry.getValue();
            items.sort(Double::compareTo);

            if (items.size() <= 6) {
                Map<Integer, Double> valueDict = new HashMap<>();
                for (int i = 0; i < items.size(); i++) {
                    valueDict.put(i, items.get(i));
                }
                eventReverseScaler.put(event, new ReverseCategoryScaler(valueDict));

            } else {
                double max = items.get(items.size() - 1);
                eventReverseScaler.put(event, new ReverseLinearScaler(max));
            }
        }

        writeObject("resources/save/eventReverseScaler.model", eventReverseScaler);
    }

    public static void main(String[] args) throws IOException {
//        listAll();
        reverseScaler();
    }
}
